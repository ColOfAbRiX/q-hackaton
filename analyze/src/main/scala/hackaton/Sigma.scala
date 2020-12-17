package hackaton

case class SigmaData(
    networkId: String,
    networkName: String,
    nodes: Set[SigmaNode],
    edges: Set[SigmaEdge],
)

case class SigmaNode(
    id: String,
    label: String,
    x: Double = 0.0,
    y: Double = 0.0,
    size: Double = 1.0,
    color: String = "#000000",
    `type`: Option[String] = None,
    image: Option[SigmaNodeImage] = None,
)

case class SigmaNodeImage(
    w: Option[Double],
    h: Option[Double],
    url: String,
    scale: Double,
    clip: Double,
)

case class SigmaEdge(
    id: String,
    label: String,
    source: String,
    target: String,
    `type`: Option[String] = None,
    size: Option[Double] = None,
    color: Option[String] = None,
    reversed: Option[Boolean] = None,
    dashed: Option[Boolean] = None,
)
