package models

import com.google.protobuf.CodedInputStream

case class CommandInfo(split: Array[Split])
object CommandInfo {
  def parse(reader: CodedInputStream) = {
    CommandInfo(Array(Split.parse(reader), Split.parse(reader)))
  }
}

case class Split(flags: Int,
                 viewOrigin: Vector,
                 viewAngles: QAngle,
                 localViewAngles: QAngle,
                 viewOrigin2: Vector,
                 viewAngles2: QAngle,
                 localViewAngles2: QAngle) {
  val FDEMO_NORMAL = 0
  val FDEMO_USE_ORIGIN2 = 1
  val FDEMO_USE_ANGLES2 = 2
  val FDEMO_NOINTERP = 4

  def getViewOrigin() = {
    if (flags.&(FDEMO_USE_ORIGIN2) != 0) viewOrigin2 else viewOrigin
  }

  def getViewAngles() = {
    if (flags.&(FDEMO_USE_ANGLES2) != 0) viewAngles else viewAngles2
  }

  def getLocalViewAngles = {
    if (flags.&(FDEMO_USE_ANGLES2) != 0) localViewAngles else localViewAngles2
  }
}

object Split {
  def parse(reader: CodedInputStream): Split = {
    Split(reader.readRawLittleEndian32(),
      Vector.parse(reader),
      QAngle.parse(reader),
      QAngle.parse(reader),
      Vector.parse(reader),
      QAngle.parse(reader),
      QAngle.parse(reader))
  }
}

case class Vector(var x: Float, var y: Float, var z: Float)


object Vector {
  def parse(reader: CodedInputStream) = {
    val v = new Vector(0, 0, 0)
    v.x = reader.readFloat
    v.y = reader.readFloat
    v.z = reader.readFloat
    v
  }
}

case class QAngle(x: Float, y: Float, z: Float)
object QAngle {
  def parse(reader: CodedInputStream) = {
    QAngle(reader.readFloat, reader.readFloat, reader.readFloat)
  }
}
