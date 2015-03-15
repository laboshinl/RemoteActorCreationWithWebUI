import akka.actor.{ActorRef, Actor}
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import akka.pattern.ask

/**
 * Created by mentall on 15.03.15.
 */
class Controller(val ActorManager     : ActorRef,
                 val OpenstackManager : ActorRef,
                 val TaskManager      : ActorRef)
  extends Actor with MyBeautifulOutput
{

  implicit val timeout: Timeout = 1 minute

  override def receive: Receive = {
    case PlanActorCreation(actorType)     => planAction(ActorManager ? ActorCreation (actorType))
    case PlanActorTermination(actorId)    => planAction(ActorManager ? ActorTermination(actorId))
    // This is not planning but direct request of String answer
    case ActorIdAndMessageToJson(id, msg) => sender ! Await.result(ActorManager ? SendMessageToActor(id.toLong, msg), timeout.duration)

    case PlanMachineStart             => planAction(OpenstackManager ? MachineStart)
    case PlanMachineTermination(vmId) => planAction(OpenstackManager ? MachineTermination(vmId))
  }

  def planAction(task : Future[Any]) = {
    val result = Await.result( TaskManager ? ManageTask( task ),timeout.duration )
    if (!result.isInstanceOf[Long]) out("result of ManageTask is not an id : Long")
    sender ! result
  }

}
