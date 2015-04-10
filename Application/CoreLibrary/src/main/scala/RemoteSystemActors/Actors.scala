package RemoteSystemActors

import java.io.Serializable

import akka.actor.{Actor, ActorRef, PoisonPill}
import akka.event.Logging
import akka.util.{Timeout, ByteString}
import akka.zeromq._
import core.messages._
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization
import scala.concurrent.duration._
import scala.collection.immutable
/**
 * Created by mentall on 13.02.15.
 */

trait RobotMessages {
  @SerialVersionUID(1267L)
  case class RemoteCommand(command: String, args: immutable.List[String]) extends Serializable
}

object RobotActor extends RobotMessages {
  implicit val timeout: Timeout = 5 seconds
  def sendCommand(actorRef: ActorRef, command: String, args: immutable.List[String]): Unit = {
    actorRef ! RemoteCommand(command, args)
  }
}

abstract class RobotActor(id: String, subString: String, sendString: String, master: ActorRef) extends Actor
  with RobotMessages with GeneralMessages {
  val logger = Logging.getLogger(context.system, this)
  val subSocket = ZeroMQExtension(context.system).newSubSocket(Connect(subString), Listener(self), Subscribe(id))
  val sendSocket  = ZeroMQExtension(context.system).newDealerSocket(Array(Connect(sendString), Listener(self)))

  @throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    subSocket ! PoisonPill
    sendSocket ! PoisonPill
    RemoteActorCreator.deleteMePlease(master)
  }
}

class ParrotActor(id: String, subString: String, sendString: String, master: ActorRef)
  extends RobotActor(id, subString, sendString, master)
  with GeneralMessages {

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
    case Ping => sender()	! Pong
    case rc : RemoteCommand => println("got command: "+ rc.command)
  }
}

class CommandProxyActor(id: String, subString: String, sendString: String, master: ActorRef)
  extends RobotActor(id, subString, sendString, master) {
  sealed trait Status
  implicit val formats = Serialization.formats(FullTypeHints(List(classOf[Status])))

  def comandToZMessage(rc: RemoteCommand): ZMQMessage = {
    val jsonString = pretty(render(Extraction.decompose(rc)))
    ZMQMessage(immutable.Seq(ByteString(id + ".command"), ByteString(jsonString)))
  }

  override def receive: Receive = {
    case rc : RemoteCommand => sendSocket ! comandToZMessage(rc)
    case Ping => sender	! Pong
  }
}