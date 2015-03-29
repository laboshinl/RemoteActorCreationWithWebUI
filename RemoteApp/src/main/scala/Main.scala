/**
 * Created by mentall on 08.02.15.
 */

import java.net.{InetAddress, NetworkInterface}

import akka.actor._
import akka.event.Logging
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._
import scala.collection.JavaConversions._

object Main extends App {

  val system = ActorSystem("HelloRemoteSystem")
  val logger = Logging.getLogger(system, this)
  val remoteActor = system.actorOf(Props[RemoteActorCreator], name = "RemoteActor")
  logger.info("Started...")
}

class RemoteActorCreator extends Actor {
  val address = NetworkInterface.getNetworkInterfaces.next.getInetAddresses.toList.get(1).getHostAddress
  val logger = Logging.getLogger(context.system, this)
  import context.dispatcher
  val remote = context.actorSelection(ConfigFactory.load().getString("my.own.master-address"))
  remote ! ConnectionRequest

  override def receive = {
    case CreateNewActor(t, id, subString, sendString) =>
      logger.debug("Got CreateNewActor request")
      if (t == "ParrotActor") {
        logger.debug("Creating new Parrot Actor: " + id)
        sender ! ActorCreated(context.system.actorOf(Props(classOf[ParrotActor], id, subString, sendString)))
      } else if (t == "CommandProxy") {
        logger.debug("Creating new CommandProxy Actor: {}", id)
        sender ! ActorCreated(context.system.actorOf(Props(classOf[CommandProxyActor], id, subString, sendString)))
      }
      else
        sender ! NonexistentActorType
    case StopSystem => context.system.scheduler.scheduleOnce(1.second) {
      logger.info("Terminating system..." + context.system.toString)
      context.system.shutdown()
    }
    case Connected  => logger.info("Connected to main actor system...")
    case TellYourIP => sender ! MyIPIs(address)
  }
}