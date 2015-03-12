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
  // таблица с роутерами
  var remoteRouters = new mutable.HashMap[Long, ActorRef]
  // загрузка роутера (число обслуживаемых юзеров, роутер)
  var routersLoad = new mutable.ArrayBuffer[(Long, Long)]
  var uniqueId = 0

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = logger.debug("Path : " + context.self.path.toString)
  def getRouterCount : Int = routersLoad.size

  implicit val timeout: Timeout = 1 minute

  /**
   * роутер подключается как ремот система в данной функции
   * заносим его в список известных remoteRouters и говорит о том что на нём
   * ещё никого нет ((0, uniqueId)) ((загрузка, роутер))
   * @param sender
   */

  def onRouterConnectionRequest(sender: ActorRef) = {
    logger.debug("Connection request")
    uniqueId += 1
    remoteRouters += ((uniqueId, sender))
    routersLoad += ((0, uniqueId))
    routersLoad = routersLoad.sorted
    logger.debug("Sorted List : " + routersLoad.toString)
    sender ! Connected
  }

  /**
   * Http сервер при запросе регистрации от Http клиента берёт id клиента, генерит id автора
   * запрашивает у роутера данный метод, стартует актора с коннетк стрингом и созданнымм
   * id. Как-то так это будет происходить.
   * @param sender
   */

  def onRegisterPairRequest(sender : ActorRef, pair : RegisterPair) = {
    logger.debug("Registering pair : " + pair.clientID + " " + pair.actorID)
    if (routersLoad.size > 0) {
      logger.debug("Routers Load before register: " + routersLoad.toString)
      //get router with minimum users
      val first = routersLoad.remove(0)
      val routerId = first._2
      val userCount = first._1
      //get router ref
      val router = remoteRouters(routerId)
      //adding user for router
      routersLoad += ((userCount + 1, routerId))
      //sorting list
      routersLoad = routersLoad.sorted
      logger.debug("Routers Load after register: " + routersLoad.toString)
      logger.debug("Remote Router: " + router.toString)
      //register new id's on router
      val respForClient = Await.result((router ? SetMessage(pair.actorID)), 1 minute)
      val respForActor = Await.result((router ? SetMessage(pair.clientID)), 1 minute)
      val clientStr = respForClient.asInstanceOf[String]
      val actorStr = respForActor.asInstanceOf[String]
      logger.debug("Router response : (client: " + clientStr + " " + actorStr + ")")
      //возвращаем зарегистрированные адреса тому кто попросил регистрацию
      sender ! PairRegistered(clientStr, actorStr)
    } else {
      //если нет роутеров, то ничего не остаётся как послать клиента.
      logger.debug("No Routers connected")
      sender ! NoRouters
    }
  }

  override def receive : Receive = {
    case ConnectionRequest                =>  {
      onRouterConnectionRequest(sender)
    }
    /**
     * need to test this method
      */
    case pair: RegisterPair  => {
      onRegisterPairRequest(sender, pair)
    }
    case msg => logger.debug("Unknown Message: " + msg.toString)
  }

}
