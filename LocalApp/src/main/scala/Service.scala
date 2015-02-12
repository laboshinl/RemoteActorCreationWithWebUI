/**
 * Created by mentall on 08.02.15.
 */

import akka.actor.Actor.Receive
import org.json4s.{Formats, DefaultFormats}
import spray.httpx.Json4sSupport
import spray.routing.HttpService
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.util.Timeout
import akka.actor._
import spray.can.Http
import akka.pattern.{ ask, pipe }
import spray.http._
import HttpMethods._
import MediaTypes._

class ParrotActor() extends Actor{
  override def receive: Receive = {
    case msg : String => {println(msg+msg+msg+"!"); sender ! msg+msg+msg+"!"}
  }
}

case class whoToGreet(var name: String)
case class Greeting(var msg: String)

case class ActorPathRepresentation(var path: String)
case class ActorPathAndMessageRepresentation(var path: String, var msg: String)

class GreetingActor() extends Actor{
  override def receive: Receive = {
    case whoToGreet(name) => sender ! Greeting("hello, "+name)
  }
}

case class ActorType(var t : String)

class ActorCreator extends Actor{
  override def receive: Receive = {
    case ActorType(t) => if (t == "ParrotActor") {
      println("Got parrotactor, creating")
      sender ! ActorCreated(context.actorOf(Props[ParrotActor]))
    }
  }
}

class WebUIActor(var remoter:ActorRef)
  extends HttpService with Json4sSupport with Actor with ActorLogging with MyBeautifulOutput
{
  import akka.pattern.{ ask, pipe }
  import context.dispatcher

  implicit val timeout: Timeout = 1.second // for the actor 'asks'
  def actorRefFactory = context
  val json4sFormats = DefaultFormats

  val ac = context.actorOf(Props[ActorCreator])

  override def receive = runRoute(route)
//  {
//    case _: Http.Connected => sender ! Http.Register(self)
//
//    case HttpRequest(GET, Uri.Path("/"), _, _, _) =>
//      sender ! index
//
//    case HttpRequest(GET, Uri.Path("/stop"), _, _, _) =>
//      sender ! HttpResponse(entity = "Shutting down in 5 seconds ...")
//      sender ! Http.Close
//      remoter ! StopSystem
//      context.system.scheduler.scheduleOnce(1.second) { context.system.shutdown() }
//
//    case HttpRequest(GET, Uri.Path("/actor"), _, _, _) =>
//      sender ! HttpResponse(entity = "List of available actors")
//
//    case HttpRequest(PUT, Uri.Path("/actor"), _, _, _) =>
//      remoter ! CreateAnotherActor
//      waiter = sender
//
//    case ActorCreated(adr) => {
//      out("started")
//
//      waiter ! HttpResponse(entity = "dasd")
//      ActorJson(id = "123", ref = "23")
//    }
//  }

  lazy val route = {
    path(""){
      get{
        complete{
          index
        }
      }
    }~
      path("actor"){
        get{
          respondWithMediaType(`application/json`){
            complete{
              availableActors
            }
          }
        }~
          put{
            entity(as[ActorType]) {
              at =>
              complete{
                val res = Await.result(ac ? at, timeout.duration).asInstanceOf[ActorCreated]
                HttpResponse(entity = HttpEntity(`text/html`,res.toString))
              }
            }
          }~
          post{
            entity(as[ActorPathAndMessageRepresentation]) {
              ar => complete{
                println("Got message \""+ar.msg+"\" for actor "+ar.path)
                val res = Await.result(context.actorSelection(ar.path) ? ar.msg, timeout.duration)
                HttpResponse(entity = HttpEntity(`text/html`,res.toString))
              }
            }
          }~
          delete{
            entity(as[ActorPathRepresentation]) {
              ar => complete{
                context.actorSelection(ar.path) ! PoisonPill
                HttpResponse(entity = HttpEntity(`text/html`,"PoisonPill sended to actor"))
              }
            }
          }
      }~
      path("system"){
        get{
          complete{
            index
          }
        }~
          put{
            complete {
              index
            }
          }~
          delete{
            complete{
              remoter ! StopSystem
              context.system.scheduler.scheduleOnce(1.second) { context.system.shutdown() }
              sender ! HttpResponse(entity = "Shutting down in 5 seconds ...\n")
              Http.Close
            }
          }
      }
  }

  lazy val index = HttpResponse(
    entity = HttpEntity(`text/html`,
      <html>
        <body>
          <h1>Welcome!</h1>
          <table border="1">
            <caption>Available actions</caption>
            <tr><th>Resource</th><th>GET</th><th>PUT</th><th>POST</th><th>DELETE</th></tr>
            <tr>
              <td>localhost:8080/actor</td>
              <td>List of available actor classes and description</td>
              <td>Create actor of selected type</td>
              <td>Tell message to actor</td>
              <td>Delete actor with selected ActorRef</td>
            </tr>
            <tr>
              <td>localhost:8080/system</td>
              <td>List of available actor systems</td>
              <td>Create additional system on new machine</td>
              <td> - </td>
              <td>Stop actor system</td>
            </tr>
          </table>
        </body>
      </html>.toString()
    )
  )

  val availableActors =
        "{ "+
         "\"parrotActor\":\"Simple actor who respond with yours message\","+
         "\"greetingActor\":{"+
           "\"description\" : \"Actor who would greet you\""+
           "\"messages\":{"+
             "\"whoToGreet(var name: String)\":\"Defines name of greeting subject\","+
             "\"Greeting(var msg: String)\":\"His respond with the message\""+
           "}"+
         "}"+
        "}"
}