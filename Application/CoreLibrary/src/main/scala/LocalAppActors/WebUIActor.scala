package LocalAppActors

/**
 * Created by mentall on 08.02.15.
 */

import java.io.Serializable


import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import org.json4s.DefaultFormats
import spray.http.{HttpEntity, HttpResponse}
import spray.httpx.Json4sSupport
import spray.httpx.marshalling.ToResponseMarshallable
import spray.routing.HttpService
import spray.http.MediaTypes._
import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.language.postfixOps

/**
 * это говно не поддаётся рефакторингу и моему осознанию. Я не знаю что с этим делать.
 */

trait WebUiMessages {
  @SerialVersionUID(122L)
  case class ActorIdAndMessageToJson(id: String, msg: String) extends Serializable
  case object NoSuchId extends Serializable
  case object TaskIncomplete extends Serializable
  case object TaskCompleted extends Serializable
  case class TaskCompletedWithId(id: String) extends Serializable
  case object TaskFailed extends Serializable
  case class ActorCreationSuccess(Status: String, clientId: String, clientSubStr: String, sendString: String) extends Serializable
  case class TaskResponse(Status: String, msg: String) extends Serializable
  case class ActorTypeToJson(actorType: String) extends Serializable
  case class IdToJson(Id: String) extends Serializable
  @SerialVersionUID(1228L)
  case class RemoteCommand(clientUID: String, command: String, args: immutable.List[String]) extends Serializable
}

object WebUIActor extends WebUiMessages {
  implicit val timeout: Timeout = 2 second
  def tellNoSuchId(actorRef: ActorRef): Unit = {
    actorRef ! NoSuchId
  }

  def tellTaskIncomplete(actorRef: ActorRef): Unit = {
    actorRef ! TaskIncomplete
  }

  def tellTaskCompleted(actorRef: ActorRef): Unit = {
    actorRef ! TaskCompleted
  }

  def tellTaskCompletedWithId(actorRef: ActorRef, id: String): Unit = {
    actorRef ! TaskCompletedWithId(id)
  }

  def tellTaskFailed(actorRef: ActorRef): Unit = {
    actorRef ! TaskFailed
  }

  def tellTaskResponce(actorRef: ActorRef, status: String, msg: String): Unit = {
    actorRef ! TaskResponse(status, msg)
  }

  def tellActorCreated(actorRef: ActorRef, status: String,
                       clientId: String, clientSubStr: String, sendString: String): Unit = {
    actorRef ! ActorCreationSuccess(status, clientId, clientSubStr, sendString)
  }
}

class WebUIActor(val controller : ActorRef, val taskManager : ActorRef)
  extends HttpService with Json4sSupport with Actor with TaskManagerMessages with ActorManagerMessages
  with WebUiMessages
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
    }~
    path("deploy-system"){
      get{ complete{ runVMsWithRemoteAppAndMessageRouter } }
    }
  }

  def runVMsWithRemoteAppAndMessageRouter : ToResponseMarshallable = {
    Await.result(controller ? ("runVMsWithRemoteAppAndMessageRouter"), timeout.duration)match {
      case id : String  => Map("Status" -> "Success", "TaskId" -> id)
      case _            => Map("Status" -> "Error",   "Reason" -> "Unknown error")
    }
  }

  def planActorOnRemoteMachine (actorType : ActorTypeToJson) : ToResponseMarshallable = {
    Await.result(Controller.planActorCreation(controller, actorType.actorType),
      timeout.duration)match {
      case id : String  => Map("Status" -> "Success", "TaskId" -> id)
      case _            => Map("Status" -> "Error",   "Reason" -> "Unknown error")
    }
  }

  def planActorDeletion(ar: IdToJson): ToResponseMarshallable = {
    Await.result(Controller.planActorTermination(controller, ar.Id),
      timeout.duration)match {
      case id : String  => Map("Status" -> "Success", "TaskId" -> id)
      case _            => Map("Status" -> "Error",   "Reason" -> "Unknown error")
    }
  }

  def sendMessageToActorOnRemoteMachine(ar: ActorIdAndMessageToJson): ToResponseMarshallable = {
    Await.result(Controller.sendMessageToActor(controller, ar.id, ar.msg), timeout.duration) match {
      case msg : String  => HttpResponse(entity = HttpEntity(`text/html`, msg))
      case NoSuchId      => HttpResponse(entity = HttpEntity(`text/html`, "There is no actor with such id"))
      case _             => HttpResponse(entity = HttpEntity(`text/html`, "Unknown error"))
    }
  }

  def planMachineDeletion(ar: IdToJson): ToResponseMarshallable = {
    Await.result(Controller.planMachineTermination(controller, ar.Id),
      timeout.duration) match {
      case id : String  => HttpResponse(entity = HttpEntity(`text/html`, "Machine termination is planned: " + id))
      case NoSuchId     => HttpResponse(entity = HttpEntity(`text/html`, "There is no vm with such id"))
      case _            => HttpResponse(entity = HttpEntity(`text/html`, "Unknown error"))
    }
  }

  def planMachineCreation: ToResponseMarshallable = {
    Await.result(Controller.planMachineStart(controller), timeout.duration)match {
      case id : Long  => HttpResponse(entity = HttpEntity(`text/html`,"Machine creation is planned: " + id))
      case _          => HttpResponse(entity = HttpEntity(`text/html`, "Unknown error"))
    }
  }


  def sendRemoteCommandToActor(rc: RemoteCommand): ToResponseMarshallable = {
    println(rc.clientUID)
    println(rc.command)
    Controller.sendRemoteCommand(controller, rc.clientUID, rc.command, rc.args)
    HttpResponse(entity = HttpEntity(`text/html`, "ok"))
  }

  /*
  TODO: Считаю, что вид надо унифицировать
  Это повышает читаемость кода, из возвращаемого мапа будет видно, что просходит.
  Зачем ActorCreationSuccess? Не должен ли success проверяться как результат Task?
   */
  def getTaskStatus(ar: IdToJson): ToResponseMarshallable = {
    Await.result(taskManager ? TaskStatus(ar.Id), timeout.duration) match{
      case TaskCompleted           => Map("Status" -> "Success")
      case TaskCompletedWithId(id) => Map("Status" -> "Success","TaskId" -> id.toString)
      case TaskIncomplete          => Map("Status" -> "Incomplete")
      case TaskFailed              => Map("Status" -> "Error", "Reason" -> "Task failed")
      case NoSuchId                => Map("Status" -> "Error", "Reason" -> "No such Id")
      case result: ActorCreationSuccess  => result
      case result: TaskResponse    => result
      case msg: String             => TaskResponse("Error", msg)
      case _                       => TaskResponse("Error", "Unknown error")
    }
  }
}
