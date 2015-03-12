/**
 * Created by mentall on 08.03.15.
 */
import java.io.Serializable


case object ConnectionRequest extends Serializable
case object Connected extends Serializable
@SerialVersionUID(14L)
case class AddPair(val clientId : String, val actorId : String)
@SerialVersionUID(15L)
case class GetMessage(val Key: String) extends Serializable
@SerialVersionUID(16L)
case class SetMessage(val Key: String) extends Serializable
@SerialVersionUID(17L)
case class NoElementWithSuchKey() extends Serializable
