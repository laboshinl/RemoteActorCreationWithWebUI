import akka.actor.{ActorRef, Actor, ActorLogging}
import akka.remote.{DisassociatedEvent, RemotingLifecycleEvent}

/**
 * Created by baka on 03.04.15.
 */
class RouterHeartBleed(actorSystemPath: String, connectedActors: List[ActorRef]) extends Actor with ActorLogging {

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    context.system.eventStream.subscribe(self, classOf[RemotingLifecycleEvent])
  }

  override def receive = {
    case msg: DisassociatedEvent =>
      log.info("Disassociatng: {}...", msg.remoteAddress)
      if (msg.remoteAddress.toString.equals(actorSystemPath))
        connectedActors.foreach{
          (actor) => actor ! Reconnect
        }
  }
}