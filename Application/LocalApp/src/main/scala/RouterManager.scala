import java.util.UUID

import akka.actor.{ActorRef, Actor}
import akka.event.{Logging, LoggingAdapter}
import akka.remote.DisassociatedEvent
import akka.util.Timeout
import scala.concurrent.duration._
import scala.collection.mutable
import scala.concurrent.Await
import akka.pattern.ask

import core.messages._

/**
 * Created by baka on 11.03.15.
 */
class RouterManager extends Actor with DisassociateSystem {
  val logger : LoggingAdapter = Logging.getLogger(context.system, this)
  var usersAmountOnRouter = new mutable.ArrayBuffer[(Long, ActorRef)]
  var routerUUIDMap       = new mutable.HashMap[UUID, ActorRef]
  var clientOfRouter      = new mutable.HashMap[UUID, ActorRef]

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = logger.debug("Path : {}", context.self.path)
  
  def getRouterCount : Int = usersAmountOnRouter.size

  implicit val timeout: Timeout = 5 seconds

  /**
   * роутер подключается как ремот система в данной функции
   * заносим его в список известных remoteRouters и говорим о том что на нём
   * ещё никого нет ((0, uniqueId)) ((загрузка, роутер))
   *
   * UPD: теперь у роутера спрашивается его состояние, если он был жив, а LocalApp упал, то
   * роутер переподключится сам и скажет кто на нём есть из живых ребят. \
   * Всё что выше - частный случай для первого включения.
   */

  def connectRouter(sender: ActorRef, request: RouterConnectionRequest) = {
    if (!routerUUIDMap.contains(request.uUID)) {
      logger.info("Connection request")
      routerUUIDMap += ((request.uUID, sender))
      usersAmountOnRouter += ((request.routingPairs.size, sender))
      usersAmountOnRouter = usersAmountOnRouter.sorted
      request.routingPairs.keys.foreach {
        uUID => clientOfRouter += ((uUID, sender))
      }
      logger.debug("Sorted List : " + usersAmountOnRouter.toString)
    }
  }

  /**
   * рефакторнинг выделением методов
   * общаемся только с роутером
   */

  def registerPairOnRemoteRouter(router: ActorRef, pair: RegisterPair): (String, String, String) = {
    val clientStr     = Await.result((router ? SetMessage(pair.actorId)), timeout.duration).asInstanceOf[String]
    val actorStr      = Await.result((router ? SetMessage(pair.clientId)), timeout.duration).asInstanceOf[String]
    val connectString = Await.result((router ? GetSendString), timeout.duration).asInstanceOf[String]
    router ! AddPair(pair.clientId, pair.actorId)
    (clientStr, actorStr, connectString)
  }

  /**
   * чистая функция (ну люблю я чистоту, что поделать)
   * апдейтит нагрузку роутеров
   */

  def updateUsersAmountOnRouter(usersAmount: Long, router: ActorRef): mutable.ArrayBuffer[(Long, ActorRef)] = {
    usersAmountOnRouter += ((usersAmount + 2, router))
    usersAmountOnRouter.sorted
  }

  /**
   * Регистрация пары клиентов происходит так:
   * 1. получаем роутера с наименьшей загрузкой
   * 2. говорим, что добавим на него юзера
   * 3. регистрируем клиента на роутере, получаем адрес pub
   * 4. регистрируем актора на роутере, получаем адрес pub
   * 5. получаем для них адрес куда отправлять сообщения
   * 6. говорим роутеру связать пару клиентов
   * 7. отдаём всё полученное запросившему регистрацию
   */

  /**
   * Нужно придумать что делать с юзерами, которые висят на этом роутере.
   * Пока они просто теряют связь :(
   * Можно в наглую прибить всех акторов в ремот системах, которые были подключены к данному роутеру,
   * а всем клиентам сказать чтоб переподключились, но это хреновый вей.
   * Нужно как-то перебрасывать всех клиентов с роутера на роутер, а как пока не очень понятно
   * С учётом того, что клиент вообще висит на http это ваще почти нереально Оо
   * Ваши предложения и пожелания приветствуются!
   * В целом, есть только одна мысль - поддержитвать с клиента хертбит до актора, если не дошло - просить перезапустить
   * актора. Локалапп должен сам разобраться кто отвалился и в случае, если отвалилась ремотсистема - перезапустить актора, сообщив ему
   * все настройки соединения, или, если отвалился роутер, то перерегистрировать пару на новом роутере, если отвалилсь оба (возможно и так) то надо просто снова
   * повторить процесс логина в систему. Определить состояние можно оп внутренним признакам, т.к. всё состояние у нас хранится в элементах локалаппа
   * как-то так. Хреновасенько конечно, но лучше чем ничего.
   */

  def registerPair(sender: ActorRef, pair: RegisterPair) = {
    logger.debug("Registering pair : {}, {}", pair.clientId, pair.actorId)
    if (usersAmountOnRouter.size > 0) {
      //get router with minimum users
      val (usersAmount, router) = usersAmountOnRouter.remove(0)
      //get router ref
      //adding new user to usersAmountOnRouter
      usersAmountOnRouter = updateUsersAmountOnRouter(usersAmount, router)
      //register new id's on router
      val (clientStr, actorStr, connectString) = registerPairOnRemoteRouter(router, pair)
      clientOfRouter += ((pair.clientId, router))
      clientOfRouter += ((pair.actorId, router))
      //возвращаем зарегистрированные адреса тому кто попросил регистрацию
      logger.debug("Pair registered: (clientStr: {}, actorStr: {}, sendStr: {}) \n Routers load: {}",
                   clientStr, actorStr, connectString, usersAmountOnRouter)
      sender ! PairRegistered(clientStr, actorStr, connectString)
    } else {
      //если нет роутеров, то ничего не остаётся как послать клиента.
      logger.debug("No Routers connected")
      sender ! NoRouters
    }
  }

  def unregisterPair(unregisterPair: UnregisterPair): Unit = {
    deleteClient(DeleteClient(unregisterPair.actorId))
    deleteClient(DeleteClient(unregisterPair.clientId))
  }

  def deleteClient(msg : DeleteClient) = {
    if (clientOfRouter.contains(msg.clientUUID)) {
      val router = clientOfRouter(msg.clientUUID)
      router ! msg
      clientOfRouter -= msg.clientUUID
      for (i <- 0 to usersAmountOnRouter.size) {
        if(usersAmountOnRouter(i)._2 == router) {
          val tuple = usersAmountOnRouter(i)
          usersAmountOnRouter(i) = (tuple._1 - 1, tuple._2)
        }
      }
    }
  }

  def updateOnDisassociateEvent(event: DisassociatedEvent): Unit = {
    routerUUIDMap   = disassociateSystem(routerUUIDMap, event)
    clientOfRouter  = disassociateSystem(clientOfRouter, event)
    usersAmountOnRouter = disassociateUsers(usersAmountOnRouter, event)
  }

  override def receive : Receive = {
    case req: RouterConnectionRequest => connectRouter(sender(), req)
    case pair: RegisterPair           => registerPair(sender(), pair)
    case pair: UnregisterPair         => unregisterPair(pair)
    case msg : DeleteClient           => deleteClient(msg)
    case event: DisassociatedEvent    => updateOnDisassociateEvent(event)
    case Ping                         => sender() ! Pong
    case msg                          => logger.debug("Unknown Message: {}", msg)
  }
}
