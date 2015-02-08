/**
 * Created by mentall on 08.02.15.
 */
import akka.actor._
import scala.concurrent.duration._

case object CreateAnotherActor
case object Gotcha
case object StopSystem
case class ActorCreated(val adr: ActorRef)

object Main extends App{
  val system = ActorSystem("HelloRemoteSystem")
  val remoteActor = system.actorOf(Props[RemoteActor], name = "RemoteActor")
  println("---------STARTED---------")
}

class RemoteActor extends Actor {
  import context.dispatcher

  override def receive = {
    case CreateAnotherActor => {println("----CREATE REQUEST----"); sender ! ActorCreated(context.system.actorOf(Props[AnotherActor]))}
    case StopSystem => context.system.scheduler.scheduleOnce(1.second) {println("----SHUTTING DOWN----"); context.system.shutdown() }
  }
}

class AnotherActor extends Actor{

  override def receive = {
    case msg : String => {println("AnotherActor received: "+msg); sender ! Gotcha}
  }
}