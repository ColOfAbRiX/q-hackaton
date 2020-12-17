package hackaton.elastic.api.query

sealed trait QueryDefinition

case class BooleanQuery(
    filters: List[QueryDefinition],
    must: List[QueryDefinition],
    not: List[QueryDefinition],
    should: List[QueryDefinition],
    minimumShouldMatch: Option[String] = None,
)

case class ExistsQuery(field: String) extends QueryDefinition

case class TermQuery[T](field: String, value: T) extends QueryDefinition

case class TermsQuery[T](field: String, values: Iterable[T]) extends QueryDefinition

case class MatchQuery[T](field: String, value: T) extends QueryDefinition

case class RegexQuery(field: String, regex: String) extends QueryDefinition

case class RangeQuery(
    field: String,
    lte: Option[RangeLimitValue] = None,
    gte: Option[RangeLimitValue] = None,
    gt: Option[RangeLimitValue] = None,
    lt: Option[RangeLimitValue] = None,
) extends QueryDefinition

sealed trait RangeLimitValue {
  def value: Any
}

case class StringLimit(value: String) extends RangeLimitValue

case class LongLimit(value: Long) extends RangeLimitValue

case class IntLimit(value: Int) extends RangeLimitValue
