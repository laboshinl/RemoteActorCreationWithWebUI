import java.io.Serializable

import akka.actor.ActorRef

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

@SerialVersionUID(31L)
case class ActorIdAndMessageToJson(var id: String, var msg: String) extends Serializable
@SerialVersionUID(32L)
case class ActorTypeToJson(var actorType: String) extends Serializable
@SerialVersionUID(33L)
case class ActorIdToJson(val id : String) extends Serializable
@SerialVersionUID(38L)
case class TaskIdToJson(val id : String) extends Serializable

@SerialVersionUID(34L)
case object StartMachine extends Serializable
@SerialVersionUID(35L)
case class TerminateMachine(val id : Long) extends Serializable
@SerialVersionUID(36L)
case class MachineTaskCompleted(val id : String) extends Serializable
@SerialVersionUID(37L)
case object NoMachineWithSuchId extends Serializable

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
