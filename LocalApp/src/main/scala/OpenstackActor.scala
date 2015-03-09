import akka.actor.Actor
import com.typesafe.config.ConfigFactory
import org.openstack4j.api.{Builders, OSClient}
import org.openstack4j.model.compute.Server
import org.openstack4j.openstack.OSFactory

/**
 * Created by mentall on 18.02.15.
 */
class OpenstackActor extends Actor with MyBeautifulOutput{
  val config = ConfigFactory.load()

  val os : OSClient = OSFactory.builder()
    .endpoint   ( config.getString("my.own.openstack-ip")       )
    .credentials( config.getString("my.own.openstack-login"),
                  config.getString("my.own.openstack-password") )
    .tenantName ( config.getString("my.own.openstack-tenant")   )
    .authenticate()

  val networks : java.util.List[String] = new java.util.LinkedList()
  networks.add(   config.getString("my.own.openstack-network")  )
  var uniqueId : Long = 0
  var Servers = new scala.collection.mutable.HashMap[Long, Server]

  override def receive = {
    case StartMachine => {
      uniqueId += 1
      val svr = os.compute().servers().boot(Builders.server().
        availabilityZone(config.getString("my.own.openstack-availibility-zone")).
        name("actors-handler-" + uniqueId).
        flavor     ( config.getString("my.own.openstack-flavour")  ).
        image      ( config.getString("my.own.openstack-image")    ).
        keypairName( config.getString("my.own.openstack-key-pair") ).
        networks(networks).build())
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
