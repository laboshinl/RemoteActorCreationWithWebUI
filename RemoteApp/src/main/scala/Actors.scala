import akka.actor.{ActorRef, Actor}
import akka.util.ByteString
import akka.zeromq._

import scala.collection.immutable

/**
 * Created by mentall on 13.02.15.
 */
class ParrotActor(id : String, subString : String, sendString : String) extends Actor with MyBeautifulOutput{

  val subSocket = ZeroMQExtension(context.system).newSubSocket(Connect(subString), Listener(self), Subscribe(id))
  val sendSocket  = ZeroMQExtension(context.system).newDealerSocket(Array(Connect(sendString), Listener(self)))
  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    out("Parrot Actor created: " + id + " With subString: " + subString + " and sendString: " + sendString)
  }

  override def receive: Receive = {
    case msg : ZMQMessage => sender ! ZMQMessage(immutable.Seq(ByteString(id), ByteString("I'm parrot!")) ++ msg.frames.drop(0))
    case msg : String => {println(msg+msg+msg+"!"); sender ! msg+msg+msg+"!"}
    case CheckAddress => sender	 ! AddressIsOk
  }
}