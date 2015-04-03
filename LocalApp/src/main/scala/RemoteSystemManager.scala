import java.util.UUID

import akka.actor.{ActorRef, Actor}
import akka.event.Logging
import akka.remote.DisassociatedEvent

import scala.collection.mutable

/**
 * Created by mentall on 12.02.15.
 */

/**
 * This class is a broker of messages from webui to remote actor in actor system in VM
 */
class RemoteSystemManager() extends Actor {
  var waiter : ActorRef = null
  var actorManager: ActorRef = null
  val logger = Logging.getLogger(context.system, self)
  var remoteSystems = new mutable.HashMap[UUID, ActorRef]
  logger.info("Remoter started")

  def remote : ActorRef = {
    val r = scala.util.Random.nextInt(remoteSystems.size)
    logger.debug("I have " + remoteSystems.size + " remote system and i choose " + r + " to send a message")
    val uUID = remoteSystems.keySet.toArray.apply(r)
    remoteSystems(uUID)
  }

  def disassociateSystem(disassociatedEvent: DisassociatedEvent): mutable.HashMap[UUID, ActorRef] = {
    remoteSystems.filter{
      (tuple) =>
        if (
          tuple._2.path.address.system.equals(disassociatedEvent.remoteAddress.system) &&
          tuple._2.path.address.port.equals(disassociatedEvent.remoteAddress.port) &&
          tuple._2.path.address.host.equals(disassociatedEvent.remoteAddress.host)
        ) {
          logger.debug("Deleting actor: {}", tuple._2)
          false
        } else true
    }
  }

  def onRemoteSystemConnection(request: RemoteConnectionRequest): Unit = {
    if (actorManager != null) {
      if (!remoteSystems.contains(request.uUID)) {
        logger.info("Connection request")
        remoteSystems += ((request.uUID, sender))
        actorManager ! request
        sender ! Connected
        sender ! TellYourIP
      }
    }
  }

  override def receive: Receive = {
    case msg: CreateNewActor   =>  {
      logger.debug("Got request on creation")
      if(remoteSystems.isEmpty) {
        logger.debug("Empty remoteSystemsList")
        sender ! NoRemoteSystems
      }
      waiter = sender
      remote ! msg
    }
    case ActorCreated(adr)                   =>  {logger.debug("Checking address"); adr ! CheckAddress}
    case NonexistentActorType                =>  {logger.debug("Nonexsistent actor type"); waiter ! NonexistentActorType}
    case AddressIsOk                         =>  {logger.debug("Address is ok"); waiter ! ActorCreated(sender)}
    case StopSystem                          =>  {logger.info("Stopping remote system"); for (r <- remoteSystems.values) r ! StopSystem}
    case req: RemoteConnectionRequest        =>  onRemoteSystemConnection(req)
    case event: DisassociatedEvent           =>  remoteSystems = disassociateSystem(event)
    case MyIPIs(ip)                          =>  {
      logger.debug(ip)
    }
    case ActorManagerStarted                 =>  actorManager = sender
    case Ping                                =>  sender ! Pong
  }
}
