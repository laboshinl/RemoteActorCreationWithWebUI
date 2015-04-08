package core.messages;

import java.io.Serializable
import java.util.UUID
import akka.actor.ActorRef
import scala.collection.{mutable, immutable}
import scala.concurrent.Future
import akka.zeromq.ZMQMessage


/**
 * Created by baka on 05.04.15.
 */
trait TaskManagerMessages{
  @SerialVersionUID(74L)
  case object TaskIncomplete extends Serializable
  @SerialVersionUID(75L)
  case object TaskFailed extends Serializable
  @SerialVersionUID(76L)
  case class TaskCompleted() extends Serializable
  @SerialVersionUID(81L)
  case class TaskCompletedWithId(id: String) extends Serializable
  @SerialVersionUID(77L)
  case object NoSuchId extends Serializable
  @SerialVersionUID(72L)
  case class ManageTask(task : Future[Any]) extends Serializable
  @SerialVersionUID(73L)
  case class TaskStatus(taskId : String) extends Serializable
  @SerialVersionUID(202L)
  case class TaskResponse(Status: String, Result: String) extends Serializable
}

trait ActorManagerMessages{



  @SerialVersionUID(22L)
  case object CheckAddress extends Serializable
  @SerialVersionUID(23L)
  case object AddressIsOk extends Serializable

  @SerialVersionUID(126L)
  case class ActorCreationSuccess(Status: String, clientUID : String, subString : String, sendString : String) extends Serializable



}

trait RouterManagerMessages{

  @SerialVersionUID(1113L)
  case object Reconnect
  @SerialVersionUID(230L)
  case class RouterConnectionRequest(uUID: UUID, routingPairs: mutable.HashMap[UUID, UUID]) extends Serializable
  @SerialVersionUID(16L)
  case class GetPairedSocket(Key: String) extends Serializable
  @SerialVersionUID(89L)
  case class GetPairedUser(Key: String) extends Serializable
  @SerialVersionUID(90L)
  case class ResendMsg(resendTo : UUID, msg : ZMQMessage) extends Serializable
  @SerialVersionUID(123L)
  case class GetSendString() extends Serializable
  @SerialVersionUID(14L)
  case class AddPair(clientId : UUID, actorId : UUID)
  @SerialVersionUID(15L)
  case class GetMessage(Key: UUID) extends Serializable
  @SerialVersionUID(16L)
  case class SetMessage(Key: UUID) extends Serializable
  @SerialVersionUID(125L)
  case class DeleteClient(clientUUID : UUID) extends Serializable

}

trait RemoteSystemMessages{
  @SerialVersionUID(201L)
  case object NoRemoteSystems extends Serializable
  @SerialVersionUID(2291L)
  case class RemoteConnectionRequest(uUID: UUID, robotsUUIDMap: immutable.HashMap[UUID, ActorRef]) extends Serializable


}

trait OpenstackManagerMessages{
  @SerialVersionUID(84L)
  case object MachineStart extends Serializable
  @SerialVersionUID(85L)
  case class MachineTermination(vmId : String) extends Serializable
}

trait GeneralMessages {
  @SerialVersionUID(2511L)
  case class Ping(actorUUID: UUID) extends Serializable
  @SerialVersionUID(2512L)
  case class Pong(actorUUID: UUID) extends Serializable
  @SerialVersionUID(26L)
  case object Connected extends Serializable
}

trait unused{

  @SerialVersionUID(17L)
  case class NoElementWithSuchKey() extends Serializable
}
/**
 * сообщения внутри JVM можно делать без сериализации
 */

