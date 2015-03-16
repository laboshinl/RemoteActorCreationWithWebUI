import akka.actor.{ActorRef, Actor}
import akka.event.Logging

/**
 * Created by mentall on 12.02.15.
 */

/**
 * This class is a broker of messages from webui to remote actor in actor system in VM
 */
class RemoteConnection extends Actor {
  var waiter : ActorRef = null
  val logger = Logging.getLogger(context.system, self)
  var remoteSystems = new scala.collection.mutable.HashMap[Long, ActorRef]
  var uniqueId : Long = 0

  logger.info("Remoter started")

  def remote : ActorRef = {
    // Please, make u code breath. Please :-[
    val r = scala.util.Random.nextInt(remoteSystems.size) + 1
    logger.debug("I have " + remoteSystems.size + " remote system and i choose " + r + " to send a message")
    remoteSystems(r)
  }

  override def receive: Receive = {
    //TODO:remove actor with id...
    case CreateNewActor(t, id, subString, sendString)   =>  {logger.debug("Got request on creation"); waiter = sender; remote ! CreateNewActor(t, id, subString, sendString)}
    case ActorCreated(adr)                              =>  {logger.debug("Checking address"); adr ! CheckAddress}
    case NonexistentActorType                           =>  {logger.debug("Nonexsistent actor type"); waiter ! NonexistentActorType}
    case AddressIsOk                                    =>  {logger.debug("Address is ok"); waiter ! ActorCreated(sender)}
    case StopSystem                                     =>  {logger.info("Stopping remote system"); for (r <- remoteSystems.values) r ! StopSystem}
    case ConnectionRequest                              =>  {
      logger.info("Connection request")
      uniqueId += 1
      remoteSystems += ((uniqueId, sender))
      sender!Connected; sender ! TellYourIP
    }
    case MyIPIs(ip)                                     =>  {logger.debug(ip)}
  }
}
