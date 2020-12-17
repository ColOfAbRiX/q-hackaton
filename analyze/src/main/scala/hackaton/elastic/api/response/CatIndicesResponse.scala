package hackaton.elastic.api.response

case class CatIndicesResponse(index: String, primaryShards: Int, count: Long, primaryStoreSizeInBytes: Long)
