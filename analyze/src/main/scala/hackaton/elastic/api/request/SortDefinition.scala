package hackaton.elastic.api.request

case class SortDefinition(field: String, order: SortOrder = ASC)

sealed trait SortOrder

case object ASC extends SortOrder

case object DSC extends SortOrder
