package LocalAppActors

import java.io.Serializable
import java.util.UUID

import akka.actor.{Actor, ActorRef, PoisonPill}
import akka.event.Logging
import akka.pattern.ask
import akka.remote.DisassociatedEvent
import akka.util.Timeout
import core.messages._

import scala.collection.{immutable, mutable}
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.Future
;

/**
 * Created by mentall on 15.03.15.
 * 
 * 
 * Этот актор ответственен за создание и взаимодействие с акторами удаленной системы.
 * Он содержит таблицу соответствия идентификатора адресу актора (uuid, actorref).
 */

trait ActorManagerMessages {
  @SerialVersionUID(82L)
  case class ActorCreation(actorType : String) extends Serializable
  @SerialVersionUID(83L)
  case class ActorTermination(actorId: String) extends Serializable
  @SerialVersionUID(79L)
  case class SendMessageToActor(actorId: String, msg: String) extends Serializable
  @SerialVersionUID(228L)
  case class RemoteCommand(clientUID: String, command: String, args: immutable.List[String]) extends Serializable
  @SerialVersionUID(229L)
  case class UpdateActors(robotsUUIDMap: immutable.HashMap[UUID, ActorRef]) extends Serializable
}

// вся соль того что написано, в том, чтобы актор вызывающий эти функции ничего не знал про сообщения,
// которые он отправляет

object ActorManager extends ActorManagerMessages {
  // я предлагаю делать так: если это call (в смысле нужен ответ, то функция пусть возвращает футуру
  // тода норм будет делать Await.result(createActor(manager, "CommandProxy"), timeout.duration),
  // хотя можно и внутри функции, никто не запрещает,
  // но тогда надо передавать таймаут, а чот неохота, громоздко больно
  def createActor(receiver: ActorRef, actorType: String): Future[Any] = {
    receiver ? ActorCreation(actorType)
  }

  // cast остаётся просто cast-ом
  def disassociateSystem(receiver: ActorRef, event: DisassociatedEvent): Unit ={
    receiver ! DisassociatedEvent
  }

  def deleteActor(receiver: ActorRef, actorId: String): Future[Any] = {
    receiver ? ActorTermination(actorId)
  }

  def sendRemoteCommand(receiver: ActorRef, clientUID: String, command: String, args: immutable.List[String]): Unit = {
    receiver ! RemoteCommand(clientUID, command, args)
  }

  def sendMessageToActor(receiver: ActorRef, actorId: String, msg: String): Future[Any] = {
    receiver ? SendMessageToActor(actorId, msg)
  }

  def updateOnRSConnection(receiver: ActorRef, robotsUUIDMap: immutable.HashMap[UUID, ActorRef]): Unit = {
    receiver ! UpdateActors(robotsUUIDMap)
  }
}

class ActorManager(val routerManager: ActorRef, val remoteSystemManager : ActorRef)
  extends Actor with ActorManagerMessages with TaskManagerMessages with DisassociateSystem with RouterManagerMessages{
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
    case req: UpdateActors                => updateOnRSConnect(req)
  }

  /**
   * нужно заапдейтить акторов из Remote System, если упала главна нода
   * после того как она поднялась. Добавляем всех акторов из Remote System
   * если их нет в списке.
   */
  def updateOnRSConnect(req: UpdateActors): Unit = {
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
        //TODO: это есессно не скомпилится, нужно написать примерно тож самое для всех акторов
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
