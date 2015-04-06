import akka.actor.{ActorRef, Actor}
import akka.event.Logging
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import akka.pattern.ask
import core.messages._

/**
 * Created by mentall on 15.03.15.
 */
class Controller(val actorManager     : ActorRef,
                 val openStackManager : ActorRef,
                 val taskManager      : ActorRef)
  extends Actor
{

  implicit val timeout: Timeout = 2 second
  val logger = Logging.getLogger(context.system, this)

  override def receive: Receive = {
    case PlanActorCreation(actorType)     => planAction(actorManager     ? ActorCreation (actorType))
    case PlanActorTermination(actorId)    => planAction(actorManager     ? ActorTermination(actorId))
    case PlanMachineStart                 => planAction(openStackManager ? MachineStart)
    case PlanMachineTermination(vmId)     => planAction(openStackManager ? MachineTermination(vmId))
    case ActorIdAndMessageToJson(id, msg) => sender ! Await.result(actorManager ? SendMessageToActor(id, msg), timeout.duration)
    case command: RemoteCommand           => actorManager ! command
  }

  def planAction(task : Future[Any]) = {
    val result = Await.result(taskManager ? ManageTask(task), timeout.duration)
    if (!result.isInstanceOf[String]) logger.error("result of ManageTask is not an id : String")
    sender ! result
  }

}
