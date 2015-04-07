import akka.actor.Actor
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

import core.messages._

/**
 * Created by mentall on 15.03.15.
 */
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
