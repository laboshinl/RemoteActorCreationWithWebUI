/**
 * Created by baka on 08.03.15.
 */

import akka.actor._
import akka.event.LoggingAdapter
import akka.event.Logging
import akka.zeromq._
import akka.zeromq.ZMQMessage


class ReceiverActor(val address : String, val port : String, val routingInfo : ActorRef) extends Actor {
  var uniquePort = port.toInt + 1
  val zmqSystem = ZeroMQExtension(context.system)
  val listenSocket : ActorRef = zmqSystem.newRouterSocket(Array(Bind("tcp://*:" + port), Listener(self)))
  val logger : LoggingAdapter = Logging.getLogger(context.system, this)

  def resendForRouting(msg : ZMQMessage) = {
    routingInfo ! msg
  }

  override def receive: Receive = {
    //TODO: remove client
    case msg : ZMQMessage   => resendForRouting(msg)
  }

}
