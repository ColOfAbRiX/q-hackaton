package hackaton.elastic.api.settings

import java.net.ServerSocket

sealed trait ConfigSettings {
  def getEsPort: Int
  def getEsHost: String
  def getAvailablePort: Int = {
    val dynamicServerPort = new ServerSocket(0)
    dynamicServerPort.setReuseAddress(true)
    val port = dynamicServerPort.getLocalPort
    dynamicServerPort.close()
    port
  }
}

object DefaultSettings extends ConfigSettings {
  def getEsHost: String = "localhost"
  def getEsPort: Int    = 9200
}

case class SimpleSettings(host: String, esPort: Int) extends ConfigSettings {
  def getEsHost: String = host
  def getEsPort: Int    = esPort
}
