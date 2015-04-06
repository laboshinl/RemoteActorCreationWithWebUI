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

@SerialVersionUID(13L)
case class CreateNewActor(actorType: String, actorId : String, clientId: String, subString : String, sendString : String) extends Serializable
@SerialVersionUID(12L)
case class ActorCreated(adr: ActorRef) extends Serializable{
  override def toString = "ActorRef:"+adr
}
@SerialVersionUID(21L)
case object NonexistentActorType extends Serializable
@SerialVersionUID(22L)
case object CheckAddress extends Serializable
@SerialVersionUID(23L)
case object AddressIsOk extends Serializable
@SerialVersionUID(24L)
case object StopSystem extends Serializable
@SerialVersionUID(26L)
case object Connected extends Serializable

@SerialVersionUID(27L)
case object TellYourIP extends Serializable
@SerialVersionUID(28L)
case class MyIPIs (IP : String) extends Serializable

@SerialVersionUID(84L)
case object MachineStart extends Serializable
@SerialVersionUID(85L)
case class MachineTermination(vmId : String) extends Serializable

@SerialVersionUID(39L)
case class RegisterPair(clientId : UUID, actorId : UUID) extends Serializable
@SerialVersionUID(40L)
case class PairRegistered(clientSubStr : String, actorSubStr : String, sendString : String) extends Serializable
@SerialVersionUID(41L)
case object NoRouters extends Serializable

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
@SerialVersionUID(79L)
case class SendMessageToActor(actorId: String, msg: String) extends Serializable
@SerialVersionUID(82L)
case class ActorCreation(actorType : String) extends Serializable
@SerialVersionUID(83L)
case class ActorTermination(actorId: String) extends Serializable

@SerialVersionUID(125L)
case class DeleteClient(clientUUID : UUID) extends Serializable

@SerialVersionUID(126L)
case class ActorCreationSuccess(Status: String, clientUID : String, subString : String, sendString : String) extends Serializable


@SerialVersionUID(201L)
case object NoRemoteSystems extends Serializable
@SerialVersionUID(202L)
case class TaskResponse(Status: String, Result: String) extends Serializable

@SerialVersionUID(228L)
case class RemoteCommand(clientUID: String, command: String, args: immutable.List[String]) extends Serializable

/**
 * сообщения внутри JVM можно делать без сериализации
 */

@SerialVersionUID(229L)
case class RemoteConnectionRequest(uUID: UUID, robotsUUIDMap: immutable.HashMap[UUID, ActorRef]) extends Serializable
@SerialVersionUID(230L)
case class RouterConnectionRequest(uUID: UUID, routingPairs: mutable.HashMap[UUID, UUID]) extends Serializable

@SerialVersionUID(251L)
case class Ping(actorUUID: UUID) extends Serializable
@SerialVersionUID(251L)
case class Pong(actorUUID: UUID) extends Serializable

/**
 * куски из роутера
 *
 */


@SerialVersionUID(16L)
case class GetPairedSocket(Key: String) extends Serializable
@SerialVersionUID(89L)
case class GetPairedUser(Key: String) extends Serializable
@SerialVersionUID(90L)
case class ResendMsg(resendTo : UUID, msg : ZMQMessage) extends Serializable

case object Reconnect

/**
 * куски из ремот аппа
 */
