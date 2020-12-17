package hackaton.elastic.api.request

import hackaton.elastic.api.query.QueryDefinition

case class CountRequest(indexes: Seq[String], query: Option[QueryDefinition] = None)
