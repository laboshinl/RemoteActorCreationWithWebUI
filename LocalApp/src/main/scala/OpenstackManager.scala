import akka.actor.{ActorRef, Actor}
import akka.event.Logging
import com.typesafe.config.ConfigFactory
import org.openstack4j.api.{Builders, OSClient}
import org.openstack4j.model.compute.Server
import org.openstack4j.openstack.OSFactory

/**
 * Created by mentall on 18.02.15.
 */
class OpenstackManager extends Actor {
  val config = ConfigFactory.load()
  val logger = Logging.getLogger(context.system, self)
  val os : OSClient = OSFactory.builder()
    .endpoint   ( config.getString("my.own.openstack-ip")       )
    .credentials( config.getString("my.own.openstack-login"),
                  config.getString("my.own.openstack-password") )
    .tenantName ( config.getString("my.own.openstack-tenant")   )
    .authenticate()

  val networks : java.util.List[String] = new java.util.LinkedList()
  networks.add(   config.getString("my.own.openstack-network")  )
  var Servers = new scala.collection.mutable.HashMap[String, Server]

  override def receive = {
    case MachineStart => {
      val vmId = java.util.UUID.randomUUID.toString
      val svr = os.compute().servers().boot(Builders.server().
        availabilityZone(config.getString("my.own.openstack-availibility-zone")).
        name("actors-handler-" + vmId).
        flavor     ( config.getString("my.own.openstack-flavour")  ).
        image      ( config.getString("my.own.openstack-image")    ).
        keypairName( config.getString("my.own.openstack-key-pair") ).
        networks(networks).build())
      Servers += ((vmId, svr))
      logger.info("Open Stack Machine started...")
      sender ! TaskCompletedWithId(vmId)
    }
    case MachineTermination(m) => {
      if(Servers.contains(m)){
        logger.info("Terminating server " + m)
        os.compute().servers().delete(Servers(m).getId)
        Servers -= m
        logger.info("Open Stack Machine terminated...")
        sender ! TaskCompletedWithId(m)
      }
    }
    case msg : String => logger.debug("Received msg: " + msg)
  }
}
