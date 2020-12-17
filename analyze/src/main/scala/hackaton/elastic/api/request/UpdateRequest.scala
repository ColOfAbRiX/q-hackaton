package hackaton.elastic.api.request

import io.circe.Encoder
import io.circe.syntax._

case class UpdateRequest[T: Encoder](index: String, id: String, data: T) {

  def dataAsJson: String = data.asJson.toString().filter(x => x != '\n')
}
