package hackaton.elastic.api.request

import hackaton.elastic.api.query.QueryDefinition

case class DeleteByQueryRequest(indices: Seq[String], query: QueryDefinition, refreshPolicy: RefreshPolicy = Immediate)
