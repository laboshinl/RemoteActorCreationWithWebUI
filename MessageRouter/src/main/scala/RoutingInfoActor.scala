import java.util.UUID

import akka.actor.{PoisonPill, Props, ActorRef, Actor}
import akka.actor.Actor.Receive
import akka.event.{Logging, LoggingAdapter}
import akka.util.ByteString
import akka.zeromq.{ZMQMessage, ZeroMQExtension, Bind}
import com.typesafe.config.ConfigFactory

import scala.collection.{immutable, mutable}

/**
 * Created by baka on 18.03.15.
 */

class RoutingInfoActor(val address : String, val port : String) extends Actor {
  var uniquePort = port.toInt + 1
  val zmqSystem = ZeroMQExtension(context.system)
  val logger : LoggingAdapter = Logging.getLogger(context.system, this)
  val remote = context.actorSelection(ConfigFactory.load().getString("my.own.master-address"))
  remote ! ConnectionRequest
  var routingAddresses = new mutable.HashMap[UUID, String]
  var routingInfo = new mutable.HashMap[UUID, ActorRef]
  var routingPairs = new mutable.HashMap[UUID, UUID]

  def setNewUser(msg : SetMessage) : Unit = {
    val bindString = "tcp://" + address + ":" + uniquePort.toString
    routingAddresses += ((msg.Key, bindString))
    routingInfo += ((msg.Key, context.system.actorOf(Props(classOf[Publisher], bindString, routingPairs))))
    uniquePort += 1
    logger.debug("Received Set message: " + msg.Key + " created PubSocket on: " + routingInfo(msg.Key))
    sender ! bindString
  }

  def deleteUser(msg : DeleteClient) : Unit = {
    val clientUUID = msg.clientUUID
    if (routingPairs.contains(clientUUID)) {
      val robotUUID = routingPairs(clientUUID)
      routingPairs.remove(clientUUID)
      routingPairs.remove(robotUUID)
      routingAddresses.remove(clientUUID)
      routingAddresses.remove(robotUUID)
      routingInfo(clientUUID) ! PoisonPill
      routingInfo(robotUUID) ! PoisonPill
      routingInfo.remove(clientUUID)
      routingInfo.remove(robotUUID)
    }
  }

  def getUserPublisherConnectionString(msg : GetMessage) : Unit = {
    if (routingAddresses.contains(msg.Key)) {
      logger.debug("Received Get message: " + msg.Key + ", returning: " + routingAddresses(msg.Key))
      sender ! routingAddresses(msg.Key)
    }
    else {
      logger.debug("Received Get message: " + msg.Key + ", no element with such key")
      sender ! NoElementWithSuchKey
    }
  }

  def associateUsers(msg : AddPair) : Unit = {
    routingPairs += ((msg.clientId, msg.actorId))
    routingPairs += ((msg.actorId, msg.clientId))
    logger.debug("Paired: " + (msg.actorId, msg.clientId).toString)
  }

  def getSendString : Unit = {
    sender ! "tcp://" + address + ":" + port
  }

  def resendToPublisher(msg : ZMQMessage) : Unit = {
    val senderUUID = UUID.fromString(msg.frame(1).decodeString("UTF-8").split("\\.")(0))
    if (routingInfo.contains(senderUUID)) {
      routingInfo(senderUUID) ! msg
    }
    else
      logger.error("Can't find socket for uuid: " + senderUUID)
  }

  override def receive: Receive = {
    case msg : ZMQMessage       => resendToPublisher(msg)
    case msg : SetMessage       => setNewUser(msg)
    case msg : AddPair          => associateUsers(msg)
    case msg : GetMessage       => getUserPublisherConnectionString(msg)
    case msg : DeleteClient     => deleteUser(msg)
    case GetSendString          => getSendString
    case Connected              => logger.info("Connected to main actor system...")
    case msg : String           => logger.debug("Received string: " + msg); sender ! msg
    case aNonMsg                => logger.error("Some problems on ReceiverActor on address: " + address + ":" + port + " Msg: " + aNonMsg.toString)
  }
}
