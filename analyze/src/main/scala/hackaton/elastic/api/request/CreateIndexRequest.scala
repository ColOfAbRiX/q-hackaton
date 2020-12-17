package hackaton.elastic.api.request

case class CreateIndexRequest(indexName: String, indexSettings: IndexSettings = IndexSettings())

case class IndexSettings(indexSettings: Map[String, String] = Map.empty)
