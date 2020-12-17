package hackaton.elastic.internals.parser

import hackaton.elastic.api.request._
import com.sksamuel.elastic4s.requests.admin.{
  IndicesExistsRequest => E4SIndicesExistsRequest, RefreshIndexRequest => E4SRefreshRequest,
}
import com.sksamuel.elastic4s.requests.cat.{ CatIndexes => E4SCatIndicesRequest }
import com.sksamuel.elastic4s.requests.count.{ CountRequest => E4SCountRequest }
import com.sksamuel.elastic4s.requests.delete.{ DeleteByQueryRequest => E4SDeleteByQueryRequest }
import com.sksamuel.elastic4s.requests.get.{ GetRequest => E4SGetRequest }
import com.sksamuel.elastic4s.requests.indexes.{
  CreateIndexRequest => E4SCreateIndexRequest, IndexSettings => E4SIndexSettings,
}
import com.sksamuel.elastic4s.requests.searches.sort.{ FieldSort => E4SSortDefinition }
import com.sksamuel.elastic4s.requests.searches.{ SearchRequest => E4SSearchRequest }
import com.sksamuel.elastic4s.requests.update.{ UpdateRequest => E4SUpdateRequest }
import com.sksamuel.elastic4s.{ Index, Indexes }
import io.circe.Encoder
import io.scalaland.chimney.dsl._
import com.sksamuel.elastic4s.requests.common.{ RefreshPolicy => E4SRefreshPolicy }
import com.sksamuel.elastic4s.requests.searches.sort.SortOrder.{ Asc, Desc }

object RequestParser {

  def parse(request: CreateIndexRequest): E4SCreateIndexRequest =
    request
      .into[E4SCreateIndexRequest]
      .withFieldComputed(_.name, _.indexName)
      .withFieldComputed(_.settings, createIndexRequest => parse(createIndexRequest.indexSettings))
      .transform

  private def parse(indexSettings: IndexSettings): E4SIndexSettings =
    indexSettings.indexSettings.foldLeft(new E4SIndexSettings) {
      case (e4sSettings, (key, value)) => e4sSettings.add(key, value)
    }

  def parse(request: SearchRequest): E4SSearchRequest =
    request
      .into[E4SSearchRequest]
      .withFieldComputed(_.indexes, x => Indexes(x.indexes))
      .withFieldComputed(_.query, _.query.map(QueryParser.parse))
      .withFieldComputed(_.size, _.size)
      .withFieldComputed(_.sorts, _.sortDefinition.map(parse))
      .transform

  def parse(request: CountRequest): E4SCountRequest =
    request
      .into[E4SCountRequest]
      .withFieldComputed(_.indexes, x => Indexes(x.indexes))
      .withFieldComputed(_.query, x => x.query.map(QueryParser.parse))
      .transform

  def parse(request: IndicesExistsRequest): E4SIndicesExistsRequest =
    E4SIndicesExistsRequest(Indexes(request.indices))

  def parse(request: DeleteByQueryRequest): E4SDeleteByQueryRequest =
    E4SDeleteByQueryRequest(
      indexes = request.indices,
      query = QueryParser.parse(request.query),
      refresh = Some(parse(request.refreshPolicy)),
    )

  def parse(request: GetRequest): E4SGetRequest =
    E4SGetRequest(Index(request.index), request.id)

  def parse[T](request: UpdateRequest[T])(implicit encoder: Encoder[T]): E4SUpdateRequest =
    E4SUpdateRequest(request.index, request.id, documentSource = Some(request.dataAsJson))

  def parse(request: RefreshRequest): E4SRefreshRequest = E4SRefreshRequest(request.indices)

  def parse(request: CatIndicesRequest): E4SCatIndicesRequest =
    E4SCatIndicesRequest(indexPattern = request.pattern, health = None)

  def parse(refreshPolicy: RefreshPolicy): E4SRefreshPolicy = refreshPolicy match {
    case Immediate => E4SRefreshPolicy.IMMEDIATE
    case NoRefresh => E4SRefreshPolicy.None
    case WaitUntil => E4SRefreshPolicy.WAIT_FOR
  }

  def parse(sortDefinition: SortDefinition): E4SSortDefinition = E4SSortDefinition(
    sortDefinition.field,
    order = sortDefinition.order match {
      case ASC => Asc
      case DSC => Desc
    },
  )
}
