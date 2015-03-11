import akka.actor.{ActorRef, Actor}
import akka.event.{Logging, LoggingAdapter}
import akka.util.Timeout
import scala.concurrent.duration._
import scala.collection.mutable
import scala.concurrent.Await
import akka.pattern.ask

/**
 * Created by baka on 11.03.15.
 */
class RouterActorsProvider extends Actor {
  val logger : LoggingAdapter = Logging.getLogger(context.system, this)
  var remoteRouters = new mutable.HashMap[Long, ActorRef]
  var routersLoad = new mutable.MutableList[(Long, Long)]
  var uniqueId = 0
  implicit val timeout: Timeout = 1 minute // for the actor 'asks' 
  override def receive : Receive = {
    case ConnectionRequest                =>  {
      logger.debug("Connection request")
      uniqueId += 1
      remoteRouters += ((uniqueId, sender))
      routersLoad += ((0, uniqueId))
      routersLoad = routersLoad.sorted
      logger.debug("Sorted List : " + routersLoad.toString)
      sender ! Connected
    }
    case pair: RegisterPair  => {
      logger.debug("Registering pair : " + pair.clientID + " " + pair.actorID)
      if (routersLoad.size > 0) {
        val routerId = routersLoad.head._2
        val router = remoteRouters(routerId)
        val respForClient = Await.result((router ? SetMessage(pair.actorID)), 1 minute)
        val respForActor = Await.result((router ? SetMessage(pair.clientID)), 1 minute)
        val clientStr = respForClient.asInstanceOf[String]
        val actorStr = respForActor.asInstanceOf[String]
        logger.debug("Router response : (client: " + clientStr + " " + actorStr + ")")
        sender ! PairRegistered(clientStr, actorStr)
      }
    }

  }

}
