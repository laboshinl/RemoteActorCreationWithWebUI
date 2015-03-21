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
case class AddPair(clientId : UUID, actorId : UUID)
@SerialVersionUID(15L)
case class GetMessage(Key: UUID) extends Serializable
@SerialVersionUID(16L)
case class SetMessage(Key: UUID) extends Serializable
@SerialVersionUID(17L)
case class NoElementWithSuchKey() extends Serializable
@SerialVersionUID(123L)
case class GetSendString() extends Serializable
@SerialVersionUID(124L)
case class PublishFor(receiverUUID : UUID, message : ZMQMessage) extends Serializable
@SerialVersionUID(125L)
case class DeleteClient(clientUUID : UUID) extends Serializable

