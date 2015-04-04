package my.own.remoteapp.remoteheartbleed

import akka.actor.{ActorRef, Actor, ActorLogging}
import akka.remote.{DisassociatedEvent, RemotingLifecycleEvent}
import my.own.messages.Reconnect

/**
 * Created by baka on 03.04.15.
 */

/**
 * Need to have LIB project here.
 */

class RemoteHeartBleed(actorSystemPath: String, connectedActors: List[ActorRef]) extends Actor with ActorLogging{

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
