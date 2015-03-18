import java.util.UUID

import akka.actor.{ActorRef, Actor}
import akka.event.Logging
import akka.zeromq.ZMQMessage
import scala.collection.immutable
import scala.concurrent.duration._
import akka.pattern.ask
import akka.util.{ByteString, Timeout}
import scala.concurrent.Await

/**
 * Created by baka on 18.03.15.
 */
class PrepareForResendActor(val routerInfo : ActorRef) extends Actor {
  implicit val timeout: Timeout = 1 minute
  var logger = Logging.getLogger(context.system, self)

  def prepareForResend(msg : ZMQMessage) = {
    val senderUUID = msg.frame(1).decodeString("UTF-8")
    logger.debug("Receive msg from: " + senderUUID)
    val pairedUUIDReply = Await.result((routerInfo ? GetPairedUser(senderUUID)), timeout.duration)

    if (pairedUUIDReply != null && pairedUUIDReply.isInstanceOf[UUID]) {
      val receiverUUID = pairedUUIDReply.asInstanceOf[UUID]
      logger.debug("Prepairing msg for: " + receiverUUID)
      val resendMsg = ZMQMessage(immutable.Seq(ByteString(receiverUUID.toString)) ++ msg.frames.drop(2))
      routerInfo ! ResendMsg(receiverUUID, resendMsg)
    } else {
      logger.error("Can't find pair for uuid: " + senderUUID)
    }
  }

  override def receive: Receive = {
    case msg : ZMQMessage => prepareForResend(msg)
    case msg              => logger.error("Can't work with this msg: " + msg)
  }
}
