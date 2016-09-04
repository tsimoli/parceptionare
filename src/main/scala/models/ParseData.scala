package models

import play.api.libs.json.Json

case class ParseData(matchId: String, shareCode: String, url: String, matchDuration: Long, matchDate: Long)

object ParseData {
  implicit val parseDataFormat = Json.format[ParseData]
}