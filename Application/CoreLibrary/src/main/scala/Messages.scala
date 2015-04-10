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

  @SerialVersionUID(202L)
  case class TaskResponse(Status: String, Result: String) extends Serializable
}

trait ActorManagerMessages{
  @SerialVersionUID(126L)
  case class ActorCreationSuccess(Status: String, clientUID : String, subString : String, sendString : String) extends Serializable
}



trait OpenstackManagerMessages{

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

