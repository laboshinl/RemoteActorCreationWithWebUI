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

  feature("VM creation") {
    scenario("Create and delete VM in OS") {
      implicit val system = ActorSystem("LocalSystem")
      implicit val timeout : Timeout = 2 minute
      val osActor : ActorRef = system.actorOf(Props(new OpenstackActor))
      When("Task was " + StartMachine)

      /**
       * two machines starts here, but instances have the same names, i think its bad
       * P.S. it's cool to use bang for routing :)
       */
      osActor ! StartMachine
      val f = (osActor ? StartMachine)
      val response = Await.result(f, 2 minute)
      response.isInstanceOf[MachineTaskCompleted] match {
        case true => Then("All good, machine started, response: " + response)
        case _ => Then("All bad, se response: " + response); assert(false)
      }
      val machineId = response.asInstanceOf[MachineTaskCompleted].id.toLong
      When("Task was " + TerminateMachine)
      val secondResponse = Await.result((osActor ? TerminateMachine(machineId)), 2 minute)
      secondResponse.isInstanceOf[MachineTaskCompleted] match {
        case true => Then("All good, machine stopped, response: " + secondResponse)
        case _ => Then("All bad, se response: " + secondResponse); assert(false)
      }


    }
  }
}
