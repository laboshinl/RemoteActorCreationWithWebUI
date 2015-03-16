import akka.actor.{PoisonPill, ActorRef, Actor}
import akka.actor.Actor.Receive
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
  var uniqueActorId : Long = 0
  // WHAT??????
  // I need to have ONE ID FOR PAIR.
  // def uniqueActorId  : Long = { _uniqueActorId += 1; _uniqueActorId }

  var idToActorsMap = new scala.collection.mutable.HashMap[Long, ActorRef]

  override def receive: Receive = {
    case ActorCreation(t) => createActorOnRemoteMachine(t)
    case ActorTermination(id) => deleteActorOnRemoteMachine(id)
    case SendMessageToActor(id, msg) => sendMessageToActorOnRemoteMachine(id, msg)
  }

  def sendMessageToActorOnRemoteMachine(id: Long, msg: String) = {
    if (idToActorsMap.contains(id)){
      val res = Await.result(idToActorsMap(id) ? msg, timeout.duration)
      if (res.isInstanceOf[String]) sender ! res.toString
      else logger.debug("Actor's response in not string"); sender ! "Actor's response in not string"
    }
    else sender ! NoSuchId
  }

  def deleteActorOnRemoteMachine(id: Long) = {
    if (idToActorsMap.contains(id)){
      idToActorsMap(id) ! PoisonPill
      idToActorsMap -= id
      sender ! id
    }
    else sender ! NoSuchId
  }

  def createActorOnRemoteMachine (actorType : String) = {
    val actorId = uniqueActorId.toString + "-actor"
    val clientId = uniqueActorId.toString + "-client"
    logger.debug("Create Actor for client: " + clientId)
    Await.result((RouterProvider ? RegisterPair(clientId, actorId)), timeout.duration) match {
      case res : PairRegistered =>
        logger.debug("Pair registred on Router")
        Await.result(RemoterActor ? CreateNewActor(actorType, actorId, res.actorSubStr, res.sendString), timeout.duration) match {
          case createRes : ActorCreated =>
            logger.debug("Actor created!")
            idToActorsMap += ((uniqueActorId, createRes.asInstanceOf[ActorCreated].adr))
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
    uniqueActorId += 1
  }
}
