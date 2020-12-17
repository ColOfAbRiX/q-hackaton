package hackaton

import sys.process._

object Main extends App {

  // 1. Read all commits (with time limit)
  // 2. Find the changed lines and the author
  // 3. Checkout a commit

  val targetDirectory = "../../../Desktop/metals"

  val gitlogCommand = List("git", "log", "--pretty=format:" + GitLogEntry.gitFormat)
  val logEntries    = run(gitlogCommand).map(GitLogEntry.fromGitLog).take(10)

  logEntries
    .sliding(2)
    .filter(_.size == 2)
    .foreach { compareCommits =>
      val current        = compareCommits(1)
      val previous       = compareCommits(0)
      val gitdiffCommand = List("git", "diff", "--numstat", s"${previous.commitRev}..${current.commitRev}")
      val diff           = run(gitdiffCommand)

      println(s"DIFFS of ${previous.commitRev}..${current.commitRev}")
      println(diff)
      println("")
    }

  private def run(command: List[String]): List[String] =
    Process(command, new java.io.File(targetDirectory)).lazyLines.toList

}
