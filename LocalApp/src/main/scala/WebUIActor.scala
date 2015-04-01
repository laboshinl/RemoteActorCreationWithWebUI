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

class WebUIActor(val controller : ActorRef, val taskManager : ActorRef)
  extends HttpService with Json4sSupport with Actor
{
  //Statements required by traits
  implicit def executionContext : ExecutionContextExecutor = actorRefFactory.dispatcher
  def actorRefFactory = context
  implicit val timeout: Timeout = 2 second
  val json4sFormats = DefaultFormats
  override def receive = runRoute(route)

  //This is the tree of all working routes that answer to user's requests
  lazy val route = {
    //TODO: create DISCONNECT request from client
    path(""){ get{ complete{ HttpResponse(entity = HttpEntity(`text/html`,io.Source.fromFile("index.html").mkString)) }  }  }~
    path("actor"){
      get{ respondWithMediaType(`application/json`){ complete{ Map("parrotActor" -> "Simple actor who responds with yours message") } } }~
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
      get{ complete{ HttpResponse(entity = HttpEntity(`text/html`,io.Source.fromFile("index.html").mkString)) } }~
      put{ complete { planMachineCreation } }~
      delete{ entity(as[IdToJson]) { ar => complete { planMachineDeletion(ar) } } }
    }~
    path("task"){
      get{ entity(as[IdToJson]) { ar => respondWithMediaType(`application/json`){ complete{ getTaskStatus(ar) } } } }
    }
  }

  def planActorOnRemoteMachine (actorType : ActorTypeToJson) : ToResponseMarshallable = {
    Await.result(controller ? PlanActorCreation(actorType.actorType), timeout.duration)match {
      case id : String  => Map("Status" -> "Success", "TaskId" -> id)
      case _            => Map("Status" -> "Error",   "Reason" -> "Unknown error")
    }
  }

  def planActorDeletion(ar: IdToJson): ToResponseMarshallable = {
    Await.result(controller ? PlanActorTermination(ar.Id), timeout.duration)match {
      case id : String  => Map("Status" -> "Success", "TaskId" -> id)
      case _            => Map("Status" -> "Error",   "Reason" -> "Unknown error")
    }
  }

  def sendMessageToActorOnRemoteMachine(ar: ActorIdAndMessageToJson): ToResponseMarshallable = {
    Await.result(controller ? ar, timeout.duration) match {
      case msg : String  => HttpResponse(entity = HttpEntity(`text/html`, msg))
      case NoSuchId      => HttpResponse(entity = HttpEntity(`text/html`, "There is no actor with such id"))
      case _             => HttpResponse(entity = HttpEntity(`text/html`, "Unknown error"))
    }
  }

  def planMachineDeletion(ar: IdToJson): ToResponseMarshallable = {
    Await.result(controller ? PlanMachineTermination(ar.Id), timeout.duration) match {
      case id : String  => HttpResponse(entity = HttpEntity(`text/html`, "Machine termination is planned: " + id))
      case NoSuchId     => HttpResponse(entity = HttpEntity(`text/html`, "There is no vm with such id"))
      case _            => HttpResponse(entity = HttpEntity(`text/html`, "Unknown error"))
    }
  }

  def planMachineCreation: ToResponseMarshallable = {
    Await.result(controller ? PlanMachineStart, timeout.duration)match {
      case id : Long  => HttpResponse(entity = HttpEntity(`text/html`,"Machine creation is planned: " + id))
      case _          => HttpResponse(entity = HttpEntity(`text/html`, "Unknown error"))
    }
  }


  def sendRemoteCommandToActor(rc: RemoteCommand): ToResponseMarshallable = {
    println()
    println()
    println(rc.clientUID)
    println(rc.command)
    println()
    println()
//    controller ! rc
    HttpResponse(entity = HttpEntity(`text/html`, "ok"))
  }

  def getTaskStatus(ar: IdToJson): ToResponseMarshallable = {
    Await.result(taskManager ? TaskStatus(ar.Id), timeout.duration) match{
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
