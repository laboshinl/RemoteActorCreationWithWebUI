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

//TODO: implement actordelete here!

/**
 * This class is a broker of messages from webui to remote actor in actor system in VM
 */
class RemoteSystemManager(val OSManager: ActorRef) extends Actor
  with DisassociateSystem
{
  implicit val timeout: Timeout = 2 seconds
  var waiter : ActorRef = null
  var actorManager: ActorRef = null
  val logger = Logging.getLogger(context.system, self)
  var remoteSystems = new mutable.HashMap[UUID, ActorRef]
  var idToAmount = new mutable.HashMap[ActorRef, Int]
  logger.info("Remoter started")

  def onRemoteSystemConnection(request: RemoteConnectionRequest): Unit = {
    if (actorManager != null) {
      if (!remoteSystems.contains(request.uUID)) {
        logger.info("Connection request")
        remoteSystems += ((request.uUID, sender()))
        actorManager ! ActorManager.UpdateActors(request.robotsUUIDMap)
      }
    }
  }

  def createNewActor(msg: CreateActor): Unit =
    RandomSystemPolicy.createNewActor(remoteSystems, logger, sender(), msg, OSManager, idToAmount)


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


trait AbstractPolicy {
  implicit val timeout: Timeout = 2 seconds
  def chooseRemoteSystem(rs : mutable.HashMap[UUID, ActorRef]) : ActorRef
  def createNewActor(rs: mutable.HashMap[UUID, ActorRef], logger : akka.event.LoggingAdapter,
                     sender : ActorRef, msg: CreateActor, OSManager : ActorRef,
                     idToAmount : mutable.HashMap[ActorRef, Int]): Unit
  def deleteActor(rs : mutable.HashMap[UUID, ActorRef]) : Unit
  def updateAmountMapAndStartSystems(idToAmount : mutable.HashMap[ActorRef, Int], rms : ActorRef, OSManager : ActorRef): Unit
  
  protected val maxActorsAmountOnSystem : Int = 10
}

object RandomSystemPolicy extends AbstractPolicy{
  override def createNewActor(rs: mutable.HashMap[UUID, ActorRef], logger : akka.event.LoggingAdapter,
                               sender : ActorRef, msg: CreateActor, OSManager : ActorRef,
                               idToAmount : mutable.HashMap[ActorRef, Int]): Unit = {
    logger.debug("Got request on creation")
    if (rs.isEmpty) {
      logger.debug("Empty remoteSystemsList creating new VM")
      Await.result(OSManager ? OpenStackManager.MachineStart, timeout.duration)
    }
    val rms = chooseRemoteSystem(rs)
    Await.result(rms ? RemoteActor.CreateNewActor(msg.actorType,
      msg.actorId, msg.clientId, msg.subString, msg.sendString), timeout.duration) match {
      case ActorCreated(adr) =>
        try {
          Await.result(adr ? General.Ping, timeout.duration)
          sender ! ActorManager.ActorCreated(adr)
          //запуск доп. систем
          updateAmountMapAndStartSystems(idToAmount, rms, OSManager)
        } catch {
          case e: Exception => sender ! General.FAIL("Pong not received")
        }
      case NonexistentActorType => sender ! ActorManager.NonexistentActorType
    }
  }

  override def chooseRemoteSystem(rs : mutable.HashMap[UUID, ActorRef]) : ActorRef = {
      val r = scala.util.Random.nextInt(rs.size)
      val uUID = rs.keySet.toArray.apply(r)
      rs(uUID)
    }

  override def deleteActor(rs: mutable.HashMap[UUID, ActorRef]): Unit = ???

  override def updateAmountMapAndStartSystems(idToAmount: mutable.HashMap[ActorRef, Int], rms : ActorRef, OSManager : ActorRef): Unit = {
    if(idToAmount.contains(rms)) {
      idToAmount += ((rms, idToAmount(rms)+1))
      if (idToAmount(rms) > maxActorsAmountOnSystem) OSManager ! OpenStackManager.MachineStart
    }
    else idToAmount += ((rms, 1))
  }
}