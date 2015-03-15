import java.io.Serializable
import akka.actor.ActorRef
import scala.concurrent.Future

/**
 * Created by mentall on 13.02.15.
 */
@SerialVersionUID(13L)
case class CreateNewActor(var actorType: String, var actorId : String, var subString : String, var sendString : String) extends Serializable
@SerialVersionUID(12L)
case class ActorCreated(val adr: ActorRef) extends Serializable{
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
@SerialVersionUID(25L)
case object ConnectionRequest extends Serializable
@SerialVersionUID(26L)
case object Connected extends Serializable

@SerialVersionUID(27L)
case object TellYourIP extends Serializable
@SerialVersionUID(28L)
case class MyIPIs (val IP : String) extends Serializable

//types used in marshalling json
@SerialVersionUID(78L)
case class ActorIdAndMessageToJson(var id: String, var msg: String) extends Serializable
case class ActorTypeToJson(var actorType: String) extends Serializable
case class IdToJson(val Id : String) extends Serializable

@SerialVersionUID(34L)
case object PlanMachineStart extends Serializable
@SerialVersionUID(35L)
case class PlanMachineTermination(val id : Long) extends Serializable
@SerialVersionUID(84L)
case object MachineStart extends Serializable
@SerialVersionUID(85L)
case class MachineTermination(val id : Long) extends Serializable

@SerialVersionUID(39L)
case class RegisterPair(val clientID : String, val actorID : String) extends Serializable
@SerialVersionUID(40L)
case class PairRegistered(val clientSubStr : String, val actorSubStr : String, val sendString : String) extends Serializable
@SerialVersionUID(41L)
case object NoRouters extends Serializable

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

@SerialVersionUID(71L)
case class PlanActorCreation(actorType : String) extends Serializable
@SerialVersionUID(72L)
case class ManageTask(task : Future[Any]) extends Serializable
@SerialVersionUID(73L)
case class TaskStatus(taskId : Long) extends Serializable
@SerialVersionUID(74L)
case object TaskIncomplete extends Serializable
@SerialVersionUID(75L)
case object TaskFailed extends Serializable
@SerialVersionUID(76L)
case class TaskCompleted() extends Serializable
@SerialVersionUID(77L)
case object NoSuchId extends Serializable
@SerialVersionUID(79L)
case class SendMessageToActor(val id: Long, val msg: String) extends Serializable
@SerialVersionUID(80L)
case class PlanActorTermination(val id: Long) extends Serializable
@SerialVersionUID(81L)
case class TaskCompletedWithId(val id: Long) extends Serializable
@SerialVersionUID(82L)
case class ActorCreation(actorType : String) extends Serializable
@SerialVersionUID(83L)
case class ActorTermination(val id: Long) extends Serializable