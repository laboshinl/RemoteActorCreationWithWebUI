package LocalAppActors

import java.io.Serializable

import akka.actor.Actor
import akka.event.Logging
import com.typesafe.config.ConfigFactory
import core.messages._
import org.openstack4j.api.{Builders, OSClient}
import org.openstack4j.model.compute.Server
import org.openstack4j.openstack.OSFactory

/**
 * Created by mentall on 18.02.15.
 */


trait OpenStackManagerMessages {
  @SerialVersionUID(84L)
  case object MachineStart extends Serializable
  @SerialVersionUID(85L)
  case class MachineTermination(vmId : String) extends Serializable
}

class OpenStackManager extends Actor
  with OpenstackManagerMessages with TaskManagerMessages{
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

  def startMachine(image: String, namePrefix: String): String = {
    val vmId = java.util.UUID.randomUUID.toString
    val svr = os.compute().servers().boot(buildVMConfiguration(vmId, image, namePrefix))
    Servers += ((vmId, svr))
    logger.info("Open Stack Machine started...")
    vmId
  }

  /*
  TODO: Если машины нет в списке, то все рухнет
   */
  def terminateMachine(vmId: MachineTermination): Unit = {
    if(Servers.contains(vmId.vmId)) {
      logger.info("Terminating server " + vmId.vmId)
      os.compute().servers().delete(Servers(vmId.vmId).getId)
      Servers -= vmId.vmId
      logger.info("Open Stack Machine terminated...")
      sender ! TaskCompletedWithId(vmId.vmId)
    }
  }

  /*
  TODO: Переделать возвращаемое значение в кортеж или мап
   */
  override def receive = {
    // я не буду обращать на это внмания, ок?
    case ("startRemoteAppAndMessageRouter") => {
      println("\n\nstartRemoteAppAndMessageRouter\n\n")
      sender ! (
        startMachine("my.own.openstack-remoteapp-image","remoteapp-"),
        startMachine("my.own.openstack-router-image","router-")
        )
    }
    case MachineStart => sender ! TaskCompletedWithId(startMachine("my.own.openstack-remoteapp-image","remoteapp-"))
    case vmId: MachineTermination => terminateMachine(vmId)
    case msg : String => logger.debug("Received msg: " + msg)
  }

  /**
   * unused method? for delete?
   * It's used in startMachine
   * Ok, Im blind ^)^
   * @return
   */
  def buildVMConfiguration(vmId: String, image: String, namePrefix: String) = {
    Builders.server().
      availabilityZone(config.getString("my.own.openstack-availibility-zone")).
      name(namePrefix + vmId).
      flavor(config.getString("my.own.openstack-flavour")).
      image(config.getString(image)).
      keypairName(config.getString("my.own.openstack-key-pair")).
      networks(networks).build()
  }
}
