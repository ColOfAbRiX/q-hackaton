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
      }.flatMap {
      case RequestSuccess(_, _, _, result) => Task(result.contains(index))
      case RequestFailure(_, _, _, error)  => Task.raiseError(error.asException)
    }

  def indexCreate(index: String): Task[Unit] =
    Task
      .deferFutureAction { implicit s =>
        client.execute(createIndex(index))
      }.flatMap {
        case RequestSuccess(_, _, _, _) => Task(())
        case RequestFailure(_, _, _, error)  => Task.raiseError(error.asException)
      }

}
