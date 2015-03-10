/**
 * Created by mentall on 08.03.15.
 */
package receiver.MessagesOfReceiverActor

import java.io.Serializable

trait MessagesOfReceiverActor {

  case class GetMessage(val Key: String) extends Serializable

  case class SetMessage(val Key: String) extends Serializable

  case class NoElementWithSuchKey() extends Serializable

}
