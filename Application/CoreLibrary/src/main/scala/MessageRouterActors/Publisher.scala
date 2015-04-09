package MessageRouterActors

import java.util.UUID

import akka.actor.{Actor, PoisonPill}
import akka.event.{Logging, LoggingAdapter}
import akka.util.ByteString
import akka.zeromq.{Bind, ZMQMessage, ZeroMQExtension}

import scala.collection.{immutable, mutable}

/**
 * Created by baka on 18.03.15.
 */
class Publisher(val bindString : String, var routingPairs : mutable.HashMap[UUID, UUID]) extends Actor {
  val zmqSystem = ZeroMQExtension(context.system)
  val pubSocket = zmqSystem.newPubSocket(Bind(bindString))
  val logger : LoggingAdapter = Logging.getLogger(context.system, this)


  override def postStop(): Unit = {
    pubSocket ! PoisonPill
  }

  override def receive: Receive = {
    case msg : ZMQMessage => {
      val topic = msg.frame(1).decodeString("UTF-8")
      val array = topic.split("\\.")
      /**
       * only topics with 1 postfix supported now
       */
      val stringUUID = array(0)
      val endOfTopic = array(1)
      val senderUUID = UUID.fromString(stringUUID)
      if (routingPairs.contains(senderUUID)) {
        val receiverUUID = routingPairs(senderUUID)
        val zmqMsg = ZMQMessage(immutable.Seq(ByteString(receiverUUID.toString + "." + endOfTopic)) ++ msg.frames.drop(2))
        pubSocket ! zmqMsg
      } else {
        logger.error("Can't find receiver for message: ", msg.toString)
      }
    }
    case msg              => logger.error("Can't process msg: " + msg)
  }
}
