package hackaton.elastic.api.request

import hackaton.elastic.api.query.QueryDefinition

case class SearchRequest(
    indexes: Seq[String],
    query: Option[QueryDefinition] = None,
    size: Option[Int] = None,
    sortDefinition: Seq[SortDefinition] = Seq.empty,
)
