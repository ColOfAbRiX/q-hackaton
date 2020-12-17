package hackaton

import sys.process._
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global

object Main extends App {

  val targetDirectory = "../../../Desktop/metals"

  val computation =
    for {
      _          <- ElasticUtils.indexCreate("metals-repo")
      logEntries <- getLogEntries()
      _          <- processLogEntries(logEntries)
    } yield ()

  computation.runSyncUnsafe()

  /** Discovers all log entries in a GIT repo */
  private def getLogEntries(): Task[Seq[GitLogEntry]] =
    run(List("git", "log", "--pretty=format:" + GitLogEntry.gitFormat))
      .map {
        _.map(GitLogEntry.fromOutput)
          .sortBy(_.datetime)
          .take(25) // NOTE: take(25) is just to have less data to test
      }

  /** Gets differences between each pair of commits */
  private def processLogEntries(logEntries: Seq[GitLogEntry]): Task[Unit] = {
    val result = logEntries
      .sliding(2)
      .toVector
      .map { logs =>
        val diffs = interpretCommits(logs(1), logs(0))
        diffs.flatMap(processDiffs(logs(1), _))
      }
    Task.sequence(result).map(_ => ())
  }

  /** Given two commits it discovers the differences */
  private def interpretCommits(current: GitLogEntry, previous: GitLogEntry): Task[Vector[GitDiffFile]] =
    run(List("git", "diff", "--numstat", s"${previous.commitRev}..${current.commitRev}"))
      .map(_.map(GitDiffFile.fromOutput).toVector)

  /** Processes the differences between two commits */
  private def processDiffs(commit: GitLogEntry, diffs: Vector[GitDiffFile]): Task[Unit] =
    updateUser(commit.author, diffs) *>
    updateRepo(diffs)

  private def updateUser(user: String, diffs: Vector[GitDiffFile]): Task[Unit] = {
    ???
  }

  private def updateRepo(diffs: Vector[GitDiffFile]): Task[Unit] = {
    ???
  }

  private def run(command: List[String]): Task[Seq[String]] = Task {
    Process(command, new java.io.File(targetDirectory)).lazyLines
  }

}
