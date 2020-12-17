package hackaton.elastic.api.request

sealed trait RefreshPolicy

case object NoRefresh extends RefreshPolicy

case object Immediate extends RefreshPolicy

case object WaitUntil extends RefreshPolicy
