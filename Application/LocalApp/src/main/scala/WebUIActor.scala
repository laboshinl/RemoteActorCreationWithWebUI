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

import core.messages.WebUi._
import core.messages._
/**
 * это говно не поддаётся рефакторингу и моему осознанию. Я не знаю что с этим делать.
 */

class WebUIActor(val controller : ActorRef, val taskManager : ActorRef)
  extends HttpService with Json4sSupport with Actor
{
  //Statements required by traits
  implicit def executionContext : ExecutionContextExecutor = actorRefFactory.dispatcher
  def actorRefFactory = context
  implicit val timeout: Timeout = 2 second
  val json4sFormats = DefaultFormats
  override def receive = runRoute(route)
  val uuidRegexp = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"

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
    Await.result(controller ? Controller.PlanActorCreation(actorType.actorType), timeout.duration)match {
      case TaskCreated(id)  => Map("Status" -> "Success", "TaskId" -> id)
      case _                => Map("Status" -> "Error",   "Reason" -> "Unknown error")
    }
  }

  def planActorDeletion(ar: IdToJson): ToResponseMarshallable = {
    Await.result(controller ? Controller.PlanActorTermination(ar.Id), timeout.duration)match {
      case TaskCreated(id)  => Map("Status" -> "Success", "TaskId" -> id)
      case _                => Map("Status" -> "Error",   "Reason" -> "Unknown error")
    }
  }

  def sendMessageToActorOnRemoteMachine(ar: ActorIdAndMessageToJson): ToResponseMarshallable = {
    Await.result(controller ? Controller.ActorIdAndMessageToJson(ar.id, ar.msg), timeout.duration) match {
      case msg: String          => HttpResponse(entity = HttpEntity(`text/html`, msg))
      case General.FAIL(reason) => HttpResponse(entity = HttpEntity(`text/html`, reason))
      case _                    => HttpResponse(entity = HttpEntity(`text/html`, "Unknown error"))
    }
  }

  def planMachineDeletion(ar: IdToJson): ToResponseMarshallable = {
    Await.result(controller ? Controller.PlanMachineTermination(ar.Id), timeout.duration) match {
      case TaskCreated(id)      => HttpResponse(entity = HttpEntity(`text/html`, "Machine termination is planned: " + id))
      case General.FAIL(reason) => HttpResponse(entity = HttpEntity(`text/html`, reason))
      case _                    => HttpResponse(entity = HttpEntity(`text/html`, "Unknown error"))
    }
  }

  def planMachineCreation: ToResponseMarshallable = {
    Await.result(controller ? Controller.PlanMachineStart, timeout.duration) match {
      case TaskCreated(id)      => HttpResponse(entity = HttpEntity(`text/html`, "Machine creation is planned: " + id))
      case _                    => HttpResponse(entity = HttpEntity(`text/html`, "Unknown error"))
    }
  }

  //TODO: на лоб "переделать ремот команд"
  def sendRemoteCommandToActor(rc: RemoteCommand): ToResponseMarshallable = {
    if (rc.clientUID.matches(uuidRegexp)){
      println(rc.clientUID)
      println(rc.command)
      controller ! Controller.RemoteCommand(rc.clientUID, rc.command, rc.args)
      HttpResponse(entity = HttpEntity(`text/html`, "Casted"))
    }
    else HttpResponse(entity = HttpEntity(`text/html`, "Incorrect uuid"))
  }

  def getTaskStatus(ar: IdToJson): ToResponseMarshallable = Await.result(taskManager ? TaskManager.TaskStatus(ar.Id), timeout.duration) match{
    case MachineTerminated(vmId)        => Map("Status" -> "Success", "vmId" -> vmId)
    case ActorDeleted(uid)              => Map("Status" -> "Success", "UUID" -> uid)
    case TaskIncomplete                 => Map("Status" -> "Incomplete")
    case result: ActorCreationSuccess   => Map(
      "Status" -> "Success",
      "clientUID" -> result.clientId,
      "subString" -> result.clientSubStr,
      "sendString" -> result.sendString
    )
    case msg: String                    => Map("Status" -> "Error", "reason" -> msg)
    case General.FAIL(reason)           => Map("Status" -> "Error", "reason" -> reason)
    case _                              => Map("Status" -> "Error", "reason" -> "Unknown error")
  }
}
