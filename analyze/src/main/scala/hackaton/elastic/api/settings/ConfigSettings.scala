package hackaton.elastic.api.settings

import java.net.ServerSocket

sealed trait ConfigSettings {
  def getEsPort: Int

  def getSparkPort: Int

  def getAvailablePort: Int = {
    val dynamicServerPort = new ServerSocket(0)
    dynamicServerPort.setReuseAddress(true)
    val port = dynamicServerPort.getLocalPort
    dynamicServerPort.close()
    port
  }
}

object DefaultSettings extends ConfigSettings {
  private val esPort    = getAvailablePort
  private val sparkPort = getAvailablePort

  override def getEsPort: Int = esPort

  override def getSparkPort: Int = sparkPort
}

case class SimpleSettings(
    esPort: Int,
    sparkPort: Int,
    sparkAppName: String,
    additionalSparkLoaderSettings: Map[String, Any] = Map.empty,
) extends ConfigSettings {

  override def getEsPort: Int    = esPort
  override def getSparkPort: Int = sparkPort
}
