package LocalAppActors

import java.io.Serializable
import java.util.UUID

import MessageRouterActors.RoutingInfoActor
import akka.actor.{ActorSelection, Actor, ActorRef}
import akka.event.{Logging, LoggingAdapter}
import akka.pattern.ask
import akka.remote.DisassociatedEvent
import akka.util.Timeout
import core.messages._

import scala.collection.mutable
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

/**
 * Created by baka on 11.03.15.
 */

trait RMTimeout {
  implicit val timeout: Timeout = 5 seconds
}

trait RouterManagerMessages {
  @SerialVersionUID(230L)
  case class RouterConnectionRequest(uUID: UUID, routingPairs: mutable.HashMap[UUID, UUID]) extends Serializable
  @SerialVersionUID(125L)
  case class DeleteClient(clientUUID : UUID) extends Serializable
  @SerialVersionUID(126L)
  case class RegisterPair(clientUUID : UUID, actorUUID : UUID) extends Serializable
  @SerialVersionUID(126L)
  case class UnregisterPair(clientUUID: UUID, actorUUID: UUID) extends Serializable
}

object RouterManager extends RouterManagerMessages with GeneralMessages with RMTimeout {

  def connectToRouterManager(actorSelection: ActorSelection, uUID: UUID, routingPairs: mutable.HashMap[UUID, UUID]): Unit = {
    actorSelection ! RouterConnectionRequest(uUID, routingPairs)
  }

  def pingManager(actorSelection: ActorSelection): Future[Any] = {
    actorSelection ? Ping
  }

  def deleteClient(actorRef: ActorRef, clientUUID: UUID): Unit = {
    actorRef ! DeleteClient(clientUUID)
  }

  def registerPair(actorRef: ActorRef, clientUUID : UUID, actorUUID : UUID): Future[Any] = {
    actorRef ? RegisterPair(clientUUID, actorUUID)
  }
}

class RouterManager extends Actor with RMTimeout with DisassociateSystem
  with RouterManagerMessages with GeneralMessages {
  val logger : LoggingAdapter = Logging.getLogger(context.system, this)
  var usersAmountOnRouter = new mutable.ArrayBuffer[(Long, ActorRef)]
  var routerUUIDMap       = new mutable.HashMap[UUID, ActorRef]
  var clientOfRouter      = new mutable.HashMap[UUID, ActorRef]

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = logger.debug("Path : {}", context.self.path)
  
  def getRouterCount : Int = usersAmountOnRouter.size

  /**
   * роутер подключается как ремот система в данной функции
   * заносим его в список известных remoteRouters и говорим о том что на нём
   * ещё никого нет ((0, uniqueId)) ((загрузка, роутер))
   *
   * UPD: теперь у роутера спрашивается его состояние, если он был жив, а LocalApp упал, то
   * роутер переподключится сам и скажет кто на нём есть из живых ребят. \
   * Всё что выше - частный случай для первого включения.
   */

  def connectRouter(request: RouterConnectionRequest) = {
    if (!routerUUIDMap.contains(request.uUID)) {
      logger.info("Connection request")
      routerUUIDMap += ((request.uUID, sender()))
      usersAmountOnRouter += ((request.routingPairs.size, sender()))
      usersAmountOnRouter = usersAmountOnRouter.sorted
      request.routingPairs.keys.foreach {
        uUID => clientOfRouter += ((uUID, sender()))
      }
      logger.debug("Sorted List : " + usersAmountOnRouter.toString)
    }
  }

  /**
   * рефакторнинг выделением методов
   * общаемся только с роутером
   */

  def registerPairOnRemoteRouter(router: ActorRef, pair: RegisterPair): (String, String, String) = {
    val clientStr     = Await.result(RoutingInfoActor.setNewUser(router, pair.actorUUID),
      timeout.duration).asInstanceOf[String]
    val actorStr      = Await.result(RoutingInfoActor.setNewUser(router, pair.clientUUID),
      timeout.duration).asInstanceOf[String]
    val connectString = Await.result(RoutingInfoActor.getConnectionString(router),
      timeout.duration).asInstanceOf[String]
    RoutingInfoActor.addPair(router, pair.clientUUID, pair.actorUUID)
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

  def registerPair(pair: RegisterPair) = {
    logger.debug("Registering pair : {}, {}", pair.clientUUID, pair.actorUUID)
    if (usersAmountOnRouter.size > 0) {
      //get router with minimum users
      val (usersAmount, router) = usersAmountOnRouter.remove(0)
      //get router ref
      //adding new user to usersAmountOnRouter
      usersAmountOnRouter = updateUsersAmountOnRouter(usersAmount, router)
      //register new id's on router
      val (clientStr, actorStr, connectString) = registerPairOnRemoteRouter(router, pair)
      clientOfRouter += ((pair.clientUUID, router))
      clientOfRouter += ((pair.actorUUID, router))
      //возвращаем зарегистрированные адреса тому кто попросил регистрацию
      logger.debug("Pair registered: (clientStr: {}, actorStr: {}, sendStr: {}) \n Routers load: {}",
                   clientStr, actorStr, connectString, usersAmountOnRouter)
      ActorManager.pairRegistred(sender(), clientStr, actorStr, connectString)
    } else {
      //если нет роутеров, то ничего не остаётся как послать клиента.
      logger.debug("No Routers connected")
      ActorManager.replyNoRoutersError(sender())
    }
  }

  def unregisterPair(unregisterPair: UnregisterPair): Unit = {
    deleteClientLocal(DeleteClient(unregisterPair.actorUUID))
    deleteClientLocal(DeleteClient(unregisterPair.clientUUID))
  }

  def deleteClientLocal(msg : DeleteClient) = {
    if (clientOfRouter.contains(msg.clientUUID)) {
      val router = clientOfRouter(msg.clientUUID)
      RouterManager.deleteClient(router, msg.clientUUID)
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
    routerUUIDMap         = disassociateSystem(routerUUIDMap, event)
    clientOfRouter        = disassociateSystem(clientOfRouter, event)
    usersAmountOnRouter   = disassociateUsers(usersAmountOnRouter, event)
  }

  override def receive : Receive = {
    case req: RouterConnectionRequest => connectRouter(req)
    case pair: RegisterPair           => registerPair(pair)
    case pair: UnregisterPair         => unregisterPair(pair)
    case msg : DeleteClient           => deleteClientLocal(msg)
    case event: DisassociatedEvent    => updateOnDisassociateEvent(event)
    case Ping                         => sender() ! Pong
    case msg                          => logger.debug("Unknown Message: {}", msg)
  }
}
