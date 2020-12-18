package hackaton

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.http.JavaClient
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

  def insertDoc(index: String, doc: StatsEntry): Task[Unit] =
    Task
      .deferFutureAction { implicit s =>
        client.execute {
          val fields: Seq[FieldValue] = Seq(
            SimpleFieldValue("path", doc.path.name),
            SimpleFieldValue("directSubdirs", doc.children.map(_.name).toList),
            SimpleFieldValue("parent", doc.parent.map(_.name).getOrElse("none")),
            SimpleFieldValue("authors", doc.authors.map(as => as._1.show -> as._2.score)),
          )
          IndexRequest(index = index, id = Option(doc.path.name), fields = fields)
        }
      }
      .flatMap(interpretResponse(x => x))

  private def interpretResponse[A, B](f: A => B)(response: Response[A]): Task[B] =
    response match {
      case RequestSuccess(_, _, _, result) => Task(f(result))
      case RequestFailure(_, _, _, error)  => Task.raiseError(error.asException)
    }
}
