package hackaton

import monix.eval.Task
import monix.execution.Scheduler.Implicits.global

import java.nio.file._
import scala.annotation.tailrec

object Main extends App {

  val storage = scala.collection.mutable.Map[RepoPath, StatsEntry]()
  val targetDirectory = "C:\\Users\\BarnabyMalaj\\.Git\\QuantexaExplorer"
  val repoIndex = "metals-repo"

  val computation =
    for {
      indexPresent <- ElasticUtils.indexExists(repoIndex)
      _            <- if (!indexPresent) ElasticUtils.indexDelete(repoIndex) else Task.unit
      _            <- if (!indexPresent) ElasticUtils.indexCreate(repoIndex) else Task.unit
      logEntries <- getLogEntries()
      _ <- processLogEntries(logEntries)
    } yield ()

  computation.runSyncUnsafe()

  storage.foreach {
    case (path, stats) =>
      println(s"Changes for $path:")
      println(s"  Changes: ${stats.changes.show}")
      println(s"  Children: ${stats.children.map(_.name).mkString(", ")}")
      println(s"  Authors:")
      stats.authors.foreach {
        case (author, authorStats) =>
          println(s"    ${author.show}: ${authorStats.changes.show}")
      }
      println("")
  }

  /** Discovers all log entries in a GIT repo */
  private def getLogEntries(): Task[Seq[GitLogEntry]] =
    Utils
      .run(List("git", "log", "--pretty=format:" + GitLogEntry.gitFormat))
      .map {
        _.map(GitLogEntry.fromOutput)
          .sortBy(_.datetime)
          .take(10) // NOTE: take(25) is just to have less data to test
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

    Task.sequence(result) *> Task.unit
  }

  /** Given two commits it discovers the differences */
  private def interpretCommits(current: GitLogEntry, previous: GitLogEntry): Task[Vector[GitDiffFile]] =
    Utils
      .run(List("git", "diff", "--numstat", s"${previous.commitRev}..${current.commitRev}"))
      .map(_.flatMap(GitDiffFile.fromOutput).toVector)

  /** Processes the differences between two commits */
  private def processDiffs(commit: GitLogEntry, diffs: Vector[GitDiffFile]): Task[Unit] = Task {
    val affectedDirectories: Map[RepoPath, RepoChanges] =
      diffs
        .groupBy { gitDiff =>
          Option(Paths.get(gitDiff.file.name).getParent)
            .map(x => RepoPath(x.toString))
            .getOrElse(RepoPath(""))
        }
        .map {
          case (path, diffs) =>
            val added = diffs.map(_.added).sum
            val removed = diffs.map(_.removed).sum
            path -> RepoChanges(added, removed, added - removed)
        }

    for ((dir, changes) <- affectedDirectories) {
      val statsEntry = storage.getOrElseUpdate(dir, StatsEntry(dir))
      val authorStats = statsEntry.authors.getOrElse(commit.author, AuthorStats())
      val newAuthorStats = authorStats.copy(authorStats.changes + changes)

      val newStatsEntry = statsEntry.copy(
        authors = statsEntry.authors + (commit.author -> newAuthorStats),
        changes = statsEntry.changes + changes,
      )

      updateMyParent(dir)
      storage.update(dir, newStatsEntry)
    }
  }

  @tailrec
  private def updateMyParent(child: RepoPath): Unit = {
    // this doesn't work - we'd need to map each part individually
    Option(Paths.get(child.name).getParent)
      .map(x => RepoPath(x.toString)) match {
      case None => ()
      case Some(parent) =>
        val parentStat = storage.getOrElseUpdate(parent, StatsEntry(parent))
        storage.update(parent, parentStat.copy(children = parentStat.children + child))
        updateMyParent(parent)
    }
  }

}
