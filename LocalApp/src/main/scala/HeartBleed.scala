import akka.actor.{ActorRef, Actor}
import scala.concurrent.duration._
import scala.collection.mutable
/**
 * Created by baka on 31.03.15.
 */

/**
 * Отдельный чувак, который слушает хертбиты от всех ремот систем
 * З.Ы. наверняка есть умный способ использования DeadLetters для этого, но так как-то
 * свой велосипед :)
 */
case object Tick

class HeartBleed (remoteSystemHolders: List[ActorRef], timerVal: Int) extends Actor {
  var remoteSystems = new mutable.HashMap[ActorRef, Int]
  var remoteRouters = new mutable.HashMap[ActorRef, Int]


  override def preStart() {
    context.system.scheduler.schedule(timerVal second, timerVal second, self, Tick)
  }

  override def receive: Receive = {
    case Tick => refreshRemoteSystems
    case msg: HeartBeat => updateHeart(msg)
  }

  def processMap(map: mutable.HashMap[ActorRef, Int], receivers: List[ActorRef]) : mutable.HashMap[ActorRef, Int] = {
    map.foreach{
      (tuple) =>
        // если чувак не проявился за какое-то время, выпиливаем его нахер
        if (tuple._2 == 0) {
          removeActor(tuple._1, receivers)
        }
    }
    map.map {
      (tuple) =>
        ((tuple._1, 0)) // хер знает заработает ли, мб надо ((((())))) - больше скобок!
    }
  }

  def refreshRemoteSystems : Unit = {
    remoteSystems = processMap(remoteSystems, remoteSystemHolders)
  }

  def removeActor(remote: ActorRef, receivers: List[ActorRef]) : Unit = {
    receivers.foreach{
      (receiver) =>
        receiver ! RemoveActor(remote)
    }
  }

  /**
   * Эрлангер во мне негодует! нечитстая функция!
   * @param bleed
   */

  def updateHeart(bleed: HeartBeat) : Unit = {
    if (!remoteSystems.contains(bleed.remote)) {
      remoteSystems += ((bleed.remote, 1)) // more parentheses!!!
    } else {
      remoteSystems(bleed.remote) += 1
    }
  }
}

/**
 * TODO: Заделать ИДЕМПОТЕНТНУЮ реакцию на сообщения RemoveActor во всех чуваках, которые заботятся о ремот системах
 * т.е. если актора нет, то и пофиг, не делаем ничего, если есть - удоляем. На ремот системах надо сделать такого же чувака, который законнектится
 * к этому и будеи слать ему хертбиты. Возможно биты надо слать в обе стороны, но это пока только моё предположение
 * Данный код никак не тестился. Мб попозже потестю, мб нет. Если ты его допилишь хотяб слегка, будет клёва.
 * Держи пятулю!
 */