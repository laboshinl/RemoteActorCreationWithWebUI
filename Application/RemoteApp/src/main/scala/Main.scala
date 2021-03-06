/**
 * Created by mentall on 08.02.15.
 */

import java.net.{NetworkInterface}
import java.util.UUID

import akka.actor._
import akka.event.Logging
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import scala.collection.immutable
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.collection.JavaConversions._
import core.messages._
import core.heartbleed.HeartBleed
import core.messages.RemoteActor._

object Main extends App {
  val system = ActorSystem("HelloRemoteSystem")
  implicit val timeout: Timeout = 2 second
  val logger = Logging.getLogger(system, this)
  val supervisor    = system.actorOf(Props[Supervisor], "supervisor")
  val remoteActorCreator = Await.result((supervisor ? (Props[RemoteActorCreator], "RemoteActor")),
    timeout.duration).asInstanceOf[ActorRef]
  val rootSystemName = ConfigFactory.load().getString("my.own.root-system-address")
  Await.result((supervisor ? (Props(classOf[HeartBleed], rootSystemName, List(remoteActorCreator)), "HeartBleed")),
    timeout.duration)
  logger.info("Started...")
}

class Supervisor extends Actor {
  import akka.actor.OneForOneStrategy
  import akka.actor.SupervisorStrategy._
  import scala.concurrent.duration._

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case _: NullPointerException     => Restart
      case _: IllegalArgumentException => Stop
      case _: Exception                => Restart
    }

  def receive = {
    case (p: Props, n: String) => sender() ! context.actorOf(p, n)
  }
}



class RemoteActorCreator extends Actor {
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
      val connection = remote ? General.Ping
      Await.result(connection, timeout.duration)
      remote ! RemoteSystemManager.RemoteConnectionRequest(myUUID, robotsUUIDMap)
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
    sender ! ActorManager.ActorCreated(actor)
    robotsUUIDMap + ((UUID.fromString(createReq.clientId), actor))
  }

  def createActor(createReq: CreateNewActor): Unit = {
    logger.debug("Got CreateNewActor request")
    try {
      val props = createActorProps(createReq)
      robotsUUIDMap = createActor(createReq, props)
    } catch {
      case e: Exception => sender ! ActorManager.NonexistentActorType
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
    case HeartBleed.Reconnect  => connectToRootSystem()
    case DeleteMe   => robotsUUIDMap = deleteActor(sender())
    case TellYourIP => sender ! RemoteSystemManager.MyIPIs(address)
  }
}