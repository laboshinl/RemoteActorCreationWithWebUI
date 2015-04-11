import akka.actor.{ActorRef, Actor}
import akka.event.Logging
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import akka.pattern.ask
import core.messages._
import core.messages.Controller._

/**
 * Created by mentall on 15.03.15.
 */
class Controller(val actorManager     : ActorRef,
                 val openStackManager : ActorRef,
                 val taskManager      : ActorRef) extends Actor
{

  implicit val timeout: Timeout = 2 second
  val logger = Logging.getLogger(context.system, this)

  override def receive: Receive = {
    case PlanActorCreation(actorType)     => planAction(actorManager     ? ActorManager.ActorCreation(actorType))
    case PlanActorTermination(actorId)    => planAction(actorManager     ? ActorManager.ActorTermination(actorId))
    case PlanMachineStart                 => planAction(openStackManager ? OpenStackManager.MachineStart)
    case PlanMachineTermination(vmId)     => planAction(openStackManager ? OpenStackManager.MachineTermination(vmId))
    case ActorIdAndMessageToJson(id, msg) => sender() ! Await.result(actorManager ? ActorManager.SendMessageToActor(id, msg), timeout.duration)
    case RemoteCommand(uid, com, arg)     => actorManager ! ActorManager.RemoteCommand(uid, com, arg)
  }

  def planAction(task : Future[Any]) = {
    Await.result(taskManager ? TaskManager.ManageTask(task), timeout.duration) match {
      case TaskCreated(id)  => sender() ! WebUi.TaskCreated(id)
      case msg              => sender() ! General.FAIL("Strange result")
    }
  }

}
