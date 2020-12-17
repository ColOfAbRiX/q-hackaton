package hackaton

final case class RepoPath(name: String)

final case class RepoAuthor(name: String, email: String) {
  def show: String = s"$name | $email"
}

object RepoAuthor {
  def apply(id: String): RepoAuthor = {
    id.split('<').toList match {
      case ::(name, email) => RepoAuthor(name, email.head.reverse.tail.reverse)
      case Nil             => ???
    }
  }

}

final case class RepoChanges(
    added: Int = 0,
    removed: Int = 0,
    summary: Int = 0,
) {
  def +(that: RepoChanges): RepoChanges = this.copy(
    added = this.added + that.added,
    removed = this.removed + that.removed,
    summary = this.summary + that.summary,
  )

  def show: String = s"+$added -$removed ${added - removed}"
}

final case class AuthorStats(
    changes: RepoChanges = RepoChanges(),
    score: Double = 0,
)

final case class StatsEntry(
    path: RepoPath,
    parent: Option[RepoPath] = None,
    children: Set[RepoPath] = Set.empty,
    changes: RepoChanges = RepoChanges(),
    authors: Map[RepoAuthor, AuthorStats] = Map.empty,
)
