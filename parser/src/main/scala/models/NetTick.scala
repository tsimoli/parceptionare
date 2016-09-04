package models

import java.math.BigInteger
import com.google.protobuf.CodedInputStream

class NetTick {

  var tick: Int = 0
  var hostComputationTime: Int = 0
  var hostComputationTimeStdDeviation: Int = 0
  var hostFramestartStdDeviation: Int = 0

  def parse(reader: CodedInputStream) {
    while (!reader.isAtEnd()) {
      val desc = reader.readRawVarint32()
      val wireType = desc & 7
      val fieldNum = (desc.intValue()) >> 3
      val value = reader.readRawVarint32()

      fieldNum match {
        case 1 => tick = value
        case 4 => hostComputationTime = value
        case 5 => hostComputationTimeStdDeviation = value
        case 6 => hostFramestartStdDeviation = value
        case _ =>
      }
    }
  }
}