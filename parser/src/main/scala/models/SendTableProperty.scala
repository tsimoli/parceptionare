package models

case class SendTableProperty() {
  var rawFlags = 0
  var sendPropertyFlags = rawFlags
  var name: String = ""
  var dataTableName: String = ""
  var lowValue: Float = 0
  var highValue: Float = 0
  var numberOfBits: Int = 0
  var numberOfElements: Int = 0
  var priority: Int = 0
  var rawType: Int = 0
  var sendPropertyType: Int = rawType
}

object SendPropertyFlags {
  val Unsigned = 1 << 0
  val Coord = 1 << 1
  val NoScale = 1 << 2
  val RoundDown = 1 << 3
  val RoundUp = 1 << 4
  val Normal = 1 << 5
  val Exclude = 1 << 6
  val XYZE = 1 << 7
  val InsideArray = 1 << 8
  val ProxyAlwaysYes = 1 << 9
  val IsVectorElement = 1 << 10
  val Collapsible = 1 << 11
  val CoordMp = 1 << 12
  val CoordMpLowPrecision = 1 << 13
  val CoordMpIntegral = 1 << 14
  val CellCoord = 1 << 15
  val CellCoordLowPrecision = 1 << 16
  val CellCoordIntegral = 1 << 17
  val ChangesOften = 1 << 18
  val VarInt = 1 << 19
}