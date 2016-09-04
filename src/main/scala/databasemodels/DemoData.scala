package databasemodels

import org.joda.time.DateTime
import play.api.libs.json.JsValue

case class DemoData(id: Long,
                     matchId: String,
                     shareCode: String,
                     demoUrl: String,
                     matchDate: DateTime,
                     matchData: JsValue,
                     map: String,
                     team1Score: Int,
                     team2Score: Int,
                     created: DateTime)
