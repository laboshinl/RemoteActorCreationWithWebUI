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

class ReceiverActor(val address : String, val port : String) extends Actor {
  val listenSocket : ActorRef = ZeroMQExtension(context.system).newSocket(SocketType.Router, Listener(self), Bind("tcp://" + address + ":" + port), HighWatermark(50000))
  val logger : LoggingAdapter = Logging.getLogger(context.system, this)

  override def receive: Receive = {
    case msg : ZMQMessage   => logger.debug("ZMQ<essage received: " + msg.toString)
    case msg : String       => logger.debug("Received string: " + msg)
    case _                  => logger.error("Some problems on ReceiverActor on address: " + address + ":" + port)
  }
}
