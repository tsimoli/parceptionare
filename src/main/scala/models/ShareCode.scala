package models

import org.joda.time.DateTime

case class ShareCode(id: Long = 0, shareCode: String, matchId: String, outcomeId: String, token: String, status: String, created: DateTime)
