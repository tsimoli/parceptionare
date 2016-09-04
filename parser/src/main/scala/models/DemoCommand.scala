package models

object DemoCommand extends Enumeration {
  val Signon = Value(1)
  val Packet = Value(2)
  val Synctick = Value(3)
  val ConsoleCommand = Value(4)
  val UserCommand = Value(5)
  val DataTables = Value(6)
  val Stop = Value(7)
  val CustomData = Value(8)
  val StringTables = Value(9)
  val LastCommand = Value(10)
  val FirstCommand = Value(11)
}