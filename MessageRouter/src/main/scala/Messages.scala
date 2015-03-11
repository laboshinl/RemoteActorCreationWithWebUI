/**
 * Created by mentall on 08.03.15.
 */
import java.io.Serializable


case object ConnectionRequest extends Serializable
case object Connected extends Serializable
case class GetMessage(val Key: String) extends Serializable
case class SetMessage(val Key: String) extends Serializable
case class NoElementWithSuchKey() extends Serializable
