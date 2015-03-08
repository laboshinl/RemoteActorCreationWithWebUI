import akka.actor._
import akka.pattern.ask
import scala.concurrent.Await
import scala.concurrent.duration._
import org.scalatest._

/**
 * Created by mentall on 08.03.15.
 */
class TestReceiverActor extends FeatureSpec with GivenWhenThen with MessagesOfReceiverActor{
  implicit val timeout : akka.util.Timeout = 2 minute

  feature("Correct Set/Get implementation"){
    scenario("Create Actor and send him Set"){
      implicit val system = ActorSystem("TestSystem")
      val testReceiverActor = system.actorOf(Props(new ReceiverActor[String, String]("127.0.0.1", "37177")))

      When("Sending set message")
      val respSet = Await.result(testReceiverActor ? SetMessage[String, String]("TestKey", "TestValue"), 2.minute)
      if (respSet.asInstanceOf[String].equals("Value added")) Then("Good for now, key/value is set")
      else Then("Not good, key/value is not set"); assert(false)

      When("Sending get message with existing key")
      val respGet1 = Await.result(testReceiverActor ? GetMessage[String]("TestKey"), 2.minute)
      if (respGet1.isInstanceOf[String] &&
        respGet1.asInstanceOf[String].equals("TestValue")) Then("Good for now, value correct")
      else if (respGet1.isInstanceOf[NoElementWithSuchKey]){
        Then("Not good, no element with such key")
        assert(false)
      }
      else {
        Then("Answer is incorrect")
        assert(false)
      };

      When("Sending get message with nonexisting key")
      val respGet2 = Await.result(testReceiverActor ? GetMessage[String]("NoneKey"), 2.minute)
      if (respGet2.isInstanceOf[String]) {
        Then("Answer is incorrect")
        assert(false)
      }
      else if (respGet1.isInstanceOf[NoElementWithSuchKey]) Then("Good, no element with such key")
    }
  }
}
