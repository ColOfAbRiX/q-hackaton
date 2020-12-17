package hackaton.elastic.api.response

case class SearchResponse[T](documentsFound: Array[Document[T]], hits: Long)

//Every document in ElasticSearch has a unique id, an index it belongs to and the source (the contents of the document)
case class Document[T](_id: String, index: String, source: T)
