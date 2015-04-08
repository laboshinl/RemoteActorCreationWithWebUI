/**
 * Created by mentall on 08.02.15.
 */


import RemoteSystemActors.RemoteActorCreator
import akka.actor._
import akka.event.Logging
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration._
import core.heartbleed.HeartBleed

object Main extends App {
  val system = ActorSystem("HelloRemoteSystem")
  implicit val timeout: Timeout = 2 second
  val logger = Logging.getLogger(system, this)
  val supervisor    = system.actorOf(Props[Supervisor], "supervisor")
  val remoteActorCreator = Await.result((supervisor ? (Props[RemoteActorCreator], "RemoteActor")),
    timeout.duration).asInstanceOf[ActorRef]
  val rootSystemName = ConfigFactory.load().getString("my.own.root-system-address")
  Await.result((supervisor ? (Props(classOf[HeartBleed], rootSystemName, List(remoteActorCreator)), "HeartBleed")),
    timeout.duration)
  logger.info("Started...")
}

class Supervisor extends Actor {
  import akka.actor.OneForOneStrategy
  import akka.actor.SupervisorStrategy._
  import scala.concurrent.duration._

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case _: NullPointerException     => Restart
      case _: IllegalArgumentException => Stop
      case _: Exception                => Restart
    }

  def receive = {
    case (p: Props, n: String) => sender() ! context.actorOf(p, n)
  }
}



