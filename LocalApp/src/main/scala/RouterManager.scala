import java.util.UUID

import akka.actor.{ActorRef, Actor}
import akka.event.{Logging, LoggingAdapter}
import akka.remote.DisassociatedEvent
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
  // загрузка роутера (число обслуживаемых юзеров, роутер)
  var usersAmountOnRouter = new mutable.ArrayBuffer[(Long, UUID)]
  var routerUUIDMap       = new mutable.HashMap[UUID, ActorRef]
  var clientOfRouter      = new mutable.HashMap[UUID, ActorRef]

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = logger.debug("Path : {}", context.self.path)
  
  def getRouterCount : Int = usersAmountOnRouter.size

  implicit val timeout: Timeout = 2 second

  /**
   * роутер подключается как ремот система в данной функции
   * заносим его в список известных remoteRouters и говорим о том что на нём
   * ещё никого нет ((0, uniqueId)) ((загрузка, роутер))
   */

  def connectRouter(sender: ActorRef) = {
    logger.info("Connection request")
    val uniqueId = UUID.randomUUID()
    routerUUIDMap       += ((uniqueId, sender))
    usersAmountOnRouter += ((0, uniqueId))
    usersAmountOnRouter = usersAmountOnRouter.sorted
    logger.debug("Sorted List : " + usersAmountOnRouter.toString)
    sender ! Connected
  }

  /**
   * рефакторнинг выделением методов
   * общаемся только с роутером
   */

  def registerPairOnRemoteRouter(router: ActorRef, pair: RegisterPair): (String, String, String) = {
    val clientFuture  = router ? SetMessage(pair.actorId)
    val actorFuture   = router ? SetMessage(pair.clientId)
    val connectFuture = router ? GetSendString
    //футуры выполняются параллельно, потом собираются результаты, вроди как,
    //именно то, что я хотел
    val clientStr     = Await.result(clientFuture, timeout.duration).asInstanceOf[String]
    val actorStr      = Await.result(actorFuture, timeout.duration).asInstanceOf[String]
    val connectString = Await.result(connectFuture, timeout.duration).asInstanceOf[String]
    router ! AddPair(pair.clientId, pair.actorId)
    (clientStr, actorStr, connectString)
  }

  /**
   * чистая функция (ну люблю я чистоту, что поделать)
   * апдейтит нагрузку роутеров
   */

  def updateUsersAmountOnRouter(usersAmountOnRouter: mutable.ArrayBuffer[(Long, UUID)],
                        usersAmount: Long, routerId: UUID): mutable.ArrayBuffer[(Long, UUID)] = {
    usersAmountOnRouter += ((usersAmount + 1, routerId))
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

  def updateUsersAmount(uUID: UUID): mutable.ArrayBuffer[(Long, UUID)] = {
    usersAmountOnRouter.filter{
      (tuple) =>
        if (tuple._2 == uUID)
          false
        else true
    }
  }

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

  def disassociateSystem(disassociatedEvent: DisassociatedEvent): mutable.HashMap[UUID, ActorRef] = {
    routerUUIDMap.filter{
      (tuple) =>
        if (
          tuple._2.path.address.system.equals(disassociatedEvent.remoteAddress.system) &&
            tuple._2.path.address.port.equals(disassociatedEvent.remoteAddress.port) &&
            tuple._2.path.address.host.equals(disassociatedEvent.remoteAddress.host)
        ) {
          logger.debug("Deleting actor: {}", tuple._2)
          usersAmountOnRouter = updateUsersAmount(tuple._1)
          false
        } else true
    }
  }

  def registerPair(sender : ActorRef, pair : RegisterPair) = {
    logger.debug("Registering pair : {}, {}", pair.clientId, pair.actorId)
    if (usersAmountOnRouter.size > 0) {
      //get router with minimum users
      val (usersAmount, routerId) = usersAmountOnRouter.remove(0)
      //get router ref
      val router = routerUUIDMap(routerId)
      //adding new user to usersAmountOnRouter
      usersAmountOnRouter = updateUsersAmountOnRouter(usersAmountOnRouter, usersAmount, routerId)
      //register new id's on router
      val (clientStr, actorStr, connectString) = registerPairOnRemoteRouter(router, pair)
      clientOfRouter += ((pair.clientId, router))
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

  def deleteClient(msg : DeleteClient) = {
    if (clientOfRouter.contains(msg.clientUUID)) {
      clientOfRouter(msg.clientUUID) ! msg
    }
  }

  override def receive : Receive = {
    case ConnectionRequest            => connectRouter(sender())
    case pair: RegisterPair           => registerPair(sender(), pair)
    case msg : DeleteClient           => deleteClient(msg)
    case event: DisassociatedEvent    => routerUUIDMap = disassociateSystem(event)
    // да, да, я не знаю как в функциональщине можно работать с мутабл коллекциями,
    // я буду писать чистые функции, ибо нефиг.
    case msg                          => logger.debug("Unknown Message: {}", msg)
  }

}
