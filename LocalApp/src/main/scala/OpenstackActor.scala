import akka.actor.Actor
import org.openstack4j.api.{Builders, OSClient}
import org.openstack4j.model.compute.Server
import org.openstack4j.openstack.OSFactory

/**
 * Created by mentall on 18.02.15.
 */
class OpenstackActor extends Actor with MyBeautifulOutput{
  val os : OSClient = OSFactory.builder()
    .endpoint("http://195.208.117.177:5000/v2.0")
    .credentials("student","domrachev.mail@gmail.com")
    .tenantName("student")
    .authenticate()

  val networks : java.util.List[String] = new java.util.LinkedList()
  networks.add("23043359-4dd3-482e-8854-75ce39d78aa6")
  var uniqueId : Long = 0
  var Servers = new scala.collection.mutable.HashMap[Long, Server]

  override def receive = {
    case StartMachine => {
      uniqueId += 1
      val svr = os.compute().servers().boot(Builders.server().
        availabilityZone("nova").name("actors-handler-" + uniqueId).
        flavor("ea07b19e-db4b-4aaf-8afb-1dd081f2aff1").
        image("ad189f85-d25c-453f-99ca-0b210c7c4e40").
        keypairName("student").networks(networks).build())
      Servers += ((uniqueId, svr))
      out("machine started")
      sender ! MachineTaskCompleted(uniqueId.toString)
    }
    case TerminateMachine(m) => {
      if(Servers.contains(m)){
        println("terminate server "+m)
        os.compute().servers().delete(Servers(m).getId)
        Servers -= m
        out("machine terminated")
        sender ! MachineTaskCompleted(m.toString)
      }
    }
    case msg : String => println(msg)
  }
}
