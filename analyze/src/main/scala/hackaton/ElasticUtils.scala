package hackaton

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.bulk.BulkRequest
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import hackaton.elastic.QElasticClient
import hackaton.elastic.api.request.{ CreateIndexRequest, IndicesExistsRequest }
import monix.eval.Task

object ElasticUtils {

  val qEsClient: QElasticClient = QElasticClient()

  def indexExists(index: String): Task[Boolean] =
    qEsClient.exists(IndicesExistsRequest(Seq(index)))

  def indexCreate(index: String): Task[Unit] =
    qEsClient
      .createIndex(CreateIndexRequest(index))
      .map(_ => ())

  def indexDelete(indexName: String): Task[Unit] = {
    Task
      .deferFutureAction { implicit s =>
        qEsClient.ec.execute {
          deleteIndex(indexName)
        }
      }.flatMap(interpretResponse(x => ()))
  }

  def searchDirectories(index: String, directories: Vector[String]): Task[Unit] =
    Task
      .deferFutureAction { implicit s =>
        qEsClient.ec.execute {
          val request = search(index).query {
            boolQuery().should(directories.map(x => matchQuery("path", x)))
          }
          println(s"REQUEST: ${request.show}")
          request
        }
      }.flatMap(interpretResponse(x => x))

  def insertDoc(index: String, docs: Seq[StatsEntry]): Task[Unit] = {
    Task
      .deferFutureAction { implicit s =>
        qEsClient.ec.execute {
          BulkRequest(
            docs.map(x =>
              IndexRequest(
                index = index,
                id = Option(x.path.name),
                fields = Seq(
                  SimpleFieldValue("path", x.path.name),
                  SimpleFieldValue("directSubdirs", x.children.map(_.name).toList),
                  // parent is not a list, need to change output
                  SimpleFieldValue("parent", x.parent.getOrElse("none")),
                  SimpleFieldValue("authors", x.authors.map(as => as._1.show -> as._2.score)),
                ),
              ),
            ),
          )
        }
      }.flatMap(interpretResponse(x => x))
  }

  private def interpretResponse[A, B](f: A => B)(response: Response[A]): Task[B] =
    response match {
      case RequestSuccess(_, _, _, result) => Task(f(result))
      case RequestFailure(_, _, _, error)  => Task.raiseError(error.asException)
    }
}
