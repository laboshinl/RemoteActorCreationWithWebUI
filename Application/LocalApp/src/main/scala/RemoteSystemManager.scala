import java.util.UUID

import akka.actor.{ActorRef, Actor}
import akka.event.Logging
import akka.remote.DisassociatedEvent
import akka.util.Timeout
import core.messages.ActorManager.ActorCreated
import scala.collection.mutable

import core.messages.RemoteSystemManager._
import core.messages._

import scala.concurrent.Await
import scala.concurrent.duration._
import akka.pattern.ask

/**
 * Created by mentall on 12.02.15.
 */

/**
 * This class is a broker of messages from webui to remote actor in actor system in VM
 */
class RemoteSystemManager() extends Actor
  with DisassociateSystem
{
  implicit val timeout: Timeout = 2 seconds
  var waiter : ActorRef = null
  var actorManager: ActorRef = null
  val logger = Logging.getLogger(context.system, self)
  var remoteSystems = new mutable.HashMap[UUID, ActorRef]
  logger.info("Remoter started")

  def remote : ActorRef = {
    val r = scala.util.Random.nextInt(remoteSystems.size)
    logger.debug("I have " + remoteSystems.size + " remote system and i choose " + r + " to send a message")
    val uUID = remoteSystems.keySet.toArray.apply(r)
    remoteSystems(uUID)
  }

  def onRemoteSystemConnection(request: RemoteConnectionRequest): Unit = {
    if (actorManager != null) {
      if (!remoteSystems.contains(request.uUID)) {
        logger.info("Connection request")
        remoteSystems += ((request.uUID, sender()))
        actorManager ! ActorManager.UpdateActors(request.robotsUUIDMap)
      }
    }
  }

  def createNewActor(msg: CreateActor): Unit = {
    logger.debug("Got request on creation")
    if(remoteSystems.isEmpty) {
      logger.debug("Empty remoteSystemsList")
      sender() ! ActorManager.NoRemoteSystems
    }
    Await.result(remote ? RemoteActor.CreateNewActor(msg.actorType,
      msg.actorId, msg.clientId, msg.subString, msg.sendString), timeout.duration) match {
      case ActorCreated(adr) =>
        try {
          Await.result(adr ? General.Ping, timeout.duration)
          sender() ! ActorManager.ActorCreated(adr)
        } catch {
          case e: Exception => sender() ! General.FAIL("Pong not received")
        }
      case NonexistentActorType => sender() ! ActorManager.NonexistentActorType
    }
  }

  override def receive: Receive = {
    case msg: CreateActor                 => createNewActor(msg)
    /*
    Я ОСТАВЛЮ ЭТО ЗДЕСЬ, ЧТОБ ТЫ ПОСМОТРЕЛ И ОСОЗНАЛ ГДЕ ГЛАВНАЯ ОШИБКА В ТАКОМ ПОДХОДЕ :)
    case ActorCreated(adr)                => logger.debug("Checking address"); adr ! Ping
    case NonexistentActorType             => logger.debug("NonExsistent actor type"); waiter ! NonexistentActorType
    case AddressIsOk                      => logger.debug("Address is ok"); waiter ! ActorCreated(sender())
    */
    case StopAllSystems                   => logger.info("Stopping remote system"); for (r <- remoteSystems.values) r ! RemoteActor.StopSystem
    case req: RemoteConnectionRequest     => onRemoteSystemConnection(req)
    case event: DisassociatedEvent        => remoteSystems = disassociateSystem(remoteSystems, event)
    case MyIPIs(ip)                       => logger.debug(ip)
    case ActorManagerStarted              => actorManager = sender()
    case General.Ping                     => sender ! General.Pong
  }
}
