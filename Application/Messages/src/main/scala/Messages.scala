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

object ActorManager {
  @SerialVersionUID(82L)
  case class ActorCreation(actorType : String) extends Serializable
  @SerialVersionUID(83L)
  case class ActorTermination(actorId: String) extends Serializable
  @SerialVersionUID(79L)
  case class SendMessageToActor(actorId: String, msg: String) extends Serializable
  @SerialVersionUID(228L)
  case class RemoteCommandWithId(clientUID: String, command: String, args: immutable.List[String]) extends Serializable
  @SerialVersionUID(229L)
  case class UpdateActors(robotsUUIDMap: immutable.HashMap[UUID, ActorRef]) extends Serializable
  @SerialVersionUID(12L)
  case class ActorCreated(adr: ActorRef) extends Serializable
  @SerialVersionUID(21L)
  case object NonexistentActorType extends Serializable
  @SerialVersionUID(22L)
  case object NoRouters extends Serializable
  @SerialVersionUID(22L)
  case object NoRemoteSystems extends Serializable
  @SerialVersionUID(22L)
  case class PairRegistered(clientSubStr: String, actorSubStr: String, sendString: String) extends Serializable
  @SerialVersionUID(1228L)
  case class RemoteCommand(clientUID: String, command: String, args: immutable.List[String]) extends Serializable
}

object Controller {
  @SerialVersionUID(1228L)
  case class RemoteCommand(clientUID: String, command: String, args: immutable.List[String]) extends Serializable
  @SerialVersionUID(122L)
  case class PlanActorTermination(actorId: String) extends Serializable
  @SerialVersionUID(122L)
  case class PlanActorCreation(actorType : String) extends Serializable
  @SerialVersionUID(122L)
  case object PlanMachineStart extends Serializable
  @SerialVersionUID(122L)
  case class PlanMachineTermination(vmId : String) extends Serializable
  @SerialVersionUID(122L)
  case class ActorIdAndMessageToJson(var id: String, var msg: String) extends Serializable
  @SerialVersionUID(122L)
  case class TaskCreated(id: String) extends Serializable
}


object OpenStackManager {
  @SerialVersionUID(84L)
  case object MachineStart extends Serializable
  @SerialVersionUID(85L)
  case class MachineTermination(vmId : String) extends Serializable
}

object RemoteSystemManager {
  @SerialVersionUID(2291L)
  case class RemoteConnectionRequest(uUID: UUID, robotsUUIDMap: immutable.HashMap[UUID, ActorRef]) extends Serializable
  @SerialVersionUID(28L)
  case class MyIPIs (ip : String) extends Serializable
  @SerialVersionUID(31L)
  case object StopAllSystems extends Serializable
  @SerialVersionUID(32L)
  case class CreateActor(actorType: String, actorId : String, clientId: String, subString : String, sendString : String) extends Serializable
  @SerialVersionUID(33L)
  case class ActorCreatedReply(actorRef: ActorRef) extends Serializable
  @SerialVersionUID(34L)
  case object NonexistentActorType extends Serializable
  @SerialVersionUID(34L)
  case object ActorManagerStarted extends Serializable
}

object RouterManager {
  @SerialVersionUID(230L)
  case class RouterConnectionRequest(uUID: UUID, routingPairs: mutable.HashMap[UUID, UUID]) extends Serializable
  @SerialVersionUID(125L)
  case class DeleteClient(clientUUID : UUID) extends Serializable
  @SerialVersionUID(126L)
  case class RegisterPair(clientUUID : UUID, actorUUID : UUID) extends Serializable
  @SerialVersionUID(126L)
  case class UnregisterPair(clientUUID: UUID, actorUUID: UUID) extends Serializable
}

object TaskManager {
  @SerialVersionUID(73L)
  case class ManageTask(task : Future[Any]) extends Serializable
  @SerialVersionUID(74L)
  case class TaskStatus(taskId : String) extends Serializable
  @SerialVersionUID(75L)
  case class MachineStarted(vmId: String) extends Serializable
  @SerialVersionUID(75L)
  case class MachineTerminated(vmId: String) extends Serializable
  @SerialVersionUID(122L)
  case class ActorCreationSuccess(clientId: String, clientSubStr: String, sendString: String) extends Serializable
  @SerialVersionUID(122L)
  case class ActorDeleted(actorUUID: String) extends Serializable
}

object WebUi {
  @SerialVersionUID(122L)
  case class ActorIdAndMessageToJson(id: String, msg: String) extends Serializable
  @SerialVersionUID(122L)
  case object TaskIncomplete extends Serializable
  @SerialVersionUID(122L)
  case class ActorCreationSuccess(clientId: String, clientSubStr: String, sendString: String) extends Serializable
  @SerialVersionUID(122L)
  case class ActorTypeToJson(actorType: String) extends Serializable
  @SerialVersionUID(122L)
  case class IdToJson(Id: String) extends Serializable
  @SerialVersionUID(1228L)
  case class RemoteCommand(clientUID: String, command: String, args: immutable.List[String]) extends Serializable
  @SerialVersionUID(75L)
  case class MachineStarted(vmId: String) extends Serializable
  @SerialVersionUID(75L)
  case class MachineTerminated(vmId: String) extends Serializable
  @SerialVersionUID(122L)
  case class ActorDeleted(actorUUID: String) extends Serializable
  @SerialVersionUID(122L)
  case class TaskCreated(id: String) extends Serializable
}

object RoutingInfo {
  @SerialVersionUID(126L)
  case class DeleteClient(clientUUID : UUID) extends Serializable
  @SerialVersionUID(123L)
  case object GetSendString extends Serializable
  @SerialVersionUID(14L)
  case class AddPair(clientUUID : UUID, actorUUID : UUID) extends Serializable
  @SerialVersionUID(15L)
  case class GetMessage(Key: UUID) extends Serializable
  @SerialVersionUID(16L)
  case class SetMessage(Key: UUID) extends Serializable
}

object RemoteActor {
  @SerialVersionUID(13L)
  case class CreateNewActor(actorType: String, actorId : String, clientId: String, subString : String, sendString : String) extends Serializable
  @SerialVersionUID(24L)
  case object StopSystem extends Serializable
  @SerialVersionUID(27L)
  case object TellYourIP extends Serializable
  @SerialVersionUID(122L)
  case object DeleteMe extends Serializable
}

object Robot {
  @SerialVersionUID(1267L)
  case class RemoteCommand(command: String, args: immutable.List[String]) extends Serializable
}

object General {
  @SerialVersionUID(2511L)
  case class Ping(actorUUID: UUID) extends Serializable
  @SerialVersionUID(2512L)
  case class Pong(actorUUID: UUID) extends Serializable
  @SerialVersionUID(26L)
  case object Connected extends Serializable
  @SerialVersionUID(32L)
  case object OK extends Serializable
  @SerialVersionUID(33L)
  case class FAIL(issuse: String) extends Serializable
}

object HeartBleed {
  @SerialVersionUID(22L)
  object Reconnect extends Serializable
}