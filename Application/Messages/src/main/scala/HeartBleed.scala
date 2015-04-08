package core.heartbleed

import akka.actor.{ActorLogging, Actor, ActorRef}
import akka.remote.{DisassociatedEvent, RemotingLifecycleEvent}
import core.messages.HeartBleedMessages

/**
 * Created by baka on 05.04.15.
 */

trait HeartBleedMessages {
  object Reconnect
}

class HeartBleed(actorSystemPath: String, connectedActors: List[ActorRef]) extends Actor with ActorLogging with HeartBleedMessages{

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
