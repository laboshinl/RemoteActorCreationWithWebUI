import java.io.Serializable

import akka.actor.ActorRef

/**
 * Created by mentall on 13.02.15.
 */
case class CreateNewActor(var actorType: String) extends Serializable
case class ActorCreated(val adr: ActorRef) extends Serializable{
  override def toString = "ActorRef:"+adr
}
case object CheckAddress extends Serializable
case object AddressIsOk extends Serializable
case object StopSystem extends Serializable
case object ConnectionRequest extends Serializable
case object Connected extends Serializable

case class ActorIdAndMessageToJson(var id: String, var msg: String) extends Serializable
case class ActorTypeToJson(var t : String) extends Serializable
case class ActorIdToJson(var id : String) extends Serializable
