import java.io.Serializable

import akka.actor.ActorRef

/**
 * Created by mentall on 13.02.15.
 */

case class CreateNewActor(var actorType: String, var actorId : String, var tcp : String) extends Serializable
case class ActorCreated(val adr: ActorRef) extends Serializable{
  override def toString = "ActorRef:"+adr
}
case object NonexistentActorType extends Serializable
case object CheckAddress extends Serializable
case object AddressIsOk extends Serializable
case object StopSystem extends Serializable
case object ConnectionRequest extends Serializable
case object Connected extends Serializable

case object TellYourIP extends Serializable
case class MyIPIs (val IP : String) extends Serializable