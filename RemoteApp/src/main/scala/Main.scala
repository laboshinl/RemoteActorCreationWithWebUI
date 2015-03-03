/**
 * Created by mentall on 08.02.15.
 */
import akka.actor._
import scala.concurrent.duration._

object Main extends App with MyBeautifulOutput{
  val system = ActorSystem("HelloRemoteSystem")
  val remoteActor = system.actorOf(Props[RemoteActorCreator], name = "RemoteActor")
  out("started")
}

class RemoteActorCreator extends Actor with MyBeautifulOutput {
  import context.dispatcher
  val remote = context.actorSelection("akka.tcp://LocalSystem@127.0.0.1:11111/user/Remoter")
  remote ! ConnectionRequest

  override def receive = {
    case CreateNewActor(t) =>
      out("Got request for new actor")
      if (t == "ParrotActor") {
        out("Creating parrotActor");
        sender ! ActorCreated(context.system.actorOf(Props[ParrotActor]))
      }
      else sender ! NonexistentActorType
    case StopSystem => context.system.scheduler.scheduleOnce(1.second) {out("shutting down"); context.system.shutdown() }
    case Connected  => out("connected")
  }
}