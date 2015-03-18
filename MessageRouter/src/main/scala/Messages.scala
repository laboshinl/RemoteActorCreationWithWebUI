/**
 * Created by mentall on 08.03.15.
 */
import java.io.Serializable
import java.util.UUID

import akka.zeromq.ZMQMessage


@SerialVersionUID(25L)
case object ConnectionRequest extends Serializable
@SerialVersionUID(26L)
case object Connected extends Serializable
@SerialVersionUID(14L)
case class AddPair(val clientId : UUID, val actorId : UUID)
@SerialVersionUID(15L)
case class GetMessage(val Key: UUID) extends Serializable
@SerialVersionUID(16L)
case class SetMessage(val Key: UUID) extends Serializable
@SerialVersionUID(87L)
case class GetPairedSocket(val Key: String) extends Serializable
@SerialVersionUID(89L)
case class GetPairedUser(val Key: String) extends Serializable
@SerialVersionUID(90L)
case class ResendMsg(resendTo : UUID, msg : ZMQMessage) extends Serializable
@SerialVersionUID(17L)
case class NoElementWithSuchKey() extends Serializable
@SerialVersionUID(123L)
case class GetSendString() extends Serializable
