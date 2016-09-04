package models

import models.DemoCommand._

object HitGroup extends Enumeration {
  val Generic = Value(0)
  val Head = Value(1)
  val Chest = Value(2)
  val Stomach = Value(3)
  val LeftArm = Value(4)
  val RightArm = Value(5)
  val LeftLeg = Value(6)
  val RightLeg = Value(7)
  val Gear = Value(10)
}