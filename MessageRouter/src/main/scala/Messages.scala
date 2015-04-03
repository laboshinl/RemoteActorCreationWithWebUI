/**
 * Created by mentall on 08.03.15.
 */
import java.io.Serializable
import java.util.UUID

import akka.zeromq.ZMQMessage

import scala.collection.mutable


@SerialVersionUID(25L)
case class RouterConnectionRequest(routingPairs: mutable.HashMap[UUID, UUID]) extends Serializable
@SerialVersionUID(26L)
case object Connected extends Serializable
@SerialVersionUID(14L)
case class AddPair(clientId : UUID, actorId : UUID) extends Serializable
@SerialVersionUID(15L)
case class GetMessage(Key: UUID) extends Serializable
@SerialVersionUID(16L)
case class SetMessage(Key: UUID) extends Serializable
@SerialVersionUID(87L)
case class GetPairedSocket(Key: String) extends Serializable
@SerialVersionUID(89L)
case class GetPairedUser(Key: String) extends Serializable
@SerialVersionUID(90L)
case class ResendMsg(resendTo : UUID, msg : ZMQMessage) extends Serializable
@SerialVersionUID(17L)
case class NoElementWithSuchKey() extends Serializable
@SerialVersionUID(123L)
case class GetSendString() extends Serializable
@SerialVersionUID(125L)
case class DeleteClient(clientUUID : UUID) extends Serializable

case object Reconnect
