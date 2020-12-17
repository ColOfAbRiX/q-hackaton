package hackaton.elastic.api.response

case class GetResponse[T](id: String, index: String, found: Boolean, source: T)
