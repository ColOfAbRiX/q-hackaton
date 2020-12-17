package hackaton

import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.fields.TextField
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import monix.eval.Task

object ElasticUtils {

  class ElasticException(error: ElasticError) extends Throwable

  val client: ElasticClient = {
    ElasticClient(JavaClient(ElasticProperties("http://localhost:9200")))
  }

  def indexCreate(index: String): Task[Unit] =
    Task
      .deferFutureAction { implicit s =>
        client.execute(createIndex(index))
      }.flatMap {
        case RequestSuccess(status, body, headers, result) => Task(())
        case RequestFailure(status, body, headers, error)  => Task.raiseError(new ElasticException(error))
      }

}
