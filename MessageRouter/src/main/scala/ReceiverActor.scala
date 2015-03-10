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

class ReceiverActor(val address : String, val port : String) extends Actor with MessagesOfReceiverActor{
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

    /**
     * Look here - generic types need to be matched like this.
     */
    case msg if msg.isInstanceOf[SetMessage] => {
      val s = msg.asInstanceOf[SetMessage]
      val bindString = "tcp://" + address + ":" + uniquePort.toString
      routingAddresses += ((s.Key, bindString))
      routingInfo += ((s.Key, zmqSystem.newPubSocket(Bind(bindString))))
      uniquePort += 1
      logger.debug("Received Set message: " + s.Key + " created PubSocket on: " + bindString)
      sender ! bindString
    }
    case msg if msg.isInstanceOf[GetMessage]     => {
      val g = msg.asInstanceOf[GetMessage]
      if (routingAddresses.contains(g.Key)) {
        logger.debug("Received Get message: " + g.Key + ", returning: " + routingAddresses(g.Key))
        sender ! routingAddresses(g.Key)
      }
      else {
        logger.debug("Received Get message: " + g.Key + ", no element with such key")
        sender ! NoElementWithSuchKey
      }
    }
    case aNonMsg               => logger.error("Some problems on ReceiverActor on address: " + address + ":" + port + " Msg: " + aNonMsg.toString)
  }

}