import java.net.NetworkInterface
import akka.actor.{Props, ActorSystem}
import akka.event.{LoggingAdapter, Logging}

/**
 * Created by baka on 11.03.15.
 */
object Router extends App {
  val system = ActorSystem("RouterSystem")
  val logger : LoggingAdapter = Logging.getLogger(system, this)
  var addresses = NetworkInterface.getNetworkInterfaces.nextElement().getInetAddresses
  addresses.nextElement()
  val address = addresses.nextElement().getHostAddress
  logger.debug("My IP: " + address)
  val remoteActor = system.actorOf(Props(classOf[ReceiverActor], address, "12345"), name = "ReceiverActor")
  logger.debug("Router System Started")
}
