import java.util.UUID

import akka.actor.{PoisonPill, Props, ActorRef, Actor}
import akka.actor.Actor.Receive
import akka.event.{Logging, LoggingAdapter}
import akka.util.ByteString
import akka.zeromq.{ZMQMessage, ZeroMQExtension, Bind}
import com.typesafe.config.ConfigFactory

import scala.collection.{immutable, mutable}

/**
 * Created by baka on 18.03.15.
 */

class RoutingInfoActor(val address : String, val port : String) extends Actor {
  var uniquePort = port.toInt + 1
  val zmqSystem = ZeroMQExtension(context.system)
  val logger : LoggingAdapter = Logging.getLogger(context.system, this)
  val remote = context.actorSelection(ConfigFactory.load().getString("my.own.master-address"))
  val poolSize = ConfigFactory.load().getInt("my.own.pool-size")
  remote ! ConnectionRequest
  var routingAddresses = new mutable.HashMap[Int, String]
  var routingInfo = new mutable.HashMap[Int, ActorRef]
  var routingPairs = new mutable.HashMap[UUID, UUID]

  override def preStart(): Unit = {
    logger.debug("Publishers pool creation...")
    (0 until poolSize).foreach { id => //WHAT THE HELL WITH THIS SYNTAX???
      val bindString = "tcp://" + address + ":" + uniquePort.toString
      routingAddresses += ((id, bindString))
      routingInfo += ((id, context.system.actorOf(Props(classOf[Publisher], bindString, routingPairs))))
      uniquePort += 1
      logger.debug("Publisher with id {} created on {}", id, bindString)
    }
  }

  override def postStop(): Unit = {
    logger.debug("Cleaning up pool...")
    routingInfo.foreach((idPublisherTuple) => idPublisherTuple._2 ! PoisonPill)
  }

  /**
   * this method must return the same value as stringToId for
   * correct hash-driven pool work
   * @param uUID
   * @return
   */
  def uUIDToId(uUID: UUID): Int = math.abs(uUID.toString.hashCode % poolSize)

  def stringToId(uUID: String): Int = math.abs(uUID.hashCode % poolSize)

  def setNewUser(msg : SetMessage) : Unit = {
    val id = uUIDToId(msg.Key)
    val bindString = routingAddresses(id)
    logger.debug("Received Set message for UUID: {}, pub socket on: {}", msg.Key, bindString)
    sender ! bindString
  }

  def deleteUser(msg : DeleteClient) : Unit = {
    val clientUUID = msg.clientUUID
    if (routingPairs.contains(clientUUID)) {
      val robotUUID = routingPairs(clientUUID)
      routingPairs.remove(clientUUID)
      routingPairs.remove(robotUUID)
    }
  }

  def replyUserPublisherConnectionString(msg : GetMessage) : Unit = {
    val id = uUIDToId(msg.Key)
    val bindString = routingAddresses(id)
    logger.debug("Received Get message: {}, returning: {}", msg.Key, bindString)
    sender ! bindString
  }

  def associateUsers(msg : AddPair) : Unit = {
    routingPairs += ((msg.clientId, msg.actorId))
    routingPairs += ((msg.actorId, msg.clientId))
    logger.debug("Paired: {}", (msg.actorId, msg.clientId))
  }

  def replySendString() : Unit = {
    sender ! "tcp://" + address + ":" + port
  }

  def resendToPublisher(msg : ZMQMessage) : Unit = {
    val senderUUID = msg.frame(1).decodeString("UTF-8").split("\\.")(0)
    val publisherId = stringToId(senderUUID)
    routingInfo(publisherId) ! msg
  }

  override def receive: Receive = {
    case msg : ZMQMessage       => resendToPublisher(msg)
    case msg : SetMessage       => setNewUser(msg)
    case msg : AddPair          => associateUsers(msg)
    case msg : GetMessage       => replyUserPublisherConnectionString(msg)
    case msg : DeleteClient     => deleteUser(msg)
    case GetSendString          => replySendString()
    case Connected              => logger.info("Connected to main actor system...")
    case msg : String           => logger.debug("Received string: {}", msg); sender ! msg
    case aNonMsg                => logger.error("Some problems on ReceiverActor on address: " + address + ":" + port + " Msg: " + aNonMsg.toString)
  }
}
