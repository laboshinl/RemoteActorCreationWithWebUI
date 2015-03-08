/**
 * Created by mentall on 08.03.15.
 */
trait MessagesOfReceiverActor {

  case class GetMessage[K](val Key: K)

  case class SetMessage[K, V](val Key: K, val Value: V)

  case class NoElementWithSuchKey()

}
