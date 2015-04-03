import java.io.Serializable
import java.util.UUID

import akka.actor.ActorRef

import scala.collection.immutable

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
case class MyIPIs (IP : String) extends Serializable

@SerialVersionUID(228L)
case class RemoteCommand(clientUID: String, command: String, args: immutable.List[String]) extends Serializable

@SerialVersionUID(229L)
case class RemoteConnectionRequest(robotsUUIDMap: immutable.HashMap[UUID, ActorRef]) extends Serializable
case object Reconnect
@SerialVersionUID(250L)
case class DeleteActor(actorUUID: UUID) extends Serializable