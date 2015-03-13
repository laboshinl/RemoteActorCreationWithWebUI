/**
 * Created by mentall on 08.03.15.
 */
import java.io.Serializable


@SerialVersionUID(25L)
case object ConnectionRequest extends Serializable
@SerialVersionUID(26L)
case object Connected extends Serializable
@SerialVersionUID(14L)
case class AddPair(val clientId : String, val actorId : String)
@SerialVersionUID(15L)
case class GetMessage(val Key: String) extends Serializable
@SerialVersionUID(16L)
case class SetMessage(val Key: String) extends Serializable
@SerialVersionUID(17L)
case class NoElementWithSuchKey() extends Serializable
@SerialVersionUID(123L)
case class GetSendString() extends Serializable
