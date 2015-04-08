package LocalAppActors

import akka.actor.{Actor, ActorRef}
import akka.event.Logging
import akka.pattern.ask
import akka.util.Timeout
import core.messages._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
 * Created by mentall on 15.03.15.
 */
class Controller(val actorManager     : ActorRef,
                 val openStackManager : ActorRef,
                 val taskManager      : ActorRef)
  extends Actor with ActorManagerMessages with OpenstackManagerMessages with TaskManagerMessages
{

  implicit val timeout: Timeout = 2 second
  val logger = Logging.getLogger(context.system, this)

  override def receive: Receive = {
    case ("runVMsWithRemoteAppAndMessageRouter") => planAction(openStackManager ? ("startRemoteAppAndMessageRouter"))
    case PlanActorCreation(actorType)                 => planAction(ActorManager.createActor(actorManager, actorType))
    case PlanActorTermination(actorId)                => planAction(ActorManager.deleteActor(actorManager, actorId))
    case PlanMachineStart                             => planAction(openStackManager ? MachineStart)
    case PlanMachineTermination(vmId)                 => planAction(openStackManager ? MachineTermination(vmId))
    case ActorIdAndMessageToJson(id, msg)             => sender ! Await.result(ActorManager.sendMessageToActor(actorManager, id, msg), timeout.duration)
    case RemoteCommand(uUID, command, args)           => ActorManager.sendRemoteCommand(actorManager, uUID, command, args)
  }

  def planAction(task : Future[Any]) = {
    val result = Await.result(taskManager ? ManageTask(task), timeout.duration)
    if (!result.isInstanceOf[String]) logger.error("result of ManageTask is not an id : String")
    sender ! result
  }

}
