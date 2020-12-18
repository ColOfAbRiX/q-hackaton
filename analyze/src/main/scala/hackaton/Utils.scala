package hackaton

import monix.eval.Task
import java.math.BigInteger
import java.security.MessageDigest
import scala.sys.process.Process
import monix.execution.Scheduler

object Utils {

  /** Executes a shell command on the targetDirectory */
  def run(targetDirectory: String, command: List[String]): Task[Seq[String]] = Task {
    Process(command, new java.io.File(targetDirectory)).lazyLines
  }

  /** Batches execution of lists */
  def batchRequests[A, B](data: Seq[A])(f: Seq[A] => Task[B]): Task[Unit] =
    Task
      .parSequenceN(4) {
        data
          .grouped(25)
          .toVector
          .map(f)
      }
      .executeOn(Scheduler.io()) *>
    Task.unit

  implicit final class MD5String(private val self: String) extends AnyVal {
    /**
     * MD5 hash of the string
     */
    def md5: String = {
      val md     = MessageDigest.getInstance("MD5")
      val digest = md.digest(self.getBytes)
      val bigInt = new BigInteger(1, digest)
      bigInt.toString(16)
    }
  }

}

//  val finalData = calculateScore(storage.toMap)

//  finalData.foreach { stats =>
//    println(s"Changes for ${stats.path}:")
//    println(s"  Changes: ${stats.changes}")
//    println(s"  Children: ${stats.children.map(_.name).mkString(", ")}")
//    println(s"  Authors:")
//    stats.authors.foreach {
//      case (author, authorStats) =>
//        println(s"    $author: ${(authorStats.score * 100).toInt}%")
//    }
//    println("")
//  }
