/**
 * Created by baka on 07.03.15.
 */


import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._
import org.scalatest._

class TestOpenStackActor extends FeatureSpec with GivenWhenThen {
  /**
   * after this test in Open Stack will be one addition VM with name actor-handler-1
   * u should write some code to make unique names
   * I think OS works good. No exceptions and null pointers i didn't see in this test
   */

  /*feature("VM creation") {
    scenario("Create and delete VM in OS") {
      implicit val system = ActorSystem("LocalSystem")
      implicit val timeout : Timeout = 2 minute
      val osActor : ActorRef = system.actorOf(Props(new OpenstackManager))
      When("Task was " + MachineStart)

      /**
       * two machines starts here, but instances have the same names, i think its bad
       * P.S. it's cool to use bang for routing :)
       */
      osActor ! MachineStart
      val f = (osActor ? MachineStart)
      val response = Await.result(f, 2 minute)
      response.isInstanceOf[TaskCompletedWithId] match {
        case true => Then("All good, machine started, response: " + response)
        case _ => Then("All bad, se response: " + response); assert(false)
      }
      val machineId = response.asInstanceOf[TaskCompletedWithId].id.toLong
      When("Task was " + MachineTermination)
      val secondResponse = Await.result((osActor ? MachineTermination(machineId)), 2 minute)
      secondResponse.isInstanceOf[TaskCompletedWithId] match {
        case true => Then("All good, machine stopped, response: " + secondResponse)
        case _ => Then("All bad, se response: " + secondResponse); assert(false)
      }

      osActor ! MachineTermination(1)
    }
  }*/
}
