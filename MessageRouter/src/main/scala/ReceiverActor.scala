/**
 * Created by baka on 08.03.15.
 */

import akka.actor._
import akka.event.LoggingAdapter
import akka.event.Logging
import akka.zeromq._
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import scala.collection.{immutable, mutable}
import akka.zeromq.ZMQMessage


class ReceiverActor(val address : String, val port : String) extends Actor {
  var uniquePort = port.toInt + 1
  val zmqSystem = ZeroMQExtension(context.system)
  val listenSocket : ActorRef = zmqSystem.newRouterSocket(Array(Bind("tcp://*:" + port), Listener(self)))
  val logger : LoggingAdapter = Logging.getLogger(context.system, this)
  val remote = context.actorSelection(ConfigFactory.load().getString("my.own.master-address"))
  remote ! ConnectionRequest
  var routingAddresses = new mutable.HashMap[String, String]
  var routingInfo = new mutable.HashMap[String, ActorRef]
  var routingPairs = new mutable.HashMap[String, String]

  def resendToReceiver(msg : ZMQMessage) : Unit = {
    val id = msg.frame(1).decodeString("UTF-8")
    logger.debug("ZMQmessage received from: " + id)
    if (routingInfo.contains(id)) {
      val receiverId = routingPairs(id)
      logger.debug("Sending to socket: ", routingAddresses(id), " with topic: ", receiverId)
      val publisher : ActorRef = routingInfo(id)
      logger.debug("Get Publisher : " + publisher)
      val zmqmsg = ZMQMessage(immutable.Seq(ByteString(receiverId)) ++ msg.frames.drop(2))
      publisher ! zmqmsg
    } else {
      logger.error("Can't find receiver for message: ", msg.toString)
    }
  }

  def setNewUser(msg : SetMessage) : Unit = {
    val bindString = "tcp://" + address + ":" + uniquePort.toString
    routingAddresses += ((msg.Key, bindString))
    routingInfo += ((msg.Key, zmqSystem.newPubSocket(Bind(bindString))))
    uniquePort += 1
    logger.debug("Received Set message: " + msg.Key + " created PubSocket on: " + bindString)
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

  def associateUsers(msg : AddPair) : Unit = {
    routingPairs += ((msg.clientId, msg.actorId))
    routingPairs += ((msg.actorId, msg.clientId))
    logger.debug("Paired: " + (msg.actorId, msg.clientId).toString)
  }

  def getSendString : Unit = {
    sender ! "tcp://" + address + ":" + port
  }

  override def receive: Receive = {
    //TODO: remove client
    case msg : ZMQMessage   => resendToReceiver(msg)
    case msg : SetMessage   => setNewUser(msg)
    case msg : AddPair      => associateUsers(msg)
    case msg : GetMessage   => getUserPublisherConnectionString(msg)
    case GetSendString      => getSendString
    case Connected          => logger.info("Connected to main actor system...")
    case msg : String       => logger.debug("Received string: " + msg); sender ! msg
    case aNonMsg            => logger.error("Some problems on ReceiverActor on address: " + address + ":" + port + " Msg: " + aNonMsg.toString)
  }

}
