package LocalAppActors

import java.io.Serializable

import akka.actor.{ActorRef, Actor}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

import akka.pattern.ask

/**
 * Created by mentall on 15.03.15.
 */


trait TaskManagerMessages {
  @SerialVersionUID(73L)
  case class ManageTask(task : Future[Any]) extends Serializable
  @SerialVersionUID(74L)
  case class TaskStatus(taskId : String) extends Serializable
}

object TaskManager extends TaskManagerMessages {
  def manageTask(actorRef: ActorRef, future: Future[Any]): Future[Any] = {
    actorRef ? future
  }

  def replyTaskStatus(actorRef: ActorRef, future: Future[Any]) = {
    actorRef ? TaskStatus
  }
}

class TaskManager extends Actor with TaskManagerMessages{
  var idToTasksMap = new scala.collection.mutable.HashMap[String, Future[Any]]

  def manageTask(task: ManageTask): Unit = {
    val id = java.util.UUID.randomUUID.toString
    idToTasksMap += ((id, task.task))
    sender ! id
  }

  def replyTaskStatus(taskStatus: TaskStatus): Unit = {
    if (idToTasksMap.contains(taskStatus.taskId))
      if (idToTasksMap(taskStatus.taskId).isCompleted)
        Await.result(idToTasksMap(taskStatus.taskId), 1 minute) match {
          case msg => sender ! msg
        }
      else sender ! TaskIncomplete
    else sender ! NoSuchId
  }

  override def receive: Receive = {
    case task: ManageTask         => manageTask(task)
    case taskStatus: TaskStatus   => replyTaskStatus(taskStatus)
  }
}
