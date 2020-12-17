package hackaton

import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import java.nio.file._
import scala.annotation.tailrec

object Main extends App {

  val storage         = scala.collection.mutable.Map[RepoPath, StatsEntry]()
  val targetDirectory = "../../../Desktop/metals"
  val repoIndex       = "metals-repo"

  val computation =
    for {
      //indexPresent <- ElasticUtils.indexExists(repoIndex)
      //_            <- if (!indexPresent) ElasticUtils.indexCreate(repoIndex) else Task.unit
      logEntries <- getLogEntries()
      _          <- processLogEntries(logEntries)
    } yield ()

  computation.runSyncUnsafe()

  storage.foreach {
    case (path, stats) =>
      println(s"Changes for $path:")
      println(s"  Changes: ${stats.changes}")
      println(s"  Children: ${stats.children.map(_.name).mkString(", ")}")
      println(s"  Authors:")
      stats.authors.foreach {
        case (author, authorStats) =>
          println(s"    $author: $authorStats")
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
          .take(100) // NOTE: take(25) is just to have less data to test
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

  /** Given  two commits it discovers the differences */
  private def interpretCommits(current: GitLogEntry, previous: GitLogEntry): Task[Vector[GitDiffFile]] =
    Utils
      .run(List("git", "diff", "--numstat", s"${previous.commitRev}..${current.commitRev}"))
      .map {
        _.filter(!_.contains("=>"))
          .flatMap(GitDiffFile.fromOutput)
          .toVector
      }

  /** Processes the differences between two commits */
  private def processDiffs(commit: GitLogEntry, diffs: Vector[GitDiffFile]): Task[Unit] = Task {
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
      val statsEntry    = storage.getOrElseUpdate(dir, StatsEntry(dir))
      val newStatsEntry = applyChanges(statsEntry, commit.author, changes)

      storage.update(dir, newStatsEntry)
      updateMyParent(newStatsEntry, commit.author, changes)
    }
  }

  @tailrec
  private def updateMyParent(child: StatsEntry, author: RepoAuthor, changes: RepoChanges): Unit = {
    val maybeParent =
      Option(Paths.get(child.path.name).getParent)
        .map(x => RepoPath(x.toString))

    maybeParent match {
      case None => ()
      case Some(parent) =>
        val parentStat     = storage.getOrElseUpdate(parent, StatsEntry(parent))
        val newParentStats = applyChanges(parentStat, author, changes).copy(children = parentStat.children + child.path)

        storage.update(parent, newParentStats)
        updateMyParent(parentStat, author, changes)
    }
  }

  /** Updates an entry with new statistics */
  private def applyChanges(entry: StatsEntry, author: RepoAuthor, changes: RepoChanges): StatsEntry = {
    val authorStats    = entry.authors.getOrElse(author, AuthorStats())
    val newAuthorStats = authorStats.copy(authorStats.changes + changes)
    entry.copy(
      authors = entry.authors + (author -> newAuthorStats),
      changes = entry.changes + changes,
    )
  }
}
