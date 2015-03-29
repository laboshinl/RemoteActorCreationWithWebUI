import akka.actor.Actor
import scala.concurrent.duration._
import akka.util.Timeout

import scala.concurrent.{Await, Future}

/**
 * Created by mentall on 15.03.15.
 */
class TaskManager extends Actor {
  var idToTasksMap = new scala.collection.mutable.HashMap[String, Future[Any]]

  override def receive: Receive = {
    case ManageTask(future) => {val id = java.util.UUID.randomUUID.toString; idToTasksMap += ((id, future)); sender ! id }
    case TaskStatus(taskId) =>
      if (idToTasksMap.contains(taskId))
        if (idToTasksMap(taskId).isCompleted) {
          Await.result(idToTasksMap(taskId), 1 minute) match{
            case msg => sender ! msg
          }
        }
        else sender ! TaskIncomplete
      else sender ! NoSuchId
  }
}
