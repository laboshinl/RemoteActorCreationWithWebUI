/**
 * Created by mentall on 08.02.15.
 */

import akka.io.IO
import scala.concurrent.duration._
import akka.util.Timeout
import akka.actor._
import spray.can.Http
import spray.http._
import HttpMethods._
import MediaTypes._

case object CreateAnotherActor
case object StopSystem
case object Gotcha
case class ActorCreated(val adr: ActorRef)

object Main extends App{
  implicit val system = ActorSystem("LocalSystem")
  val remoter = system.actorOf(Props[RemoteConnection], "Remoter")
  val webUi = system.actorOf(Props(classOf[WebUIActor],remoter), "WebUI")
  IO(Http) ! Http.Bind(webUi, interface = "localhost", port = 8080)
  println("---------STARTED---------")
}

class WebUIActor(var remoter:ActorRef) extends Actor with ActorLogging{
  implicit val timeout: Timeout = 1.second // for the actor 'asks'
  import context.dispatcher

  var tableContent = ""

  override def receive: Receive = {
    case _: Http.Connected => sender ! Http.Register(self)

    case HttpRequest(GET, Uri.Path("/"), _, _, _) =>
      sender ! index

    case HttpRequest(GET, Uri.Path("/stop"), _, _, _) =>
      sender ! HttpResponse(entity = "Shutting down in 5 seconds ...")
      sender ! Http.Close
      remoter ! StopSystem
      context.system.scheduler.scheduleOnce(1.second) { context.system.shutdown() }

    case HttpRequest(GET, Uri.Path("/start"), _, _, _) =>
      remoter ! CreateAnotherActor
      sender ! index

    case ActorCreated(adr) => {println("----ACTOR CREATED----"); tableContent += "<tr>"+adr+"</tr>"}
  }

  var index = HttpResponse(
    entity = HttpEntity(`text/html`,
      <html>
        <body>
          <h1>Say hello to <i>spray-can</i>!</h1>
          <ul>
            <li><a href="/start">/startActorOnRemoteSystem</a></li>
            <li><a href="/stop">/stop</a></li>
          </ul>
          <table>
            {tableContent}
          </table>
        </body>
      </html>.toString()
    )
  )
}

class RemoteConnection extends Actor{
  val remote = context.actorSelection("akka.tcp://HelloRemoteSystem@127.0.0.1:15150/user/RemoteActor")
  var waiter : ActorRef = null

  override def receive: Receive = {
    case CreateAnotherActor => {waiter = sender; remote ! CreateAnotherActor}
    case ActorCreated(adr) =>  adr ! "OK?"
    case Gotcha => {println("----ADDRESS CONFIRMED----"); waiter ! ActorCreated(sender)}
    case StopSystem =>  remote ! StopSystem
  }
}