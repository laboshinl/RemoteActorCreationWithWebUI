import java.io.Serializable

import akka.actor.ActorRef

/**
 * Created by mentall on 13.02.15.
 */
@SerialVersionUID(13L)
case class CreateNewActor(var actorType: String, var actorId : String, var tcp : String) extends Serializable
@SerialVersionUID(12L)
case class ActorCreated(val adr: ActorRef) extends Serializable{
  override def toString = "ActorRef:"+adr
}
case object NonexistentActorType extends Serializable
case object CheckAddress extends Serializable
case object AddressIsOk extends Serializable
case object StopSystem extends Serializable
case object ConnectionRequest extends Serializable
case object Connected extends Serializable

case class ActorIdAndMessageToJson(var id: String, var msg: String) extends Serializable
case class ActorTypeToJson(var actorType: String) extends Serializable
case class ActorIdToJson(val id : String) extends Serializable

case object StartMachine extends Serializable
case class TerminateMachine(val id : Long) extends Serializable
case class MachineTaskCompleted(val id : String) extends Serializable
case object NoMachineWithSuchId extends Serializable

case class TaskIdToJson(val id : String) extends Serializable

case object TellYourIP extends Serializable
case class MyIPIs (val IP : String) extends Serializable

case class RegisterPair(val clientID : String, val actorID : String) extends Serializable
case class PairRegistered(val clientStr : String, val actorStr : String) extends Serializable
case object NoRouters extends Serializable

@SerialVersionUID(14L)
case class AddPair(val clientId : String, val actorId : String)
@SerialVersionUID(15L)
case class GetMessage(val Key: String) extends Serializable
@SerialVersionUID(16L)
case class SetMessage(val Key: String) extends Serializable
@SerialVersionUID(17L)
case class NoElementWithSuchKey() extends Serializable
