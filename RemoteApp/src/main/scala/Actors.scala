import akka.actor.Actor

/**
 * Created by mentall on 13.02.15.
 */
class ParrotActor(id : String, tcpString : String) extends Actor with MyBeautifulOutput{

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    out("Parrot Actor created: " + id + " With: " + tcpString)
  }

  override def receive: Receive = {
    case msg : String => {println(msg+msg+msg+"!"); sender ! msg+msg+msg+"!"}
    case CheckAddress => sender	 ! AddressIsOk
  }
}