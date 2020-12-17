package hackaton.elastic.api.settings

sealed trait InputFileType

case object Csv extends InputFileType

case object Parquet extends InputFileType

case object Json extends InputFileType
