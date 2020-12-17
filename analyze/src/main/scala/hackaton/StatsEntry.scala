package hackaton

final case class RepoPath(name: String)

final case class RepoAuthor(id: String)

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
}

final case class AuthorStats(
    changes: RepoChanges = RepoChanges(),
    score: Int = 0,
)

final case class StatsEntry(
    path: RepoPath,
    children: Set[RepoPath] = Set.empty,
    changes: RepoChanges = RepoChanges(),
    authors: Map[RepoAuthor, AuthorStats] = Map.empty,
)
