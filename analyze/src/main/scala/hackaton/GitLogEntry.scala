package hackaton

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

final case class GitLogEntry(commitRev: String, author: String, datetime: LocalDateTime)

object GitLogEntry {
  val gitFormat = "%H : %an<%aE> : %aI"

  def fromOutput(line: String): GitLogEntry = {
    val split = line.split(" : ").toList
    GitLogEntry(
      commitRev = split(0),
      author = split(1),
      datetime = LocalDateTime.parse(split(2), DateTimeFormatter.ISO_OFFSET_DATE_TIME),
    )
  }
}
