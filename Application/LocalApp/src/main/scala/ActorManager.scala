import java.util.UUID

import akka.actor.{PoisonPill, ActorRef, Actor}
import akka.event.Logging
import akka.remote.DisassociatedEvent
import scala.collection.mutable
import scala.concurrent.duration._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Await

import core.messages._;

/**
 * Created by mentall on 15.03.15.
 * 
 * 
 * Этот актор ответственен за создание и взаимодействие с акторами удаленной системы.
 * Он содержит таблицу соответствия идентификатора адресу актора (uuid, actorref).
 */
class ActorManager(val routerManager: ActorRef, val remoteSystemManager : ActorRef) extends Actor with DisassociateSystem {
  implicit val timeout: Timeout = 5 second
  var logger = Logging.getLogger(context.system, self)
  var idToActor = new mutable.HashMap[UUID, ActorRef]

  /**
   * для восстановления состояния после падения
   * необходимо, чтобы Remote System Manager должен знать о
   * Actor Manager, чтобы отправлять ему сообщения.
   */
  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    super.preStart()
    remoteSystemManager ! ActorManagerStarted
  }

  override def receive: Receive = {
    case ActorCreation(t)                 => createRemoteActor(t)
    case ActorTermination(id)             => deleteRemoteActor(id)
    case SendMessageToActor(id, msg)      => sendMessageToRemoteActor(id, msg)
    case rc: RemoteCommand                => sendCommandToRemoteActor(rc)
    case event: DisassociatedEvent        => idToActor = disassociateSystem(idToActor, event)
    case req: RemoteConnectionRequest     => updateOnRSConnect(req)
  }

  /**
   * нужно заапдейтить акторов из Remote System, если упала главна нода
   * после того как она поднялась. Добавляем всех акторов из Remote System
   * если их нет в списке.
   */
  def updateOnRSConnect(req: RemoteConnectionRequest): Unit = {
    req.robotsUUIDMap.foreach{
      tuple =>
        if (!idToActor.contains(tuple._1))
          idToActor += (tuple)
    }
  }

  def sendMessageToRemoteActor(stringUUID: String, msg: String) = {
    val id = UUID.fromString(stringUUID)
    if (idToActor.contains(id)){
      val res = Await.result(idToActor(id) ? msg, timeout.duration)
      if (res.isInstanceOf[String]) sender ! res.toString
      else logger.debug("Actor's response in not string"); sender ! "Actor's response in not string"
    }
    else sender ! NoSuchId
  }

  def sendCommandToRemoteActor(command: RemoteCommand) = {
    val id = UUID.fromString(command.clientUID)
    if (idToActor.contains(id)) {
      idToActor(id) ! command
    }
    else sender ! NoSuchId
  }

  /**
   * если система отвалилась, то нагло фильтруется всё, что принадлежит умеревшей системе
   */

  def deleteRemoteActor(stringUUID: String) = {
    val id = UUID.fromString(stringUUID)
    if (idToActor.contains(id)){
      idToActor(id) ! PoisonPill
      idToActor -= id
      routerManager ! DeleteClient(id)
      sender ! TaskResponse("Success", stringUUID)
    }
    else sender ! TaskResponse("Error", "NoSuchId")
  }

  def createRemoteActor (actorType : String) = {
    val actorId  = UUID.randomUUID
    val clientId = UUID.randomUUID
    logger.debug("Create Actor for client: " + clientId.toString)
    Await.result((routerManager ? RegisterPair(clientId, actorId)), timeout.duration) match {
      case res : PairRegistered =>
        logger.debug("Pair registered on Router")
        Await.result(remoteSystemManager ? CreateNewActor(actorType, actorId.toString,
            clientId.toString, res.actorSubStr, res.sendString), timeout.duration) match {
          case createRes : ActorCreated =>
            logger.debug("Actor created!")
            idToActor += ((clientId, createRes.asInstanceOf[ActorCreated].adr))
            sender ! ActorCreationSuccess("Success", clientId.toString, res.clientSubStr, res.sendString)
          case NonexistentActorType =>
            logger.error("Error: Wrong Actor Type")
            routerManager ! UnregisterPair(clientId, actorId)
            sender ! TaskResponse("Error", "Wrong Actor Type")
        }
      case NoRouters =>
        logger.error("Error: No Routers")
        sender ! TaskResponse("Error", "Wrong Actor Type")
    }
  }
}
