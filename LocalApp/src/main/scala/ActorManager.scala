import akka.actor.{PoisonPill, ActorRef, Actor}
import akka.actor.Actor.Receive
import scala.concurrent.duration._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Await

/**
 * Created by mentall on 15.03.15.
 */
class ActorManager(val RouterProvider: ActorRef, val RemoterActor : ActorRef) extends Actor with MyBeautifulOutput{
  implicit val timeout: Timeout = 1 minute

  var _uniqueActorId : Long = 0
  def uniqueActorId  : Long = { _uniqueActorId += 1; _uniqueActorId }

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
      else out("Actor's response in not string"); sender ! "Actor's response in not string"
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

  //TODO: create connections on routers
  def createActorOnRemoteMachine (actorType : String) = {
    out("Here")
    val actorId = uniqueActorId.toString + "-actor"
    val clientId = uniqueActorId.toString + "-client"
    Await.result((RouterProvider ? RegisterPair(clientId, actorId)), timeout.duration) match {
      case res : PairRegistered =>
        out("Here2")
        Await.result(RemoterActor ? CreateNewActor(actorType, actorId, res.actorSubStr, res.sendString), timeout.duration) match {
          case createRes : ActorCreated =>
            idToActorsMap += ((uniqueActorId, createRes.asInstanceOf[ActorCreated].adr))
            sender ! (clientId.toString + " " + res.clientSubStr + " " + res.sendString)
          case NoRouters => sender ! "No Routers"
        }
      case _ => sender ! "Wrong Type"
    }
  }
}
