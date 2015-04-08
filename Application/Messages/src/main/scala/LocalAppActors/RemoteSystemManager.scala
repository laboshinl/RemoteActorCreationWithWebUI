package LocalAppActors

import java.util.UUID

import akka.actor.{Actor, ActorRef}
import akka.event.Logging
import akka.remote.DisassociatedEvent
import core.messages._

import scala.collection.mutable

/**
 * Created by mentall on 12.02.15.
 */

/**
 * This class is a broker of messages from webui to remote actor in actor system in VM
 */
class RemoteSystemManager extends Actor with DisassociateSystem with RemoteSystemMessages with ActorManagerMessages with GeneralMessages{
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

  def onRemoteSystemConnection(request: RemoteConnectionRequest): Unit = {
    if (actorManager != null) {
      if (!remoteSystems.contains(request.uUID)) {
        logger.info("Connection request")
        remoteSystems += ((request.uUID, sender()))
        actorManager ! request
        sender() ! Connected
        sender() ! TellYourIP
      }
    }
  }

  def createNewActor(msg: CreateNewActor): Unit = {
    logger.debug("Got request on creation")
    if(remoteSystems.isEmpty) {
      logger.debug("Empty remoteSystemsList")
      sender() ! NoRemoteSystems
    }
    waiter = sender()
    remote ! msg
  }

  override def receive: Receive = {
    case msg: CreateNewActor              => createNewActor(msg)
    case ActorCreated(adr)                => logger.debug("Checking address"); adr ! CheckAddress
    case NonexistentActorType             => logger.debug("NonExsistent actor type"); waiter ! NonexistentActorType
    case AddressIsOk                      => logger.debug("Address is ok"); waiter ! ActorCreated(sender())
    case StopSystem                       => logger.info("Stopping remote system"); for (r <- remoteSystems.values) r ! StopSystem
    case req: RemoteConnectionRequest     => onRemoteSystemConnection(req)
    case event: DisassociatedEvent        => remoteSystems = disassociateSystem(remoteSystems, event)
    case MyIPIs(ip)                       => logger.debug(ip)
    case ActorManagerStarted              => actorManager = sender()
    case Ping                             => sender ! Pong
  }
}
