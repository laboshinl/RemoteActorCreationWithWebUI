/**
 * Created by mentall on 08.02.15.
 */

import org.json4s.DefaultFormats
import spray.httpx.Json4sSupport
import spray.httpx.marshalling.ToResponseMarshallable
import spray.routing.HttpService
import scala.concurrent.{ExecutionContextExecutor, Future, Await}
import scala.concurrent.duration._
import akka.util.Timeout
import akka.actor._
import akka.pattern.ask
import spray.http._
import MediaTypes._
import language.postfixOps

class WebUIActor(val Controller : ActorRef, val TaskManager : ActorRef)
  extends HttpService with Json4sSupport with Actor
{
  //Statements required by traits
  implicit def executionContext : ExecutionContextExecutor = actorRefFactory.dispatcher
  def actorRefFactory = context
  implicit val timeout: Timeout = 1 minute
  val json4sFormats = DefaultFormats
  override def receive = runRoute(route)

  //This is the tree of all working routes that answer to user's requests
  lazy val route = {
    //TODO: create DISCONNECT request from client
    path(""){ get{ complete{ index }  }  }~
    path("actor"){
      get{ respondWithMediaType(`application/json`){ complete{  availableActors } } }~
      put{
        entity(as[ActorTypeToJson]) {
          actorType => complete{ planActorOnRemoteMachine(actorType) }
        }
      }~
      post{
        entity(as[ActorIdAndMessageToJson]) {
          ar => complete{ sendMessageToActorOnRemoteMachine(ar) }
        }
      }~
      delete{
        entity(as[IdToJson]) {
          ar => complete{ planActorDeletion(ar) }
        }
      }
    }~
    path("command") {
      post{
        entity(as[RemoteCommand]) {
          rc => complete{ sendRemoteCommandToActor(rc) }
        }
      }
    }~
    path("system"){
      get{ complete{ index } }~
      put{ complete { planMachineCreation } }~
      delete{ entity(as[IdToJson]) { ar => complete { planMachineDeletion(ar) } } }
    }~
    path("task"){
      get{ entity(as[IdToJson]) { ar => respondWithMediaType(`application/json`){ complete{ getTaskStatus(ar) } } } }
    }
  }

  lazy val indexPage = <html>
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

  lazy val index = HttpResponse(entity = HttpEntity(`text/html`,indexPage))

  val availableActors : String =
        "{ "+
         "\"parrotActor\":\"Simple actor who responds with yours message\""+
        "}"

  def planActorOnRemoteMachine (actorType : ActorTypeToJson) : ToResponseMarshallable = {
    Await.result(Controller ? PlanActorCreation(actorType.actorType), timeout.duration)match {
      case id : String  => Map("Status" -> "Success", "TaskId" -> id)
      case _            => Map("Status" -> "Error",   "Reason" -> "Unknown error")
    }
  }

  def planActorDeletion(ar: IdToJson): ToResponseMarshallable = {
    Await.result(Controller ? PlanActorTermination(ar.Id), timeout.duration)match {
      case id : String  => Map("Status" -> "Success", "TaskId" -> id)
      case _            => Map("Status" -> "Error",   "Reason" -> "Unknown error")
    }
  }

  def sendMessageToActorOnRemoteMachine(ar: ActorIdAndMessageToJson): ToResponseMarshallable = {
    Await.result(Controller ? ar, timeout.duration) match {
      case msg : String  => HttpResponse(entity = HttpEntity(`text/html`, msg))
      case NoSuchId      => HttpResponse(entity = HttpEntity(`text/html`, "There is no actor with such id"))
      case _             => HttpResponse(entity = HttpEntity(`text/html`, "Unknown error"))
    }
  }

  def planMachineDeletion(ar: IdToJson): ToResponseMarshallable = {
    Await.result(Controller ? PlanMachineTermination(ar.Id), timeout.duration) match {
      case id : String  => HttpResponse(entity = HttpEntity(`text/html`, "Machine termination is planned: " + id))
      case NoSuchId   => HttpResponse(entity = HttpEntity(`text/html`, "There is no vm with such id"))
      case _          => HttpResponse(entity = HttpEntity(`text/html`, "Unknown error"))
    }
  }

  def planMachineCreation: ToResponseMarshallable = {
    Await.result(Controller ? PlanMachineStart, timeout.duration)match {
      case id : Long  => HttpResponse(entity = HttpEntity(`text/html`,"Machine creation is planned: " + id))
      case _          => HttpResponse(entity = HttpEntity(`text/html`, "Unknown error"))
    }
  }

  def sendRemoteCommandToActor(rc: RemoteCommand): ToResponseMarshallable = {
    Controller ! rc
    HttpResponse(entity = HttpEntity(`text/html`, "ok"))
  }

  def getTaskStatus(ar: IdToJson): ToResponseMarshallable = {
    Await.result(TaskManager ? TaskStatus(ar.Id), timeout.duration) match{
      case TaskCompleted           => TaskResponse("Success", "")
      case TaskCompletedWithId(id) => TaskResponse("Success", id.toString)
      case TaskIncomplete          => TaskResponse("Incomplete", "")
      case TaskFailed              => TaskResponse("Error", "Task failed")
      case NoSuchId                => TaskResponse("Error", "No such Id")
      case result: ActorCreationSuccess  => result
      case result: TaskResponse    => result
      case msg: String             => TaskResponse("Error", msg)
      case _                       => TaskResponse("Error", "Unknown error")
    }
  }
}
