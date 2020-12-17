package hackaton

import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.http.JavaClient
import monix.eval.Task

object ElasticUtils {

  val client: ElasticClient = {
    ElasticClient(JavaClient(ElasticProperties("http://localhost:9200")))
  }

  def indexExists(index: String): Task[Boolean] =
    Task
      .deferFutureAction { implicit s =>
        client.execute(getIndex(Seq(index)))
      }
      .flatMap(interpretResponse(_.contains(index)))

  def indexCreate(index: String): Task[Unit] =
    Task
      .deferFutureAction { implicit s =>
        client.execute(createIndex(index))
      }
      .flatMap(interpretResponse(_ => ()))

  private def interpretResponse[A, B](f: A => B)(response: Response[A]): Task[B] =
    response match {
      case RequestSuccess(_, _, _, result) => Task(f(result))
      case RequestFailure(_, _, _, error)  => Task.raiseError(error.asException)
    }
}
