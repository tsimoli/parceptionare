package models

import org.joda.time.DateTime
import play.api.libs.json.Json

object DemoStatus extends Enumeration {
  type DemoStatus = Value
  val newDemo = Value("newDemo")
  val parsing = Value("parsing")
  val parsed = Value("parsed")
  val error = Value("error")
}

/*
  @param uniqueName consists of sign on length and demo ticks
 */
case class Demo(
                 id: Long = 0,
                 uniqueName: String,
                 status: String,
                 map: String,
                 team1Score: Int,
                 team2Score: Int,
                 changed: DateTime,
                 created: DateTime
                 )

object Demo {
  implicit val demoFormat = Json.format[Demo]
}