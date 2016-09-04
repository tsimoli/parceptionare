package parsers

import com.google.protobuf.CodedInputStream
import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks._
import stream.BitStream

object StringParser {
  def readDataTableString(reader: CodedInputStream): String = {
    val arrays = ListBuffer[Byte]()
    breakable {
      for (pos <- 0 until Int.MaxValue) {
        val b = reader.readRawByte()
        if ((b == 0) || (b == 10)) break
        arrays += b
      }
    }
    val bytes = arrays.toArray[Byte]

    new String(bytes, "ASCII");

  }
  
  def readDataTableString(reader: BitStream): String = {
    val arrays = ListBuffer[Byte]()
    breakable {
      for (pos <- 0 until Int.MaxValue) {
        val b = reader.readBits(8)(0)
        if ((b == 0) || (b == 10)) break
        arrays += b
      }
    }
    val bytes = arrays.toArray[Byte]

    new String(bytes, "ASCII");

  }
}