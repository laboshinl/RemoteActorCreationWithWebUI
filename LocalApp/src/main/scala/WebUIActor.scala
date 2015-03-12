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

class WebUIActor(val RemoterActor : ActorRef, val OpenstackActor: ActorRef, val RouterProvider : ActorRef)
  extends HttpService with Json4sSupport with Actor with MyBeautifulOutput
{
  implicit def executionContext : ExecutionContextExecutor = actorRefFactory.dispatcher
  def actorRefFactory = context
  override def receive = runRoute(route)

  implicit val timeout: Timeout = 1 minute // for the actor 'asks'
  val json4sFormats = DefaultFormats

  var uniqueId : Long = 0
  var uniqueTask : Long = 0
  var actors = new scala.collection.mutable.HashMap[Long, ActorRef]
  var tasks = new scala.collection.mutable.HashMap[Long, Future[Any]]

  lazy val route = {
    path(""){ get{ complete{ index }  }  }~
    path("actor"){
      get{ respondWithMediaType(`application/json`){ complete{  availableActors } } }~
      put{
        entity(as[ActorTypeToJson]) {
          actorType => complete{ createActorOnRemoteMachine(actorType) }
        }
      }~
      post{
        entity(as[ActorIdAndMessageToJson]) {
          ar => complete{ sendMessageToActorOnRemoteMachine(ar)  }
        }
      }~
      delete{
        entity(as[ActorIdToJson]) {
          ar => complete{ deleteActorOnRemoteMachine(ar) }
        }
      }
    }~
    path("system"){
      get{ complete{ index } }~
      put{ complete { planMachineCreation } }~
      post{ entity(as[TaskIdToJson]) { ar => complete{ getTaskStatus(ar) } } }~
      delete{ entity(as[TaskIdToJson]) { ar => complete { planMachineDeletion(ar) } } }
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
         "\"parrotActor\":\"Simple actor who respond with yours message\""+
        "}"

  //TODO: create connections on routers
  def createActorOnRemoteMachine (actorType : ActorTypeToJson) : ToResponseMarshallable = {
    out("Here")
    uniqueId += 1
    val actorId = uniqueId.toString + "-actor"
    val clientId = uniqueId.toString + "-client"
    Await.result((RouterProvider ? RegisterPair(clientId, actorId)), timeout.duration) match {
      case res : PairRegistered =>
        out("Here2")
        Await.result(RemoterActor ? CreateNewActor(actorType.actorType, actorId, res.actorStr), timeout.duration) match {
          case createRes : ActorCreated =>
            actors += ((uniqueId, createRes.asInstanceOf[ActorCreated].adr))
            HttpResponse(entity = HttpEntity(`text/html`, clientId.toString + " " + res.clientStr))
          case NoRouters => HttpResponse(entity = HttpEntity(`text/html`, "No Routers"))
        }
      case _ => HttpResponse(entity = HttpEntity(`text/html`, "Wrong Type"))
    }
  }

  //TODO: Needs refactoring or deletion
  def sendMessageToActorOnRemoteMachine(ar: ActorIdAndMessageToJson): ToResponseMarshallable = {
    val target = actors(ar.id.toLong)
    if (target == null) {
      println("Got message, but actor is dead")
      HttpResponse(entity = HttpEntity(`text/html`, "Got message, but actor is dead\n"))
    }
    else {
      println("Got message \"" + ar.msg + "\" for actor " + target)
      val res = Await.result(target ? ar.msg, timeout.duration)
      println("Actor's response:" + res.toString)
      HttpResponse(entity = HttpEntity(`text/html`, res.toString))
    }
  }

  def deleteActorOnRemoteMachine(ar: ActorIdToJson): ToResponseMarshallable = {
    if (actors.contains(ar.id.toLong)) {
      actors(ar.id.toLong) ! PoisonPill
      actors -= ar.id.toLong
      HttpResponse(entity = HttpEntity(`text/html`, "PoisonPill sended to actor"))
    }
    else {
      HttpResponse(entity = HttpEntity(`text/html`, "There is no actor with such id"))
    }
  }

  def planMachineDeletion(ar: TaskIdToJson): ToResponseMarshallable = {
    uniqueTask += 1
    tasks += (( uniqueTask, OpenstackActor ? TerminateMachine(ar.id.toLong) ))
    HttpResponse(entity = "Machine termination is planned: " + uniqueTask)
  }

  def planMachineCreation: ToResponseMarshallable = {
    uniqueTask += 1
    tasks += (( uniqueTask, OpenstackActor ? StartMachine ))
    HttpResponse(entity = "Machine creation is planned: " + uniqueTask)
  }

  // TODO: MAY BE A PROBLEM WITH CREATION HERE
  def getTaskStatus(ar: TaskIdToJson): ToResponseMarshallable = {
    if (tasks.contains(ar.id.toLong)) {
      if (tasks(ar.id.toLong).isCompleted) {
          Await.result(tasks(ar.id.toLong), 1 minute) match{
          case compl : MachineTaskCompleted => HttpResponse(entity = HttpEntity(`text/html`, "Task completed:"
            + compl.asInstanceOf[MachineTaskCompleted].id.toString))
          case _ => HttpResponse(entity = HttpEntity(`text/html`, "MachineTaskCompleted expected, got something else"))
        }
      }
      else {
        HttpResponse(entity = HttpEntity(`text/html`, "Task incomplete"))
      }
    }
    else {
      HttpResponse(entity = HttpEntity(`text/html`, "There is no task with such id"))
    }
  }
}
