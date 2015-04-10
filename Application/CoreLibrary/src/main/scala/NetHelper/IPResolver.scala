package NetHelper

import java.net.NetworkInterface

/**
 * Created by baka on 10.04.15.
 */

object IPResolver {
  def getMyIp(): String = {
    val addresses = NetworkInterface.getNetworkInterfaces.nextElement().getInetAddresses
    addresses.nextElement()
    addresses.nextElement().getHostAddress
  }
}
