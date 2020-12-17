package hackaton

import sys.process._
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import java.nio.file._

object Main extends App {

  val storage         = scala.collection.mutable.Map[RepoPath, StatsEntry]()
  val targetDirectory = "../../../Desktop/metals"
  val repoIndex       = "metals-repo"

  val computation =
    for {
      indexPresent <- ElasticUtils.indexExists(repoIndex)
      _            <- if (!indexPresent) ElasticUtils.indexCreate("metals-repo") else Task.unit
      logEntries   <- getLogEntries()
      _            <- processLogEntries(logEntries)
    } yield ()

  computation.runSyncUnsafe()

  /** Discovers all log entries in a GIT repo */
  private def getLogEntries(): Task[Seq[GitLogEntry]] =
    run(List("git", "log", "--pretty=format:" + GitLogEntry.gitFormat))
      .map {
        _.map(GitLogEntry.fromOutput)
          .sortBy(_.datetime)
        //.take(25) // NOTE: take(25) is just to have less data to test
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

  /** Given  two commits it discovers the differences */
  private def interpretCommits(current: GitLogEntry, previous: GitLogEntry): Task[Vector[GitDiffFile]] =
    run(List("git", "diff", "--numstat", s"${previous.commitRev}..${current.commitRev}"))
      .map(_.flatMap(GitDiffFile.fromOutput).toVector)

  /** Processes the differences between two commits */
  private def processDiffs(commit: GitLogEntry, diffs: Vector[GitDiffFile]): Task[Unit] = {
    val affectedDirectories =
      diffs
        .groupBy { gitDiff =>
          Option(Paths.get(gitDiff.file.name).getParent)
            .map(x => RepoPath(x.toString))
            .getOrElse(RepoPath(""))
        }
        .map {
          case (path, diffs) =>
            val added   = diffs.map(_.added).sum
            val removed = diffs.map(_.removed).sum
            path -> RepoChanges(added, removed, added - removed)
        }

    for ((dir, changes) <- affectedDirectories) {
      val statsEntry  = storage.getOrElse(dir, StatsEntry(dir))
      val authorStats = statsEntry.authors.getOrElse(commit.author, AuthorStats())

      val newPathChanges = statsEntry.changes + changes
      val newAuthorStats = authorStats.copy(authorStats.changes + changes)

      val newAuthors = statsEntry.authors + (commit.author -> newAuthorStats)
      val newStatsEntry = statsEntry.copy(
        authors = newAuthors,
        changes = newPathChanges,
      )

      storage += (dir -> newStatsEntry)
    }

    storage.values.toVector.sortBy(_.path.name).map(_.path.name).foreach(println)

    //storage.foreach {
    //  case (path, stats) =>
    //    println(s"Changes for $path:")
    //    println(s"  Changes: ${stats.changes}")
    //    println(s"  Authors:")
    //    stats.authors.foreach {
    //      case (author, authorStats) =>
    //        println(s"    $author: $authorStats")
    //    }
    //    println("")
    //}

    //println(storage)
    Task.unit
  }

  //private def processDiffs(commit: GitLogEntry, diffs: Vector[GitDiffFile]): Task[Unit] = {
  //  val affectedDirectories =
  //    diffs.map { path =>
  //      Paths.get(path.file.name).getParent.toString
  //    }.distinct
  //
  //  ElasticUtils
  //    .searchDirectories(repoIndex, affectedDirectories)
  //    .map(println)
  //}

  /** Executes a shell command on the targetDirectory */
  private def run(command: List[String]): Task[Seq[String]] = Task {
    Process(command, new java.io.File(targetDirectory)).lazyLines
  }

}
