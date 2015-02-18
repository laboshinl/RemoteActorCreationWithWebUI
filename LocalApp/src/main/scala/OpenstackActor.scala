import org.openstack4j.api.OSClient
import org.openstack4j.openstack.OSFactory

/**
 * Created by mentall on 18.02.15.
 */
class OpenstackActor {
  val os : OSClient = OSFactory.builder()
    .endpoint("http://127.0.0.1:5000/v2.0")
    .credentials("admin","sample")
    .tenantName("admin")
    .authenticate();
}
