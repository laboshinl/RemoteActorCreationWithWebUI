import akka.actor.Actor
import akka.event.Logging
import com.typesafe.config.ConfigFactory
import org.openstack4j.api.{Builders, OSClient}
import org.openstack4j.model.compute.Server
import org.openstack4j.openstack.OSFactory

import core.messages._

/**
 * Created by mentall on 18.02.15.
 */

class OpenStackManager extends Actor {
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

  def startMachine(): Unit = {
    val vmId = java.util.UUID.randomUUID.toString
    val svr = os.compute().servers().boot(buildVMConfiguration(vmId))
    Servers += ((vmId, svr))
    logger.info("Open Stack Machine started...")
    sender ! TaskCompletedWithId(vmId)
  }

  def terminateMachine(vmId: MachineTermination): Unit = {
    if(Servers.contains(vmId.vmId)) {
      logger.info("Terminating server " + vmId.vmId)
      os.compute().servers().delete(Servers(vmId.vmId).getId)
      Servers -= vmId.vmId
      logger.info("Open Stack Machine terminated...")
      sender ! TaskCompletedWithId(vmId.vmId)
    }
  }
  override def receive = {
    case MachineStart => startMachine()
    case vmId: MachineTermination => terminateMachine(vmId)
    case msg : String => logger.debug("Received msg: " + msg)
  }

  /**
   * unused method? for delete?
   * @return
   */
  def buildVMConfiguration(vmId: String) = {
    Builders.server().
      availabilityZone(config.getString("my.own.openstack-availibility-zone")).
      name("actors-handler-" + vmId).
      flavor(config.getString("my.own.openstack-flavour")).
      image(config.getString("my.own.openstack-image")).
      keypairName(config.getString("my.own.openstack-key-pair")).
      networks(networks).build()
  }
}
