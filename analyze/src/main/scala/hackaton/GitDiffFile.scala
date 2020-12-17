package hackaton

final case class RepoFile(name: String)

final case class GitDiffFile(file: RepoFile, added: Int, removed: Int)

object GitDiffFile {
  private val diffNumstatPattern = """^(\d+)\s+(\d+)\s+(.*)$""".r

  def fromOutput(line: String): GitDiffFile =
    diffNumstatPattern
      .findFirstMatchIn(line)
      .map(m => GitDiffFile(RepoFile(m.group(3)), m.group(1).toInt, m.group(2).toInt))
      .get
}
