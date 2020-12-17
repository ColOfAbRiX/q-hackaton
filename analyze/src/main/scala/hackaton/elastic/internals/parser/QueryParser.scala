package hackaton.elastic.internals.parser

import hackaton.elastic.api.query.{BooleanQuery, ExistsQuery, MatchQuery, QueryDefinition, RangeQuery, RegexQuery, TermQuery, TermsQuery}
import com.sksamuel.elastic4s.requests.searches.queries.matches.{MatchQuery => E4SMatchQuery}
import com.sksamuel.elastic4s.requests.searches.queries.term.{TermQuery => E4STermQuery, TermsQuery => E4STermsQuery}
import com.sksamuel.elastic4s.requests.searches.queries.{BoolQuery => E4SBoolQuery, ExistsQuery => E4SExistsQuery, Query => E4SQuery, RangeQuery => E4SRangeQuery, RegexQuery => E4SRegexQuery}
import io.scalaland.chimney.dsl._

object QueryParser {

  def parse(request: QueryDefinition): E4SQuery =
    request match {
      case q @ BooleanQuery(_, _, _, _, _)  => parse(q)
      case q @ ExistsQuery(_)               => parse(q)
      case q @ TermQuery(_, _)              => parse(q)
      case q @ TermsQuery(_, _)             => parse(q)
      case q @ MatchQuery(_, _)             => parse(q)
      case q @ RegexQuery(_, _)             => parse(q)
      case q @ RangeQuery(_, _, _, _, _)    => parse(q)
    }

  def parse(request: BooleanQuery): E4SBoolQuery = request.transformInto[E4SBoolQuery]

  def parse(request: ExistsQuery): E4SExistsQuery = request.transformInto[E4SExistsQuery]

  def parse(request: TermQuery[_]): E4STermQuery = request.transformInto[E4STermQuery]

  def parse[T](request: TermsQuery[T]): E4STermsQuery[T] = request
    .into[E4STermsQuery[T]]
    .withFieldComputed(_.field, _.field)
    .withFieldComputed(_.values, _.values)
    .transform

  def parse(request: MatchQuery[_]): E4SMatchQuery = request.transformInto[E4SMatchQuery]

  def parse(request: RegexQuery): E4SRegexQuery = request.transformInto[E4SRegexQuery]

  def parse(request: RangeQuery): E4SRangeQuery = request
    .into[E4SRangeQuery]
    .withFieldComputed(_.lte, _.lte.map(_.value))
    .withFieldComputed(_.lt, _.lt.map(_.value))
    .withFieldComputed(_.gt, _.gt.map(_.value))
    .withFieldComputed(_.gte, _.gte.map(_.value))
    .transform

}
