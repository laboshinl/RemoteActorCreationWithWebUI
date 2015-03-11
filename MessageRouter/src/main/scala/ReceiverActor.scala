/**
 * Created by baka on 08.03.15.
 */

package receiver.ReceiverActor
import akka.actor._
import akka.event.LoggingAdapter
import akka.event.Logging
import akka.zeromq._
import akka.util.ByteString
import scala.collection.mutable
import akka.zeromq.ZMQMessage
import receiver.MessagesOfReceiverActor._
/**
 * simple stub of router actor now
 * it has socket for listening ZMQMessages (its my work)
 * and it must have routing info as <KeyType, ValueType> (its u work)
 * @param address
 * @param port
 */

class ReceiverActor(val address : String, val port : String) extends Actor {
  var uniquePort = port.toInt + 1
  val zmqSystem = ZeroMQExtension(context.system)
  val listenSocket : ActorRef = zmqSystem.newRouterSocket(Array(Bind("tcp://*:" + port), Listener(self)))
  val logger : LoggingAdapter = Logging.getLogger(context.system, this)

  var routingAddresses = new mutable.HashMap[String, String]
  var routingInfo = new mutable.HashMap[String, ActorRef]

  override def receive: Receive = {
    case msg : ZMQMessage   => {
      /**
       * Dummy implementation, only for test.
       */
      val id = msg.frame(1).decodeString("UTF-8")
      logger.debug("ZMQmessage received from: " + id)
      if (routingInfo.contains(id)) {
        val publisher : ActorRef = routingInfo(id)
        logger.debug("Get Publisher : " + publisher)
        val m = ZMQMessage(ByteString(id), ByteString("payload"))
        publisher ! m
      }
    }
    case msg : String       => logger.debug("Received string: " + msg); sender ! msg

    case msg : SetMessage => {
      val bindString = "tcp://" + address + ":" + uniquePort.toString
      routingAddresses += ((msg.Key, bindString))
      routingInfo += ((msg.Key, zmqSystem.newPubSocket(Bind(bindString))))
      uniquePort += 1
      logger.debug("Received Set message: " + msg.Key + " created PubSocket on: " + bindString)
      sender ! bindString
    }
    case msg : GetMessage   => {
      if (routingAddresses.contains(msg.Key)) {
        logger.debug("Received Get message: " + msg.Key + ", returning: " + routingAddresses(msg.Key))
        sender ! routingAddresses(msg.Key)
      }
      else {
        logger.debug("Received Get message: " + msg.Key + ", no element with such key")
        sender ! NoElementWithSuchKey
      }
    }
    case Connected => logger.debug("Connected")
    case aNonMsg   => logger.error("Some problems on ReceiverActor on address: " + address + ":" + port + " Msg: " + aNonMsg.toString)
  }

}
