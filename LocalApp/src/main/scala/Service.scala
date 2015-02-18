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

class WebUIActor(var remoter:ActorRef)
  extends HttpService with Json4sSupport with Actor with ActorLogging with MyBeautifulOutput
{
  import akka.pattern.{ ask, pipe }
  import context.dispatcher

  implicit val timeout: Timeout = 1.second // for the actor 'asks'
  def actorRefFactory = context
  val json4sFormats = DefaultFormats

  override def receive = runRoute(route)

  var uniqueId : Long = 0
  var actors = new scala.collection.mutable.HashMap[Long, ActorRef]

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
            entity(as[ActorTypeToJson]) {
              at =>
              complete{
                val res = Await.result(remoter ? at, timeout.duration).asInstanceOf[ActorCreated]
                uniqueId += 1
                actors += ((uniqueId, res.adr))
                HttpResponse(entity = HttpEntity(`text/html`,uniqueId.toString))
              }
            }
          }~
          post{
            entity(as[ActorIdAndMessageToJson]) {
              ar => complete{
                val target = actors(ar.id.toLong)
                if (target == null){
                  println("Got message, but actor is dead")
                  HttpResponse(entity = HttpEntity(`text/html`,"Got message, but actor is dead\n"))
                }
                else {
                  println("Got message \"" + ar.msg + "\" for actor " + target)
                  val res = Await.result(target ? ar.msg, timeout.duration)
                  println("Actor's response:" + res.toString)
                  HttpResponse(entity = HttpEntity(`text/html`,res.toString))
                }
              }
            }
          }~
          delete{
            entity(as[ActorIdToJson]) {
              ar => complete{
                actors(ar.id.toLong) ! PoisonPill
                actors(ar.id.toLong) = null
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
         "\"parrotActor\":\"Simple actor who respond with yours message\""+
        "}"
}