package util

import com.github.tminglei.slickpg._
import play.api.libs.json.{JsValue, Json}

trait MyPostgresDriver extends ExPostgresDriver
with PgArraySupport
with PgDateSupport
with PgRangeSupport
with PgHStoreSupport
with PgPlayJsonSupport
with PgSearchSupport
with PgNetSupport
with PgLTreeSupport {
  def pgjson = "jsonb" // jsonb support is in postgres 9.4.0 onward; for 9.3.x use "json"

  override val api = MyAPI

  object MyAPI extends API with ArrayImplicits
  with DateTimeImplicits
  with JsonImplicits
  with NetImplicits
  with LTreeImplicits
  with RangeImplicits
  with HStoreImplicits
  with SearchImplicits
  with SearchAssistants {
    implicit val strListTypeMapper = new SimpleArrayJdbcType[String]("text").to(_.toList)
    implicit val playJsonArrayTypeMapper =
      new AdvancedArrayJdbcType[JsValue](pgjson,
        (s) => utils.SimpleArrayUtils.fromString[JsValue](Json.parse)(s).orNull,
        (v) => utils.SimpleArrayUtils.mkString[JsValue](_.toString())(v)
      ).to(_.toList)
  }
}

object MyPostgresDriver extends MyPostgresDriver
