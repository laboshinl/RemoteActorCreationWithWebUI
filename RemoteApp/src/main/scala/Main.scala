/**
 * Created by mentall on 08.02.15.
 */

import java.net.{InetAddress, NetworkInterface}

import akka.actor._
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._
import scala.collection.JavaConversions._

object Main extends App with MyBeautifulOutput{
  val system = ActorSystem("HelloRemoteSystem")
  val remoteActor = system.actorOf(Props[RemoteActorCreator], name = "RemoteActor")
  out("started")
}

class RemoteActorCreator extends Actor with MyBeautifulOutput {
  val address = NetworkInterface.getNetworkInterfaces.next.getInetAddresses.toList.get(1).getHostAddress

  import context.dispatcher
  val remote = context.actorSelection(ConfigFactory.load().getString("my.own.master-address"))
  remote ! ConnectionRequest

  override def receive = {
    case CreateNewActor(t, id, subString, sendString) =>
      out("Got request for new actor")
      if (t == "ParrotActor") {
        out("Creating parrotActor");
        //TODO: create actor with id
        sender ! ActorCreated(context.system.actorOf(Props(classOf[ParrotActor], id, subString, sendString)))
      }
      else sender ! NonexistentActorType
    case StopSystem => context.system.scheduler.scheduleOnce(1.second) {out("shutting down"); context.system.shutdown() }
    case Connected  => out("connected")
    case TellYourIP => sender ! MyIPIs(address)
  }
}