import akka.actor.Actor
import akka.zeromq._

/**
 * Created by mentall on 13.02.15.
 */
class ParrotActor(id : String, tcpString : String) extends Actor with MyBeautifulOutput{

  val subSocket = ZeroMQExtension(context.system).newSubSocket(Connect(tcpString), Listener(self), Subscribe(id))
  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    out("Parrot Actor created: " + id + " With: " + tcpString)
  }

  override def receive: Receive = {
    case msg : ZMQMessage => sender
    case msg : String => {println(msg+msg+msg+"!"); sender ! msg+msg+msg+"!"}
    case CheckAddress => sender	 ! AddressIsOk
  }
}