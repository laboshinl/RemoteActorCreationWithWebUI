package LocalAppActors

import java.io.Serializable

import akka.actor.{Actor, ActorRef}
import akka.event.Logging
import akka.pattern.ask
import akka.util.Timeout

import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait ControllerMessages{
  @SerialVersionUID(1228L)
  case class RemoteCommand(clientUID: String, command: String, args: immutable.List[String]) extends Serializable
  case class PlanActorTermination(actorId: String)
  case class PlanActorCreation(actorType : String)
  case object PlanMachineStart
  case class PlanMachineTermination(vmId : String)
  @SerialVersionUID(122L)
  case class ActorIdAndMessageToJson(var id: String, var msg: String) extends Serializable
}

/**
 * Created by mentall on 15.03.15.
 */

trait CTimeout {
  implicit val timeout: Timeout = 5 seconds
}

object Controller extends ControllerMessages with CTimeout {

  def planActorCreation(receiver: ActorRef, actorType: String) : Future[Any] = {
    receiver ? PlanActorCreation(actorType)
  }

  def planActorTermination(receiver: ActorRef, actorId: String) : Future[Any] = {
    receiver ? PlanActorTermination(actorId)
  }

  def planMachineStart(receiver: ActorRef) : Future[Any] = {
    receiver ? PlanMachineStart
  }

  def planMachineTermination(receiver: ActorRef, vmId : String) : Future[Any] = {
    receiver ? PlanMachineTermination(vmId)
  }

  def sendMessageToActor(receiver: ActorRef, id: String, msg: String) : Future[Any] = {
    receiver ? ActorIdAndMessageToJson
  }

  def sendRemoteCommand(receiver: ActorRef, uUID : String, command: String, args: immutable.List[String]) : Future[Any] = {
    receiver ? RemoteCommand(uUID, command, args)
  }
}

class Controller(val actorManager     : ActorRef,
                 val openStackManager : ActorRef,
                 val taskManager      : ActorRef)
  extends Actor with CTimeout with ControllerMessages
{
  val logger = Logging.getLogger(context.system, this)

  override def receive: Receive = {
    // Message router пока что может быть запущен только на той же машине что и LocalApp
    // ибо белый ip надо.
    case ("runVMsWithRemoteAppAndMessageRouter")      => planAction(openStackManager ? ("startRemoteAppAndMessageRouter"))
    case PlanActorCreation(actorType)                 => planAction(ActorManager.createActor(actorManager, actorType))
    case PlanActorTermination(actorId)                => planAction(ActorManager.deleteActor(actorManager, actorId))
    case PlanMachineStart                             => planAction(OpenStackManager.startMachine(openStackManager))
    case PlanMachineTermination(vmId)                 => planAction(OpenStackManager.terminateMachine(openStackManager, vmId))
    case ActorIdAndMessageToJson(id, msg)             => sender ! Await.result(ActorManager.sendMessageToActor(actorManager, id, msg), timeout.duration)
    case RemoteCommand(uUID, command, args)           => ActorManager.sendRemoteCommand(actorManager, uUID, command, args)
  }

  def planAction(task : Future[Any]) = {
    val result = Await.result(TaskManager.manageTask(actorManager, task),
      timeout.duration)
    if (!result.isInstanceOf[String]) logger.error("result of ManageTask is not an id : String")
    sender() ! result
  }

}

