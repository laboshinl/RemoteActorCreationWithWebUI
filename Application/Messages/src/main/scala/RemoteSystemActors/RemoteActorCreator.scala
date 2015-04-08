package RemoteSystemActors

import java.io.Serializable
import java.net.NetworkInterface
import java.util.UUID

import LocalAppActors.{RemoteSystemManager, ActorManager}
import akka.actor.{Actor, ActorRef, ActorSelection, Props}
import akka.event.Logging
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import core.heartbleed.HeartBleedMessages

import scala.collection.immutable
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.Future

/**
 * Created by baka on 08.04.15.
 */

trait RemoteActorMessages {
  @SerialVersionUID(13L)
  case class CreateNewActor(actorType: String, actorId : String, clientId: String, subString : String, sendString : String) extends Serializable
  @SerialVersionUID(24L)
  case object StopSystem extends Serializable
  @SerialVersionUID(27L)
  case object TellYourIP extends Serializable

  case object DeleteMe
}

object RemoteActorCreator extends RemoteActorMessages {
  def createNewActor(actorRef: ActorRef, actorType: String, actorId : String, clientId: String, subString : String, sendString : String): Future[Any] = {
    actorRef ? CreateNewActor(actorType, actorId, clientId, subString, sendString)
  }

  def stopSystem(actorRef: ActorRef): Unit = {
    actorRef ! StopSystem
  }

  def deleteMePlease(actorRef: ActorRef): Unit = {
    actorRef ! DeleteMe
  }

  def tellYourIp(actorRef: ActorRef): Unit = {
    actorRef ! TellYourIP
  }
}

class RemoteActorCreator extends Actor with RemoteActorMessages with HeartBleedMessages {
  val myUUID = UUID.randomUUID()
  implicit val timeout: Timeout = 10 seconds
  val address = NetworkInterface.getNetworkInterfaces.next().getInetAddresses.toList.get(1).getHostAddress
  val logger = Logging.getLogger(context.system, this)
  var robotsUUIDMap = new immutable.HashMap[UUID, ActorRef]
  import context.dispatcher
  var remote: ActorSelection = _


  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    super.preStart()
    connectToRootSystem()
  }

  def connectToRootSystem(): Unit = {
    try {
      logger.info("Trying to connect...")
      remote = context.actorSelection(ConfigFactory.load().getString("my.own.root-system-address") +
        ConfigFactory.load().getString("my.own.master-name"))
      Await.result(RemoteSystemManager.pingManager(remote), timeout.duration)
      RemoteSystemManager.connectToManager(remote, myUUID, robotsUUIDMap)
      logger.info("Connected...!")
    } catch {
      case e: Exception => logger.info("Retrying...");
    }
  }

  def createActorProps(createReq: CreateNewActor): Props = {
    createReq.actorType match {
      case "ParrotActor" => Props(classOf[ParrotActor],
        createReq.actorId, createReq.subString, createReq.sendString, self)
      case "CommandProxy" => Props(classOf[CommandProxyActor],
        createReq.actorId, createReq.subString, createReq.sendString, self)
      case _ => throw new Exception("NonexistentActorType")
    }
  }

  def createActor(createReq: CreateNewActor, props: Props): immutable.HashMap[UUID, ActorRef] = {
    val actor = context.system.actorOf(props)
    ActorManager.replyActorCreated(sender())
    robotsUUIDMap + ((UUID.fromString(createReq.clientId), actor))
  }

  def createActor(createReq: CreateNewActor): Unit = {
    logger.debug("Got CreateNewActor request")
    try {
      val props = createActorProps(createReq)
      robotsUUIDMap = createActor(createReq, props)
    } catch {
      case e: Exception => ActorManager.replyActorCreationError(sender())
    }
  }

  def deleteActor(actor: ActorRef): immutable.HashMap[UUID, ActorRef] = {
    robotsUUIDMap.filter(_._2 != actor)
  }

  override def receive = {
    case req: CreateNewActor => createActor(req)
    case StopSystem => context.system.scheduler.scheduleOnce(1.second) {
      logger.info("Terminating system..." + context.system.toString)
      context.system.shutdown()
    }
    case Reconnect  => connectToRootSystem()
    case DeleteMe   => robotsUUIDMap = deleteActor(sender())
    case TellYourIP => RemoteSystemManager.replyMyIp(remote, address)
  }
}
