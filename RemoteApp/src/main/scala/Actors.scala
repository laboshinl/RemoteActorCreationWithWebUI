import akka.actor.Actor

/**
 * Created by mentall on 13.02.15.
 */
class ParrotActor extends Actor{
  override def receive: Receive = {
    case msg : String => {println(msg+msg+msg+"!"); sender ! msg+msg+msg+"!"}
    case CheckAddress => sender ! AddressIsOk
  }
}