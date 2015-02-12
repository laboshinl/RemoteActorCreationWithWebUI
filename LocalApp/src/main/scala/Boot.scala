import akka.actor.{Props, ActorSystem}
import akka.io.IO
import spray.can.Http

/**
 * Created by mentall on 12.02.15.
 */
object Boot extends App with MyBeautifulOutput{
    implicit val system = ActorSystem("LocalSystem")
    val remoter = system.actorOf(Props[RemoteConnection], "Remoter")
    val webUi = system.actorOf(Props(classOf[WebUIActor],remoter), "WebUI")
    IO(Http) ! Http.Bind(webUi, interface = "localhost", port = 8080)
    out("started")
}
