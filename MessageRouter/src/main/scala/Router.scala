import java.net.NetworkInterface
import akka.actor.{ActorRef, Actor, Props, ActorSystem}
import akka.event.{LoggingAdapter, Logging}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import akka.pattern.ask
import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Created by baka on 11.03.15.
 */
object Router extends App {
  val system = ActorSystem("RouterSystem")
  implicit val timeout: Timeout = 2 second
  val logger : LoggingAdapter = Logging.getLogger(system, this)
  var addresses = NetworkInterface.getNetworkInterfaces.nextElement().getInetAddresses
  addresses.nextElement()
  val address = addresses.nextElement().getHostAddress
  logger.debug("My IP: " + address)
  val port = ConfigFactory.load().getString("my.own.port")
  val poolSize = ConfigFactory.load().getInt("my.own.pool-size")
  val supervisor = system.actorOf(Props[Supervisor], "supervisor")
  val routingInfoActor = Await.result((supervisor ? (Props(classOf[RoutingInfoActor], address, port),
    "RoutingInfoActor")), timeout.duration).asInstanceOf[ActorRef]

  /**
   * На входе нельзя ставить пулл обработчиков. Возможен реордеринг сообщений.
   * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
   *
   */
  val receiver = Await.result((supervisor ? (Props(classOf[ReceiverActor], address, port, routingInfoActor), "ReceiverActor")),
    timeout.duration).asInstanceOf[ActorRef]
  val heartBleed = Await.result((supervisor ? (Props(classOf[RouterHeartBleed], system.name, List(routingInfoActor)),
    "HeartBleed")), timeout.duration).asInstanceOf[ActorRef]
  logger.debug("Router System Started")
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
