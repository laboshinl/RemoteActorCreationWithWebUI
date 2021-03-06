import akka.actor.{PoisonPill, ActorRef, Actor}
import akka.event.Logging
import akka.util.ByteString
import akka.zeromq._
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization
import scala.collection.immutable

import core.messages._
import core.messages.Robot._

/**
 * Created by mentall on 13.02.15.
 */

abstract class RobotActor(id: String, subString: String, sendString: String, master: ActorRef) extends Actor {
  val logger = Logging.getLogger(context.system, this)
  val subSocket = ZeroMQExtension(context.system).newSubSocket(Connect(subString), Listener(self), Subscribe(id))
  val sendSocket  = ZeroMQExtension(context.system).newDealerSocket(Array(Connect(sendString), Listener(self)))

  @throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    subSocket ! PoisonPill
    sendSocket ! PoisonPill
    master ! RemoteActor.DeleteMe
  }
}

class ParrotActor(id: String, subString: String, sendString: String, master: ActorRef) extends RobotActor(id, subString, sendString, master) {

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    logger.debug("Parrot Actor created: " + id + " With subString: " + subString + " and sendString: " + sendString)
  }

  override def receive: Receive = {
    case msg : ZMQMessage => {
      logger.debug("Received ZMQ msg: " + msg)
      val payload = msg.frames.drop(1).foldLeft("")((ls : String, rs : ByteString) => ls + rs.decodeString("UTF-8"))
      logger.debug("Message payload: " + payload)
      val reply = if (msg.frames.size > 1)
        ZMQMessage(immutable.Seq(msg.frame(0), msg.frame(1), msg.frame(1), msg.frame(1)))
      else
        ZMQMessage(immutable.Seq(msg.frame(0), ByteString("Empty Payload".getBytes)))
      sendSocket ! reply
    }
    case msg : String => {
      logger.debug("Received akka msg: " + msg)
      sender ! msg + msg + msg + "!"
    }
    case General.Ping => sender()	! General.Pong
    case rc : RemoteCommand => println("got command: "+ rc.command); sendSocket ! comandToZMessage(rc)
  }

  sealed trait Status
  implicit val formats = Serialization.formats(FullTypeHints(List(classOf[Status])))
  def comandToZMessage(rc: RemoteCommand): ZMQMessage = {
    val jsonString = pretty(render(Extraction.decompose(rc)))
    ZMQMessage(immutable.Seq(ByteString(id + ".command"), ByteString(jsonString)))
  }
}

class CommandProxyActor(id: String, subString: String, sendString: String, master: ActorRef) extends RobotActor(id, subString, sendString, master) {
  sealed trait Status
  implicit val formats = Serialization.formats(FullTypeHints(List(classOf[Status])))

  def comandToZMessage(rc: RemoteCommand): ZMQMessage = {
    val jsonString = pretty(render(Extraction.decompose(rc)))
    ZMQMessage(immutable.Seq(ByteString(id + ".command"), ByteString(jsonString)))
  }

  override def receive: Receive = {
    case rc : RemoteCommand => sendSocket ! comandToZMessage(rc)
    case General.Ping => sender()	! General.Pong
  }
}