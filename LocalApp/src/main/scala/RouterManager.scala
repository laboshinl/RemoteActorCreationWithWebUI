import java.util.UUID

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
class RouterManager extends Actor {
  val logger : LoggingAdapter = Logging.getLogger(context.system, this)
  // таблица с роутерами
  var remoteRoutersMap = new mutable.HashMap[UUID, ActorRef]
  // загрузка роутера (число обслуживаемых юзеров, роутер)
  var routersLoad = new mutable.ArrayBuffer[(Long, UUID)]
  var routersClients = new mutable.HashMap[UUID, ActorRef]

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = logger.info("Path : {}", context.self.path.toString)
  def getRouterCount : Int = routersLoad.size

  implicit val timeout: Timeout = 2 second

  /**
   * роутер подключается как ремот система в данной функции
   * заносим его в список известных remoteRouters и говорит о том что на нём
   * ещё никого нет ((0, uniqueId)) ((загрузка, роутер))
   * @param sender
   */

  def connectRouter(sender: ActorRef) = {
    logger.info("Connection request")
    val uniqueId = UUID.randomUUID()
    remoteRoutersMap += ((uniqueId, sender))
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

  def registerPair(sender : ActorRef, pair : RegisterPair) = {
    logger.debug("Registering pair : " + pair.clientId + " " + pair.actorId)
    if (routersLoad.size > 0) {
      logger.debug("Routers Load before register: " + routersLoad.toString)
      //get router with minimum users
      val first = routersLoad.remove(0)
      val routerId = first._2
      val usersAmount = first._1
      //get router ref
      val router = remoteRoutersMap(routerId)
      //adding user for router
      routersLoad += ((usersAmount + 1, routerId))
      //sorting list
      routersLoad = routersLoad.sorted
      logger.debug("Routers Load after register: " + routersLoad.toString)
      logger.debug("Remote Router: " + router.toString)
      //register new id's on router
      val respForClient = Await.result((router ? SetMessage(pair.actorId)), timeout.duration)
      val respForActor = Await.result((router ? SetMessage(pair.clientId)), timeout.duration)
      val respForSendString = Await.result((router ? GetSendString), timeout.duration)
      val clientStr = respForClient.asInstanceOf[String]
      val actorStr = respForActor.asInstanceOf[String]
      val connectString = respForSendString.asInstanceOf[String]
      router ! AddPair(pair.clientId, pair.actorId)
      logger.debug("Router response : (client: " + clientStr + " " + actorStr + ")")
      routersClients += ((pair.clientId, router))
      //возвращаем зарегистрированные адреса тому кто попросил регистрацию
      sender ! PairRegistered(clientStr, actorStr, connectString)
    } else {
      //если нет роутеров, то ничего не остаётся как послать клиента.
      logger.debug("No Routers connected")
      sender ! NoRouters
    }
  }

  def deleteClient(msg : DeleteClient) = {
    if (routersClients.contains(msg.clientUUID)) {
      routersClients(msg.clientUUID) ! msg
    }
  }

  override def receive : Receive = {
    case ConnectionRequest    => connectRouter(sender)
    case pair: RegisterPair   => registerPair(sender, pair)
    case msg : DeleteClient   => deleteClient(msg)
    //TODO: remove pair request!
    case msg => logger.debug("Unknown Message: " + msg.toString)
  }

}
