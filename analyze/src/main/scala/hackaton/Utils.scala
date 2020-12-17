package hackaton

import scala.sys.process.Process
import hackaton.Main.targetDirectory
import monix.eval.Task

object Utils {

  /** Executes a shell command on the targetDirectory */
  def run(command: List[String]): Task[Seq[String]] = Task {
    Process(command, new java.io.File(targetDirectory)).lazyLines
  }

}
