import java.net.NetworkInterface
import akka.actor.{Props, ActorSystem}
import akka.event.{LoggingAdapter, Logging}
import akka.routing.RoundRobinPool
import com.typesafe.config.ConfigFactory

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
  val port = ConfigFactory.load().getString("my.own.port")
  val poolSize = ConfigFactory.load().getInt("my.own.pool-size")
  val routingInfoActor = system.actorOf(Props(classOf[RoutingInfoActor], address, port), name = "RoutingInfoActor")
  /**
   * На входе нельзя ставить пулл обработчиков. Возможен реордеринг сообщений.
   * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
   *
   */
  val remoteActor = system.actorOf(Props(classOf[ReceiverActor], address, port, routingInfoActor), name = "ReceiverActor")
  logger.debug("Router System Started")
}
