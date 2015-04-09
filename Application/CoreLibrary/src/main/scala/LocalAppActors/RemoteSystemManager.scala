package LocalAppActors

import java.io.Serializable
import java.util.UUID

import RemoteSystemActors.RemoteActorCreator
import akka.actor.{ActorSelection, Actor, ActorRef}
import akka.event.Logging
import akka.remote.DisassociatedEvent
import core.messages._
import akka.pattern.ask

import scala.collection.{immutable, mutable}
import scala.concurrent.Future

/**
 * Created by mentall on 12.02.15.
 */

/**
 * This class is a broker of messages from webui to remote actor in actor system in VM
 */

trait RemoteSystemManagerMessages {
  @SerialVersionUID(2291L)
  case class RemoteConnectionRequest(uUID: UUID, robotsUUIDMap: immutable.HashMap[UUID, ActorRef]) extends Serializable
  @SerialVersionUID(28L)
  case class MyIPIs (ip : String) extends Serializable
  @SerialVersionUID(31L)
  case object StopAllSystems
  // Тут соль в чём, эти сообщения в разных трейтах, это разные объекты -> его тупо ресендить уже не получится
  // логически это тот же CreateNewActor, но под капотом это совсем другой класс
  @SerialVersionUID(32L)
  case class CreateActorReq(actorType: String, actorId : String, clientId: String, subString : String, sendString : String) extends Serializable
  @SerialVersionUID(33L)
  case class ActorCreatedReply(actorRef: ActorRef) extends Serializable
  @SerialVersionUID(34L)
  case object NonexistentActorType extends Serializable
  case object ActorManagerStarted
}

object RemoteSystemManager extends RemoteSystemManagerMessages with GeneralMessages {
  def connectToManager(actorSelection: ActorSelection, uUID: UUID, robotsUUIDMap: immutable.HashMap[UUID, ActorRef]): Unit = {
    actorSelection ! RemoteConnectionRequest(uUID, robotsUUIDMap)
  }

  def pingManager(actorSelection: ActorSelection): Future[Any] = {
    actorSelection ? Ping
  }

  def replyMyIp(actorSelection: ActorSelection, ip: String): Unit = {
    actorSelection ! MyIPIs(ip)
  }

  def stopAllSystems(actorRef: ActorRef): Unit = {
    actorRef ! StopAllSystems
  }

  def sayActorManagerStarted(actorRef: ActorRef): Unit = {
    actorRef ! ActorManagerStarted
  }

  def createActor(actorRef: ActorRef, actorType: String, actorId : String, clientId: String,
                  subString : String, sendString : String): Future[Any] = {
    actorRef ? CreateActorReq(actorType, actorId, clientId, subString, sendString)
  }
}

class RemoteSystemManager extends Actor with DisassociateSystem with RemoteSystemManagerMessages with GeneralMessages {
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
        actorManager ! request
        sender() ! Connected
        RemoteActorCreator.tellYourIp(sender())
      }
    }
  }

  def createNewActor(msg: CreateActorReq): Unit = {
    logger.debug("Got request on creation")
    if(remoteSystems.isEmpty) {
      logger.debug("Empty remoteSystemsList")
      ActorManager.replyNoRoutersError(sender())
    }
    waiter = sender()
    remote ! msg
  }

  override def receive: Receive = {
    case msg: CreateActorReq              => createNewActor(msg)
    case ActorCreatedReply(adr)           => logger.debug("Checking address"); adr ! Ping
    case NonexistentActorType             => logger.debug("NonExsistent actor type"); ActorManager.replyActorCreationError(waiter)
    case Pong                             => logger.debug("Address is ok"); ActorManager.replyActorCreated(sender())
    case StopAllSystems                    => logger.info("Stopping remote system"); for (r <- remoteSystems.values) RemoteActorCreator.stopSystem(r)
    case req: RemoteConnectionRequest     => onRemoteSystemConnection(req)
    case event: DisassociatedEvent        => remoteSystems = disassociateSystem(remoteSystems, event)
    case MyIPIs(ip)                       => logger.debug(ip)
    case ActorManagerStarted              => actorManager = sender()
    case Ping                             => sender ! Pong
  }
}
