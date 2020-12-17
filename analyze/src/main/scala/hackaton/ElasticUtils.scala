package hackaton

import hackaton.elastic.QElasticClient
import hackaton.elastic.api.request.{ CreateIndexRequest, IndicesExistsRequest }
import monix.eval.Task

object ElasticUtils {

  val esClient: QElasticClient = QElasticClient()

  def indexExists(index: String): Task[Boolean] =
    esClient.exists(IndicesExistsRequest(Seq(index)))

  def indexCreate(index: String): Task[Unit] =
    esClient
      .createIndex(CreateIndexRequest(index))
      .map(_ => ())

  //private def interpretResponse[A, B](f: A => B)(response: Response[A]): Task[B] =
  //  response match {
  //    case RequestSuccess(_, _, _, result) => Task(f(result))
  //    case RequestFailure(_, _, _, error)  => Task.raiseError(error.asException)
  //  }
}
