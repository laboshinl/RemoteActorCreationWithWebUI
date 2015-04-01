import akka.actor.{ActorLogging, ActorRef, Actor}
import akka.remote._
/**
 * Created by baka on 31.03.15.
 */

/**
 * Этот чувак слушает предсмертные вопли ремот систем и рассылает всем кто держит какие-то ссылки на ремот системы сообщения о смерти
 * чисто в теории можно от него избавиться и слушать каждым из держателей в отдельности
 */

class HeartBleed (remoteSystemHolders: List[ActorRef]) extends Actor with ActorLogging {
  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    context.system.eventStream.subscribe(self, classOf[RemotingLifecycleEvent])
  }

  override def receive = {
    case msg: DisassociatedEvent =>
      log.debug("Disassociate remote system {}", msg.remoteAddress)
      remoteSystemHolders.foreach{
        (holder) =>
          holder ! msg
      }
  }
}
