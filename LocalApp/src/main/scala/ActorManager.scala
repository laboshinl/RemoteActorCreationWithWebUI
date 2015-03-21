import java.util.UUID

import akka.actor.{PoisonPill, ActorRef, Actor}
import akka.event.Logging
import scala.concurrent.duration._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Await

/**
 * Created by mentall on 15.03.15.
 */
class ActorManager(val RouterProvider: ActorRef, val RemoterActor : ActorRef) extends Actor {
  implicit val timeout: Timeout = 1 minute
  var logger = Logging.getLogger(context.system, self)

  var idToActorsMap = new scala.collection.mutable.HashMap[UUID, ActorRef]

  override def receive: Receive = {
    case ActorCreation(t) => createActorOnRemoteMachine(t)
    case ActorTermination(id) => deleteActorOnRemoteMachine(id)
    case SendMessageToActor(id, msg) => sendMessageToActorOnRemoteMachine(id, msg)
  }

  def sendMessageToActorOnRemoteMachine(stringUUID: String, msg: String) = {
    val id = UUID.fromString(stringUUID)
    if (idToActorsMap.contains(id)){
      val res = Await.result(idToActorsMap(id) ? msg, timeout.duration)
      if (res.isInstanceOf[String]) sender ! res.toString
      else logger.debug("Actor's response in not string"); sender ! "Actor's response in not string"
    }
    else sender ! NoSuchId
  }

  def deleteActorOnRemoteMachine(stringUUID: String) = {
    val id = UUID.fromString(stringUUID)
    if (idToActorsMap.contains(id)){
      idToActorsMap(id) ! PoisonPill
      idToActorsMap -= id
      RouterProvider ! DeleteClient(id)
      sender ! stringUUID
    }
    else sender ! NoSuchId
  }

  def createActorOnRemoteMachine (actorType : String) = {
    val actorId  = UUID.randomUUID
    val clientId = UUID.randomUUID

    logger.debug("Create Actor for client: " + clientId.toString)
    Await.result((RouterProvider ? RegisterPair(clientId, actorId)), timeout.duration) match {
      case res : PairRegistered =>
        logger.debug("Pair registered on Router")
        Await.result(RemoterActor ? CreateNewActor(actorType, actorId.toString, res.actorSubStr, res.sendString), timeout.duration) match {
          case createRes : ActorCreated =>
            logger.debug("Actor created!")
            idToActorsMap += ((clientId, createRes.asInstanceOf[ActorCreated].adr))
            sender ! (clientId.toString + " " + res.clientSubStr + " " + res.sendString)
          case NonexistentActorType => {
            logger.error("Error: Wrong Actor Type")
            sender ! "Error: Wrong Actor Type"
          }
        }
      case NoRouters =>{
        logger.error("Error: No Routers")
        sender ! "Error: No Routers"
      }
    }
  }
}
