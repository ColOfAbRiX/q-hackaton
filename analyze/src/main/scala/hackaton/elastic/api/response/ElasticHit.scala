package hackaton.elastic.api.response

case class ElasticHit(
    id: String,
    index: String,
    version: Long,
    found: Boolean,
    score: Float,
    fields: Map[String, AnyRef],
    source: Map[String, AnyRef],
    highlight: Map[String, Seq[String]],
    innerHits: Map[String, InnerHits],
)

case class InnerHits(total: Long, max_score: Double, hits: Seq[InnerHit])

case class InnerHit(
    nested: Map[String, AnyRef],
    score: Double,
    source: Map[String, AnyRef],
    highlight: Map[String, Seq[String]],
)
