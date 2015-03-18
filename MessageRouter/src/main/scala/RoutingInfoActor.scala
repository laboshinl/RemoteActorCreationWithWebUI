import java.util.UUID

import akka.actor.{ActorRef, Actor}
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
    routingInfo += ((msg.Key, zmqSystem.newPubSocket(Bind(bindString))))
    uniquePort += 1
    logger.debug("Received Set message: " + msg.Key + " created PubSocket on: " + routingInfo(msg.Key))
    sender ! bindString
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

  def getPairedUserPublisherSocket(msg : GetPairedSocket) : Unit = {
    val uuid = UUID.fromString(msg.Key)
    val socket = if (routingPairs.contains(uuid)) {
      val pairedUUID = routingPairs(uuid)
      if (routingInfo.contains(pairedUUID)) routingInfo(pairedUUID) else null
    } else null
    sender ! socket
  }

  def getPairedUserUUID(msg: GetPairedUser) : Unit = {
    val uuid = UUID.fromString(msg.Key)
    val pairedUUID = if (routingPairs.contains(uuid)) {
      routingPairs(uuid)
    } else null
    sender ! pairedUUID
  }

  def associateUsers(msg : AddPair) : Unit = {
    routingPairs += ((msg.clientId, msg.actorId))
    routingPairs += ((msg.actorId, msg.clientId))
    logger.debug("Paired: " + (msg.actorId, msg.clientId).toString)
  }

  def getSendString : Unit = {
    sender ! "tcp://" + address + ":" + port
  }

  def resendToReceiver(msg : ResendMsg) : Unit = {
    val resendToUUID = msg.resendTo
    val zmqMsg = msg.msg
    if (routingInfo.contains(resendToUUID)) {
      logger.debug("Sending msg to: " + resendToUUID + " " + zmqMsg.frames(0).decodeString("UTF-8"))
      val pubSocket = routingInfo(resendToUUID)
      logger.debug(pubSocket.toString)
      pubSocket ! zmqMsg
    } else {
      logger.error("Can't find socket for uuid: " + resendToUUID)
    }
  }

  override def receive: Receive = {
    case msg : ResendMsg        => resendToReceiver(msg)
    case msg : SetMessage       => setNewUser(msg)
    case msg : AddPair          => associateUsers(msg)
    case msg : GetMessage       => getUserPublisherConnectionString(msg)
    case GetSendString          => getSendString
    case msg : GetPairedSocket  => getPairedUserPublisherSocket(msg)
    case msg : GetPairedUser    => getPairedUserUUID(msg)
    case Connected              => logger.info("Connected to main actor system...")
    case msg : String           => logger.debug("Received string: " + msg); sender ! msg
    case aNonMsg                => logger.error("Some problems on ReceiverActor on address: " + address + ":" + port + " Msg: " + aNonMsg.toString)
  }
}
