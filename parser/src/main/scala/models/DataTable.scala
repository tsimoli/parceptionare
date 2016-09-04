package models

import scala.collection.mutable.ListBuffer
import java.math.BigInteger
import com.google.protobuf.CodedInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DataTable {
  var isEnd: Int = 0
  var isEndBoolean: Boolean = isEnd != 0
  var netTableName: String = ""
  var needsDecoder: Int = 0
  var needsDecoderBoolean: Boolean = needsDecoder != 0

  def parse(reader: CodedInputStream): List[SendProp] = {
    var props = new ListBuffer[SendProp]()
    while (!reader.isAtEnd()) {
      val desc = reader.readRawVarint32()
      val wireType = desc & 7
      val fieldNum = desc >> 3

      if (wireType == 2) {
        if (fieldNum == 2) {
          val len = reader.readRawVarint32()
          netTableName = new String(reader.readRawBytes(len))
        } else if (fieldNum == 4) {
          val len = reader.readRawVarint32()
          val sendProp = new SendProp()
          sendProp.parse(CodedInputStream.newInstance(reader.readRawBytes(len)))
          props += sendProp
        }
      } else if (wireType == 0) {
        val value = reader.readRawVarint32()

        fieldNum match {
          case 1 => if (value != 0) {
            isEndBoolean = true
          }
          case 3 => if (value != 0) needsDecoderBoolean = true
        }
      }
    }
    props.toList
  }

}

class SendProp() {
  var propType: Int = 0
  var varName: String = ""
  var flags: Int = 0
  var priority: Int = 0
  var dtName: String = ""
  var numElements: Int = 0
  var lowValue: Float = 0
  var highValue: Float = 0
  var numBits: Int = 0

  def parse(reader: CodedInputStream) = {
    while (!reader.isAtEnd()) {
      val desc = reader.readRawVarint32()
      val wireType = desc & 7
      val fieldNum = desc >> 3

      if (wireType == 2) {
        if (fieldNum == 2) {
          val len = reader.readRawVarint32()
          varName = new String(reader.readRawBytes(len))
        } else if (fieldNum == 5) {
          val len = reader.readRawVarint32()
          dtName = new String(reader.readRawBytes(len))
        }
      } else if (wireType == 0) {
        val value = reader.readRawVarint32()
        fieldNum match {
          case 1 => propType = value
          case 3 => flags = value
          case 4 => priority = value
          case 6 => numElements = value
          case 9 => numBits = value
          case _ =>
        }
      } else if (wireType == 5) {
        val value: Float = reader.readFloat()
   
        fieldNum match {
          case 7 => lowValue = value
          case 8 => highValue = value
          case _ =>
        }
      }
    }
  }
}