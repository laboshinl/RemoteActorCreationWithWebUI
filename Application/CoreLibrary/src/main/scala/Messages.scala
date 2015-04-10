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

trait RemoteActorMessages {
  @SerialVersionUID(13L)
  case class CreateNewActor(actorType: String, actorId : String, clientId: String, subString : String, sendString : String) extends Serializable
  @SerialVersionUID(24L)
  case object StopSystem extends Serializable
  @SerialVersionUID(27L)
  case object TellYourIP extends Serializable
  case object DeleteMe
}

trait GeneralMessages {
  @SerialVersionUID(2511L)
  case class Ping(actorUUID: UUID) extends Serializable
  @SerialVersionUID(2512L)
  case class Pong(actorUUID: UUID) extends Serializable
  @SerialVersionUID(26L)
  case object Connected extends Serializable
}

trait HeartBleedMessages {
  @SerialVersionUID(22L)
  object Reconnect extends Serializable
}


