package hackaton.elastic

import hackaton.elastic.api.request._
import hackaton.elastic.api.response._
import hackaton.elastic.api.settings.ConfigSettings
import hackaton.elastic.internals.parser.{ RequestParser, ResponseParser }
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.Executor._
import com.sksamuel.elastic4s.Functor._
import com.sksamuel.elastic4s.{ ElasticClient, ElasticError, ElasticProperties, RequestFailure, RequestSuccess, Response }
import io.circe.Encoder
import monix.eval.Task
import monix.execution.Scheduler
import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.reflect.runtime.universe.TypeTag
import com.sksamuel.elastic4s.http.JavaClient

case class Client(config: ConfigSettings, scheduler: Scheduler = Scheduler.io()) {

  //import com.sksamuel.elastic4s.embedded.{InternalLocalNode, LocalNode}
  //import com.sun.scenario.Settings
  //import com.google.common.io.Files
  //private def dir: File = Files.createTempDir()
  //private def absPath: String = dir.getAbsolutePath
  //
  //private lazy val node: InternalLocalNode = {
  //  System.setProperty("es.set.netty.runtime.available.processors", "false")
  //  LocalNode(
  //    Settings
  //      .builder()
  //      .put("cluster.name", "HaqathonElasticClient")
  //      .put("path.home", s"$absPath/elasticsearch-home")
  //      .put("path.data", s"$absPath/elasticsearch-data")
  //      .put("path.repo", s"$absPath/elasticsearch-repo")
  //      .put("http.port", config.getEsPort)
  //      .put("node.name", "test-node")
  //      .put("discovery.type", "single-node")
  //      .build(),
  //  )
  //}
  //
  //private val ec: ElasticClient = ElasticClient(node.client(shutdownNodeOnClose = true).client)

  val ec: ElasticClient = {
    ElasticClient(JavaClient(ElasticProperties("http://localhost:9200")))
  }

  def createIndex(createIndexRequest: CreateIndexRequest): Task[CreateIndexResponse] =
    executeTask(implicit s => ec.execute(RequestParser.parse(createIndexRequest)))(scheduler)
      .flatMap(parseResponse(_, ResponseParser.parse))

  def search(searchRequest: SearchRequest): Task[SearchResponse[Map[String, AnyRef]]] =
    executeTask(implicit s => ec.execute(RequestParser.parse(searchRequest)))(scheduler)
      .flatMap(parseResponse(_, ResponseParser.parse))

  def searchAndReturnAsCC[T <: Product: TypeTag: ClassTag](searchRequest: SearchRequest): Task[SearchResponse[T]] =
    search(searchRequest).map { searchResponse =>
      searchResponse.copy(documentsFound = searchResponse.documentsFound.map { document =>
        document.copy(source = convertSourceToCaseClass[T](document.source))
      })
    }

  def get(getRequest: GetRequest): Task[GetResponse[Map[String, AnyRef]]] =
    executeTask(implicit s => ec.execute(RequestParser.parse(getRequest)))(scheduler)
      .flatMap(parseResponse(_, ResponseParser.parse))

  def getAsCaseClass[T <: Product: TypeTag: ClassTag](getRequest: GetRequest): Task[GetResponse[T]] =
    get(getRequest).map { getResponse =>
      getResponse.copy(source = convertSourceToCaseClass[T](getResponse.source))
    }

  def update[T <: Product: TypeTag: ClassTag: Encoder](updateRequest: UpdateRequest[T]): Task[UpdateResponse] =
    executeTask(implicit s => ec.execute(RequestParser.parse(updateRequest)))(scheduler)
      .flatMap(parseResponse(_, ResponseParser.parse))

  def count(countRequest: CountRequest): Task[CountResponse] =
    executeTask(implicit s => ec.execute(RequestParser.parse(countRequest)))(scheduler)
      .flatMap(parseResponse(_, ResponseParser.parse))

  def exists(existsRequest: IndicesExistsRequest): Task[Boolean] =
    executeTask(implicit s => ec.execute(RequestParser.parse(existsRequest)))(scheduler)
      .flatMap(parseResponse(_, ResponseParser.parse))

  def deleteByQuery(deleteRequest: DeleteByQueryRequest): Task[DeleteByQueryResponse] =
    executeTask(implicit s => ec.execute(RequestParser.parse(deleteRequest)))(scheduler)
      .flatMap(parseResponse(_, ResponseParser.parse))

  def refresh(refreshRequest: RefreshRequest): Task[RefreshResponse] =
    executeTask(implicit s => ec.execute(RequestParser.parse(refreshRequest)))(scheduler)
      .flatMap(parseResponse(_, ResponseParser.parse))

  def catIndices(catIndicesRequest: CatIndicesRequest): Task[Seq[CatIndicesResponse]] =
    executeTask(implicit s => ec.execute(RequestParser.parse(catIndicesRequest)))(scheduler)
      .flatMap(parseResponse(_, ResponseParser.parse))

  def close(): Unit = {
    ec.close()
  }

  def esPort: Int = config.getEsPort

  private def executeTask[T](f: Scheduler => Future[T])(implicit io: Scheduler): Task[T] =
    Task
      .deferFutureAction(f)
      .executeOn(io)
      .asyncBoundary

  private def parseResponse[ESResponse, Result](
      response: Response[ESResponse],
      responseMapping: ESResponse => Result,
  ): Task[Result] =
    response match {
      case RequestSuccess(_, _, _, result) => Task.now(responseMapping(result))
      case RequestFailure(_, _, _, error) =>
        Task.raiseError(throw new Exception(formatError(error)))
    }

  private def convertSourceToCaseClass[T <: Product: TypeTag: ClassTag](source: Map[String, AnyRef]): T = {
    import hackaton.elastic.internals.reflect._
    fromMap[T](source)
  }

  private def formatError(err: ElasticError, rootError: Boolean = false): String = {
    val errorType =
      Option(err.`type`).map(err => s"${if (!rootError) "Error type" else "Root error type"}: $err, ").getOrElse("")
    val index = err.index.map(index => s"index: $index, ").getOrElse("")
    val causeOfError =
      err.causedBy.map(err => s", caused by an error of type ${err.`type`} for reason: ${err.reason}").getOrElse("")
    val rootCauseOfError: Seq[String] =
      Option(err.rootCause).map(_.map(err => formatError(err, true))).getOrElse(Seq.empty)
    val errors = Seq(
      s"${errorType}" ++ index ++ s"reason: ${err.reason} " ++ causeOfError,
    ) ++ rootCauseOfError
    errors.mkString("\n")
  }
}
