
import akka.actor._
import akka.pattern.ask
import scala.concurrent.Await
import scala.concurrent.duration._
import org.scalatest._
import org.zeromq.ZMQ
import receiver.ZHelpers.ZHelpers._
import receiver.ZMsg.ZMsg
import receiver.ReceiverActor.ReceiverActor
import receiver.MessagesOfReceiverActor._


/**
 * Created by mentall on 08.03.15.
 */


class TestReceiverActor extends FeatureSpec with GivenWhenThen with MessagesOfReceiverActor{
  implicit val timeout : akka.util.Timeout = akka.util.Timeout.durationToTimeout(2.minute)
  implicit val system = ActorSystem("TestSystem")

  feature("Correct Set/Get implementation"){
    scenario("Create Actor and send him Set") {
      val testReceiverActor = system.actorOf(Props(new ReceiverActor("127.0.0.1", "37177")))

      When("Sending set message")
      val respSet = Await.result(testReceiverActor ? SetMessage("TestKey"), 2.second)
      if ((respSet.asInstanceOf[String]).equals("tcp://127.0.0.1:37178")) Then("Good for now, key/value is set")
      else {Then("Not good, key/value is not set"); assert(false)}

      When("Sending get message with existing key")
      val respGet1 = Await.result(testReceiverActor ? GetMessage("TestKey"), 2.minute)
      if (respGet1.isInstanceOf[String] &&
        respGet1.asInstanceOf[String].equals("tcp://127.0.0.1:37178")) Then("Good for now, value correct")
      else if (respGet1.isInstanceOf[NoElementWithSuchKey]){
        Then("Not good, no element with such key")
        assert(false)
      }
      else {
        Then("Answer is incorrect")
        assert(false)
      };

      When("Sending get message with nonexisting key")
      val respGet2 = Await.result(testReceiverActor ? GetMessage("NoneKey"), 2.minute)
      if (respGet2.isInstanceOf[String]) {
        Then("Answer is incorrect")
        assert(false)
      }
      else if (respGet1.isInstanceOf[NoElementWithSuchKey]) Then("Good, no element with such key")
    }
  }

  /**
   * THis test is dummy, and must be rewrite, but now it can tell us that all works, and messages
   * can be send by ZEROMQ.
   * Its Great, I think,
   */
  feature("Send ZeroMQMessage to Router") {
    scenario("Send Test") {
      val testReceiverActor = system.actorOf(Props(new ReceiverActor("127.0.0.1", "55555")))
      val bindString = Await.result((testReceiverActor ? SetMessage("DummyKey")), 2 minute)
      println(bindString.asInstanceOf[String])
      /*val dummyActor = system.actorOf(Props(new Actor {
        var received : Boolean = false
        var subSocket : ActorRef = ZeroMQExtension(system).newSocket(SocketType.Sub, Listener(self), Connect(bindString.asInstanceOf[String]), SubscribeAll)
        override def receive: Receive = {
          case msg : ZMQMessage => received = true
          case msg => assert(false)
        }
      }))*/

      val ctx = ZMQ.context(1)
      val client = ctx.socket(ZMQ.DEALER)
      val rand = GetRandomGen
      client.setIdentity((rand.nextLong.toString + "-" + rand.nextLong.toString).getBytes)
      val identity = new String(client getIdentity)

      client.connect("tcp://127.0.0.1:55555")
      val sub = ctx.socket(ZMQ.SUB)
      sub.connect(bindString.asInstanceOf[String])
      sub.subscribe("DummyKey".getBytes())
      val msg = new ZMsg("DummyKey")
      client.sendMsg(msg)
      assert(sub.recvMsg().bodyToString.equals("payload"))
    }
  }
}
