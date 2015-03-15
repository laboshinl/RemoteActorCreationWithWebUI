import akka.actor.Actor
import scala.concurrent.duration._
import akka.util.Timeout

import scala.concurrent.{Await, Future}

/**
 * Created by mentall on 15.03.15.
 */
class TaskManager extends Actor with MyBeautifulOutput{
  var _uniqueTaskId  : Long = 0
  def uniqueTaskId   : Long = { _uniqueTaskId  += 1; _uniqueTaskId  }

  var idToTasksMap = new scala.collection.mutable.HashMap[Long, Future[Any]]

  override def receive: Receive = {
    case ManageTask(future) => {val id = uniqueTaskId; idToTasksMap += (( id, future)); sender ! id }
    case TaskStatus(taskId) =>
      if (idToTasksMap.contains(taskId))
        if (idToTasksMap(taskId).isCompleted) {
          Await.result(idToTasksMap(taskId), 1 minute) match{
            case compl   : TaskCompleted => sender ! TaskCompleted
            case complId : TaskCompletedWithId => sender ! TaskCompletedWithId
            case msg : String => sender ! msg
            case _ => sender ! TaskFailed
          }
        }
        else sender ! TaskIncomplete
      else sender ! NoSuchId
  }
}
