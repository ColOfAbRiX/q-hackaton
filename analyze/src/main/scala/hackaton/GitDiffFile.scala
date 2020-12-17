package hackaton

final case class GitDiffFile(file: RepoPath, added: Int, removed: Int)

object GitDiffFile {
  private val diffNumstatPattern = """^(\d+)\s+(\d+)\s+(.*)$""".r

  def fromOutput(line: String): Seq[GitDiffFile] =
    diffNumstatPattern
      .findFirstMatchIn(line)
      .map(m => Vector(GitDiffFile(RepoPath(m.group(3)), m.group(1).toInt, m.group(2).toInt)))
      .getOrElse(Vector.empty)
}
