import java.util.UUID

import akka.actor.ActorRef
import akka.remote.DisassociatedEvent
import scala.collection.mutable

/**
 * Created by baka on 06.04.15.
 */

/**
 * клёвая примесь, которая нужна для дизассоциации отвалишихся актор систем для всех, кто держит на них ссылки
 */

trait DisassociateSystem {
  def disassociateSystem(hash: mutable.HashMap[UUID, ActorRef], disassociatedEvent: DisassociatedEvent): mutable.HashMap[UUID, ActorRef] = {
    hash.filter{
      (tuple) =>
        ! (
            tuple._2.path.address.system.equals(disassociatedEvent.remoteAddress.system) &&
            tuple._2.path.address.port.equals(disassociatedEvent.remoteAddress.port) &&
            tuple._2.path.address.host.equals(disassociatedEvent.remoteAddress.host)
          )
    }
  }
  def disassociateUsers(amountArray: mutable.ArrayBuffer[(Long, ActorRef)],
                       disassociatedEvent: DisassociatedEvent): mutable.ArrayBuffer[(Long, ActorRef)] = {
    amountArray.filter{
      (tuple) =>
        ! (
          tuple._2.path.address.system.equals(disassociatedEvent.remoteAddress.system) &&
            tuple._2.path.address.port.equals(disassociatedEvent.remoteAddress.port) &&
            tuple._2.path.address.host.equals(disassociatedEvent.remoteAddress.host)
          )
    }
  }
}
