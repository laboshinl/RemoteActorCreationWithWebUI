import akka.actor.{Props, ActorSystem}
import akka.io.IO
import com.typesafe.config.ConfigFactory
import spray.can.Http

/**
 * Created by mentall on 12.02.15.
 */
object Boot extends App with MyBeautifulOutput{
    implicit val system = ActorSystem("LocalSystem")
    val r = system.actorOf(Props[RemoteConnection], "Remoter")
    val o = system.actorOf(Props[OpenstackActor], "Openstack")
    val webUi = system.actorOf(Props(classOf[WebUIActor], r, o), "WebUI")
    val config = ConfigFactory.load()
    IO(Http) ! Http.Bind(webUi,
      interface = config.getString("my.own.spray-bind-ip"),
      port = config.getInt("my.own.spray-bind-port"))
    out("started")
}
