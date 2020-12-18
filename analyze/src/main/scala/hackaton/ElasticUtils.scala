package hackaton

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.requests.bulk.BulkRequest
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import monix.eval.Task

object ElasticUtils {

  val client: ElasticClient = {
    ElasticClient(JavaClient(ElasticProperties("http://localhost:9200")))
  }

  def doesIndexExist(index: String): Task[Boolean] =
    Task
      .deferFutureAction { implicit s =>
        client.execute(indexExists(index))
      }
      .flatMap {
        case RequestSuccess(_, _, _, result) => Task(result.exists)
        case RequestFailure(_, _, _, error)  => Task(false)
      }

  def indexCreate(index: String): Task[Unit] =
    Task
      .deferFutureAction { implicit s =>
        client.execute(createIndex(index))
      }
      .flatMap(interpretResponse(_ => ()))

  def indexDelete(index: String): Task[Unit] =
    Task
      .deferFutureAction { implicit s =>
        client.execute {
          deleteIndex(index)
        }
      }
      .flatMap(interpretResponse(_ => ()))

  def insertDoc(index: String, docs: Seq[StatsEntry]): Task[Unit] = {
    Task
      .deferFutureAction { implicit s =>
        client.execute {
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
      }
      .flatMap(interpretResponse(x => x))
  }

  private def interpretResponse[A, B](f: A => B)(response: Response[A]): Task[B] =
    response match {
      case RequestSuccess(_, _, _, result) => Task(f(result))
      case RequestFailure(_, _, _, error)  => Task.raiseError(error.asException)
    }
}
