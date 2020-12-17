package hackaton

import sys.process._

object Main extends App {

  val targetDirectory = "../../../Desktop/metals"

  val logEntries =
    run(List("git", "log", "--pretty=format:" + GitLogEntry.gitFormat))
      .map(GitLogEntry.fromOutput)
      .sortBy(_.datetime)
      .take(25) // NOTE: take(25) is just to have less data to test

  val commitDiffs =
    logEntries
      .sliding(2)
      .map(logs => processDiffs(logs(1), interpretCommits(logs(1), logs(0))))

  private def processDiffs(commit: GitLogEntry, diffs: Vector[GitDiffFile]): Unit = {
    updateUser(commit.author, diffs)
    updateRepo(diffs)
  }

  private def updateUser(user: String, diffs: Vector[GitDiffFile]): Unit = {
    ???
  }

  private def updateRepo(diffs: Vector[GitDiffFile]): Unit = {
    ???
  }

  private def interpretCommits(current: GitLogEntry, previous: GitLogEntry): Vector[GitDiffFile] =
    run(List("git", "diff", "--numstat", s"${previous.commitRev}..${current.commitRev}"))
      .map(GitDiffFile.fromOutput)
      .toVector

  private def run(command: List[String]): LazyList[String] =
    Process(command, new java.io.File(targetDirectory)).lazyLines

}
