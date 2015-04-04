package my.own.localapp.actormanager

import my.own.messages._
import java.util.UUID

import akka.actor.{PoisonPill, ActorRef, Actor}
import akka.event.Logging
import akka.remote.DisassociatedEvent
import my.own.localapp.messages.RemoteCommand
import scala.collection.mutable
import scala.concurrent.duration._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Await

/**
 * Created by mentall on 15.03.15.
 * 
 * 
 * Этот актор ответственен за создание и взаимодействие с акторами удаленной системы.
 * Он содержит таблицу соответствия идентификатора адресу актора (uuid, actorref).
 */
class ActorManager(val routerManager: ActorRef, val remoteSystemManager : ActorRef) extends Actor {
  implicit val timeout: Timeout = 2 second
  var logger = Logging.getLogger(context.system, self)

  var idToActor = new scala.collection.mutable.HashMap[UUID, ActorRef]

  override def receive: Receive = {
    case ActorCreation(t)             => createRemoteActor(t)
    case ActorTermination(id)         => deleteRemoteActor(id)
    case SendMessageToActor(id, msg)  => sendMessageToRemoteActor(id, msg)
    case rc: RemoteCommand            => sendCommandToRemoteActor(rc)
    case event: DisassociatedEvent    => idToActor = disassociateActors(event)
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
      idToActor(id) ! TellYourIP
      idToActor(id) ! command
    }
    else sender ! NoSuchId
  }

  /**
   * тут нагло фильтруется всё, что принадлежит умеревшей системе
   */

  def disassociateActors(disassociatedEvent: DisassociatedEvent): mutable.HashMap[UUID, ActorRef] = {
    idToActor.filter{
      (tuple) =>
        if (
          // иначе похоже никак. Связка имени системы, ип и адреса по идее уникальна.
          tuple._2.path.address.system.equals(disassociatedEvent.remoteAddress.system) &&
            tuple._2.path.address.port.equals(disassociatedEvent.remoteAddress.port) &&
            tuple._2.path.address.host.equals(disassociatedEvent.remoteAddress.host)
        ) {
          logger.debug("Deleting actor: {}", tuple._2)
          false
        } else true
    }
  }

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
        Await.result(remoteSystemManager ? CreateNewActor(actorType, actorId.toString, res.actorSubStr, res.sendString), timeout.duration) match {
          case createRes : ActorCreated =>
            logger.debug("Actor created!")
            idToActor += ((clientId, createRes.asInstanceOf[ActorCreated].adr))
            sender ! ActorCreationSuccess("Success", clientId.toString, res.clientSubStr, res.sendString)
          case NonexistentActorType => {
            logger.error("Error: Wrong Actor Type")
            sender ! TaskResponse("Error", "Wrong Actor Type")
          }
        }
      case NoRouters => {
        logger.error("Error: No Routers")
        sender ! TaskResponse("Error", "Wrong Actor Type")
      }
    }
  }
}
