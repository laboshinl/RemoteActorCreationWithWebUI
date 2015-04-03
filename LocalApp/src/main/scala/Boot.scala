import akka.actor.{ActorRef, Actor, Props, ActorSystem}
import akka.event.Logging
import akka.io.IO
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import spray.can.Http
import akka.pattern.ask
import scala.concurrent.duration._
import scala.concurrent.Await

/**
 * Created by mentall on 12.02.15.
 */
object Boot extends App {
  implicit val system = ActorSystem("LocalSystem")
  implicit val timeout: Timeout = 5 second
  val logger = Logging.getLogger(system, this)
  val supervisor    = system.actorOf(Props[Supervisor], "supervisor")
  val taskManager   = Await.result((supervisor ? Props(classOf[TaskManager])),         timeout.duration).asInstanceOf[ActorRef]
  val remoteSystem  = Await.result((supervisor ? Props(classOf[RemoteSystemManager])), timeout.duration).asInstanceOf[ActorRef]
  val routerManager = Await.result((supervisor ? Props(classOf[RouterManager])),       timeout.duration).asInstanceOf[ActorRef]
  val OSManager     = Await.result((supervisor ? Props(classOf[OpenstackManager])),    timeout.duration).asInstanceOf[ActorRef]
  val actorManager  = Await.result((supervisor ? Props(classOf[ActorManager], routerManager, remoteSystem)),
    timeout.duration).asInstanceOf[ActorRef]
  val controller    = Await.result((supervisor ? Props(classOf[Controller], actorManager, OSManager, taskManager)),
    timeout.duration).asInstanceOf[ActorRef]
  val web           = Await.result((supervisor ? Props(classOf[WebUIActor], controller, taskManager)),
    timeout.duration).asInstanceOf[ActorRef]
  val heartBleed    = Await.result((supervisor ? Props(classOf[HeartBleed], List(remoteSystem, routerManager, actorManager))),
    timeout.duration).asInstanceOf[ActorRef]

  val config = ConfigFactory.load()
  IO(Http) ! Http.Bind(
    web,
    interface = config.getString("my.own.spray-bind-ip"),
    port = config.getInt("my.own.spray-bind-port")
  )
  logger.info("System started...")
}

class Supervisor extends Actor {
  import akka.actor.AllForOneStrategy
  import akka.actor.SupervisorStrategy._
  import scala.concurrent.duration._

  override val supervisorStrategy =
    AllForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case _: NullPointerException => Restart
      case _: IllegalArgumentException => Stop
      case _: Exception => Escalate
    }

  def receive = {
    case p: Props => sender() ! context.actorOf(p, p.actorClass().getName)
  }
}
