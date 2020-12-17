package hackaton

import cats.effect.ExitCode
import hackaton.Utils._
import monix.eval.{Task, TaskApp}

import java.nio.file._
import scala.annotation.tailrec

object Main extends TaskApp {

  val storage = scala.collection.mutable.Map[RepoPath, StatsEntry]()
  //  val targetDirectory = "C:\\Users\\BarnabyMalaj\\.Git\\QuantexaExplorer"

  def run(args: List[String]): Task[ExitCode] =
    args.headOption match {
      case Some(targetDirectory) =>
        val repoIndex = s"repoStats-${targetDirectory.md5}"
        for {
          indexPresent <- ElasticUtils.indexExists(repoIndex)
          _            <- if (!indexPresent) ElasticUtils.indexDelete(repoIndex) else Task.unit
          _            <- if (!indexPresent) ElasticUtils.indexCreate(repoIndex) else Task.unit
          _            <- Task(println(s"Initialised $repoIndex"))
          logEntries   <- getLogEntries(targetDirectory)
          _            <- Task(println(s"Discovered ${logEntries.size} commits"))
          _            <- processLogEntries(targetDirectory, logEntries)
          scores       <- calculateScore(storage.toMap)
          _            <- Task(println(s"Calculated ${scores.size} scores"))
          _ <- Task.parSequenceN(8)(
                 scores
                   .grouped(25).toVector
                   .map(batch => ElasticUtils.insertDoc(repoIndex, batch)),
               )
          _ <- Task(println(s"Data inserted into ElasticSearch"))
        } yield ExitCode.Success
      case None =>
        Task(System.err.println("Usage: MyApp name")).as(ExitCode(2))

    }

  /** When all the data is available, calculate the scores */
  private def calculateScore(data: Map[RepoPath, StatsEntry]): Task[Seq[StatsEntry]] = Task.pure {
    data
      .values
      .map { entry =>
        val allChanges = entry.changes.added + entry.changes.removed
        val authorsScores = entry.authors.map {
          case (author, stats) =>
            val userScore = (stats.changes.added + Math.abs(stats.changes.removed)).toDouble / allChanges.toDouble
            author -> stats.copy(score = userScore)
        }
        entry.copy(authors = authorsScores)
      }
      .toSeq
  }

  /** Discovers all log entries in a GIT repo */
  private def getLogEntries(targetDirectory: String): Task[Seq[GitLogEntry]] =
    Utils
      .run(targetDirectory, List("git", "log", "--pretty=format:" + GitLogEntry.gitFormat))
      .map {
        _.map(GitLogEntry.fromOutput)
          .sortBy(_.datetime)
          .take(1000) // NOTE: take(25) is just to have less data to test
      }

  /** Gets differences between each pair of commits */
  private def processLogEntries(targetDirectory: String, logEntries: Seq[GitLogEntry]): Task[Unit] = {
    val result = logEntries
      .sliding(2)
      .toVector
      .map { logs =>
        val diffs = interpretCommits(targetDirectory, logs(1), logs(0))
        diffs.flatMap(processDiffs(logs(1), _))
      }

    Task.sequence(result) *> Task.unit
  }

  /** Given two commits it discovers the differences */
  private def interpretCommits(
      targetDirectory: String,
      current: GitLogEntry,
      previous: GitLogEntry,
  ): Task[Vector[GitDiffFile]] =
    Utils
      .run(targetDirectory, List("git", "diff", "--numstat", s"${previous.commitRev}..${current.commitRev}"))
      .map {
        _.filter(!_.contains("=>"))
          .flatMap(GitDiffFile.fromOutput)
          .toVector
      }

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
            val added   = diffs.map(_.added).sum
            val removed = diffs.map(_.removed).sum
            path -> RepoChanges(added, removed, added - removed)
        }

    for ((dir, changes) <- affectedDirectories) {
      val statsEntry = storage.getOrElseUpdate(dir, StatsEntry(dir))
      val newStatsEntry = applyChanges(statsEntry, commit.author, changes)
        .copy(parent = getParent(statsEntry.path))

      storage.update(dir, newStatsEntry)
      updateMyParent(newStatsEntry, commit.author, changes)
    }
  }

  @tailrec
  private def updateMyParent(child: StatsEntry, author: RepoAuthor, changes: RepoChanges): Unit =
    getParent(child.path) match {
      case None => ()
      case Some(parent) =>
        val parentStat = storage.getOrElseUpdate(parent, StatsEntry(parent))
        val newParentStats = applyChanges(parentStat, author, changes)
          .copy(
            children = parentStat.children + child.path,
            parent = getParent(parentStat.path),
          )

        storage.update(parent, newParentStats)
        updateMyParent(parentStat, author, changes)
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

  /** Returns the parent of the node, if it's not the root */
  private def getParent(path: RepoPath): Option[RepoPath] =
    Option(Paths.get(path.name).getParent)
      .map(x => RepoPath(x.toString))
}
