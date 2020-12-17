package hackaton.elastic.internals.parser

import hackaton.elastic.api.response.{
  CatIndicesResponse, CountResponse, CreateIndexResponse, DeleteByQueryResponse, Document, GetResponse, RefreshResponse,
  SearchResponse, UpdateResponse,
}
import com.sksamuel.elastic4s.requests.count.{ CountResponse => E4SCountResponse }
import com.sksamuel.elastic4s.requests.delete.{ DeleteByQueryResponse => E4SDeleteByQueryResponse }
import com.sksamuel.elastic4s.requests.indexes.admin.{ IndexExistsResponse => E4SIndexExistsResponse }
import com.sksamuel.elastic4s.requests.indexes.{ CreateIndexResponse => E4SCreateIndexResponse }
import com.sksamuel.elastic4s.requests.searches.{ SearchResponse => E4SSearchResponse }
import io.scalaland.chimney.dsl._
import com.sksamuel.elastic4s.requests.searches.{ SearchHit => E4SElasticHit }
import com.sksamuel.elastic4s.requests.get.{ GetResponse => E4SGetResponse }
import com.sksamuel.elastic4s.requests.update.{ UpdateResponse => E4SUpdateResponse }
import com.sksamuel.elastic4s.requests.indexes.admin.{ RefreshIndexResponse => E4SRefreshResponse }
import com.sksamuel.elastic4s.requests.cat.{ CatIndicesResponse => E4SCatIndicesResponse }

object ResponseParser {

  def parse(response: E4SCreateIndexResponse): CreateIndexResponse =
    response.transformInto[CreateIndexResponse]

  def parse(response: E4SSearchResponse): SearchResponse[Map[String, AnyRef]] =
    response
      .into[SearchResponse[Map[String, AnyRef]]]
      .withFieldComputed(_.documentsFound, _.hits.hits.map(ResponseParser.parse))
      .withFieldComputed(_.hits, _.hits.total.value)
      .transform

  def parse(response: E4SGetResponse): GetResponse[Map[String, AnyRef]] =
    response
      .into[GetResponse[Map[String, AnyRef]]]
      .withFieldComputed(_.id, _.id)
      .withFieldComputed(_.found, _.found)
      .withFieldComputed(_.index, _.index)
      .withFieldComputed(_.source, _.sourceAsMap)
      .transform

  def parse(response: E4SCountResponse): CountResponse =
    response
      .into[CountResponse]
      .withFieldComputed(_.count, _.count)
      .transform

  def parse(response: E4SIndexExistsResponse): Boolean = response.exists

  def parse(response: E4SDeleteByQueryResponse): DeleteByQueryResponse =
    DeleteByQueryResponse(response.deleted)

  def parse(elasticHit: E4SElasticHit): Document[Map[String, AnyRef]] =
    elasticHit
      .into[Document[Map[String, AnyRef]]]
      .withFieldComputed(_._id, _.id)
      .withFieldComputed(_.index, _.index)
      .withFieldComputed(_.source, _.sourceAsMap)
      .transform

  def parse(response: E4SUpdateResponse): UpdateResponse =
    UpdateResponse(response.index, response.id, response.result)

  def parse(response: E4SRefreshResponse): RefreshResponse = RefreshResponse()

  def parse(response: Seq[E4SCatIndicesResponse]): Seq[CatIndicesResponse] = response.map(parse)

  def parse(response: E4SCatIndicesResponse): CatIndicesResponse =
    response
      .into[CatIndicesResponse]
      .withFieldComputed(_.index, _.index)
      .withFieldComputed(_.count, _.count)
      .withFieldComputed(_.primaryShards, _.pri)
      .withFieldComputed(_.primaryStoreSizeInBytes, _.priStoreSize)
      .transform

}
