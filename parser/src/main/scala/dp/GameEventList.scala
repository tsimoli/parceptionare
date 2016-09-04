package dp

import com.google.protobuf.CodedInputStream
import parsers.StringParser
import scala.collection.mutable.ListBuffer
import dp.handler.GameEventHandler
import parsers.DemoParser

class GameEventList {
  def parse(reader: CodedInputStream, parser: DemoParser) = {
    GameEventHandler.handleGameEventList(readDescriptors(reader), parser);
  }

  private def readDescriptors(reader: CodedInputStream) = {
    val descriptors = ListBuffer[Descriptor]()
    while (!reader.isAtEnd()) {
      val desc = reader.readRawVarint32()
      val wireType = desc & 7
      val fieldNum = desc >> 3

      if ((fieldNum != 2) && (fieldNum != 1)) {
        println("Throw shit")
      }

      val length = reader.readRawVarint32()
      val descriptor = new Descriptor()
      descriptor.parse(CodedInputStream.newInstance(reader.readRawBytes(length)))
      descriptors += descriptor
    }
    descriptors
  }
}

class Descriptor() {
  var eventId: Int = 0
  var name: String = ""
  var keys: Array[Key] = Array.empty[Key]
  def parse(reader: CodedInputStream) = {
    var keys = ListBuffer[Key]()
    while (!reader.isAtEnd()) {
      val desc = reader.readRawVarint32()
      val wireType = desc & 7
      val fieldNum = desc >> 3

      if ((wireType == 0) && (fieldNum == 1)) {
        val eventIda = reader.readRawVarint32()
        eventId = eventIda
      } else if ((wireType == 2) && (fieldNum == 2)) {
        val namee = new String(reader.readRawBytes(reader.readRawVarint32()))
        name = namee
      } else if ((wireType == 2) && (fieldNum == 3)) {
        var length = reader.readRawVarint32()
        var key = new Key();
        key.parse(CodedInputStream.newInstance(reader.readRawBytes(length)))
        keys += key
      }
    }
    this.keys = keys.toArray
  }

}

class Key() {
  var keyType: Int = 0
  var name: String = ""

  def parse(reader: CodedInputStream) = {
    while (!reader.isAtEnd()) {
      val desc = reader.readRawVarint32()
      val wireType = desc & 7
      val fieldNum = desc >> 3

      if ((wireType == 0) && (fieldNum == 1)) {
        keyType = reader.readRawVarint32()
      } else if ((wireType == 2) && (fieldNum == 2)) {
        val namee = new String(reader.readRawBytes(reader.readRawVarint32()))
        name = namee
      }
    }
  }
}