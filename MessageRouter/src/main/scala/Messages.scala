/**
 * Created by mentall on 08.03.15.
 */

import java.io.Serializable

trait MessagesOfReceiverActor {

  case class GetMessage[K](val Key: K) extends Serializable

  case class SetMessage[K, V](val Key: K, val Value: V) extends Serializable

  case class NoElementWithSuchKey() extends Serializable

}
