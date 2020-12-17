package hackaton

import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.ElasticDsl._
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

  private def interpretResponse[A, B](f: A => B)(response: Response[A]): Task[B] =
    response match {
      case RequestSuccess(_, _, _, result) => Task(f(result))
      case RequestFailure(_, _, _, error)  => Task.raiseError(error.asException)
    }
}
