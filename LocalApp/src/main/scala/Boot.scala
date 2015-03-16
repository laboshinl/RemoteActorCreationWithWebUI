import akka.actor.{Props, ActorSystem}
import akka.event.Logging
import akka.io.IO
import com.typesafe.config.ConfigFactory
import spray.can.Http

/**
 * Created by mentall on 12.02.15.
 */
object Boot extends App {
  implicit val system = ActorSystem("LocalSystem")
  val logger = Logging.getLogger(system, this)
  val tm = system.actorOf(Props[TaskManager]                   , "TaskManager"    )
  val r  = system.actorOf(Props[RemoteConnection]              , "Remoter"        )
  val rp = system.actorOf(Props[RouterManager]                 , "RoutersProvider")
  val om = system.actorOf(Props(classOf[OpenstackManager]     ), "Openstack"      )
  val am = system.actorOf(Props(classOf[ActorManager],rp, r   ), "ActorManager"   )
  val c  = system.actorOf(Props(classOf[Controller],am, om, tm), "Controller"     )
  val w  = system.actorOf(Props(classOf[WebUIActor],c , tm    ), "WebUI"          )

  val config = ConfigFactory.load()
  IO(Http) ! Http.Bind(
    w,
    interface = config.getString("my.own.spray-bind-ip"),
    port = config.getInt("my.own.spray-bind-port")
  )
  logger.info("System started...")
}
