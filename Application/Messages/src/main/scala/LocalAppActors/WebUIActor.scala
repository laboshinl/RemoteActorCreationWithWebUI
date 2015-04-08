package LocalAppActors

/**
 * Created by mentall on 08.02.15.
 */

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import core.messages._
import org.json4s.DefaultFormats

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.language.postfixOps

/**
 * это говно не поддаётся рефакторингу и моему осознанию. Я не знаю что с этим делать.
 * @param controller
 * @param taskManager
 */

class WebUIActor(val controller : ActorRef, val taskManager : ActorRef)
  extends HttpService with Json4sSupport with Actor with TaskManagerMessages with ActorManagerMessages
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
    println(rc.clientUID)
    println(rc.command)
    controller ! rc
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
