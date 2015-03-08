/**
 * Created by baka on 08.03.15.
 */

import akka.actor._
import akka.event.LoggingAdapter
import akka.event.Logging
import akka.zeromq._

/**
 * simple stub of router actor now
 * it has socket for listening ZMQMessages (its my work)
 * and it must have routing info as <KeyType, ValueType> (its u work)
 * @param address
 * @param port
 */

class ReceiverActor[K, V](val address : String, val port : String) extends Actor with MessagesOfReceiverActor{
  val listenSocket : ActorRef = ZeroMQExtension(context.system).newSocket(SocketType.Router, Listener(self), Bind("tcp://" + address + ":" + port), HighWatermark(50000))
  val logger : LoggingAdapter = Logging.getLogger(context.system, this)

  var routingInfo = new scala.collection.mutable.HashMap[K, V]

  override def receive: Receive = {
    case msg : ZMQMessage   => logger.debug("ZMQ<essage received: " + msg.toString)
    case msg : String       => logger.debug("Received string: " + msg)
    case s   : SetMessage[K, V]   => {
      routingInfo += ((s.Key, s.Value));
      logger.debug("Received Set message: " + s.Key.toString + " " + s.Value.toString)
      sender ! "Value added"
    }
    case g   : GetMessage[K]   => {
      if (routingInfo.contains(g.Key)) {
        logger.debug("Received Get message: " + g.Key.toString + ", returning: " + routingInfo(g.Key))
        sender ! routingInfo(g.Key)
      }
      else {
        logger.debug("Received Get message: " + g.Key.toString + ", no element with such key")
        sender ! NoElementWithSuchKey
      }
    }
    case _                  => logger.error("Some problems on ReceiverActor on address: " + address + ":" + port)
  }

}