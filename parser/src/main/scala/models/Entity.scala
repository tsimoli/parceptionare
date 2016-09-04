package models

import stream.BitStream
import scala.collection.mutable.ListBuffer
import decoder.PropDecoder
import parsers.DemoParser
import rx.lang.scala.Subject

case class Entity(var id: Int, var serverClass: ServerClass) {

  var flattenedProps = serverClass.flattenedProps

  var props = new Array[PropertyEntry](flattenedProps.size)

  for (i <- 0 until flattenedProps.size) {
    props(i) = new PropertyEntry(flattenedProps(i), i)
  }

  def findProperty(propertyName: String) = {
    props.find { property => property.flattenedPropEntry.propertyName == propertyName }
  }

  def applyUpdate(reader: BitStream) = {
    val newWay = reader.readBit()
    var index = -1
    val entries = ListBuffer[PropertyEntry]()

    // Collect prop to be updated
    while ({ index = readFieldIndex(reader, index, newWay); index != -1 }) {
      entries += this.props(index)
    }

    // decode props that are updated
    entries.foreach { prop =>
      val decoded = prop.decode(reader, id)
      props.update(decoded._1, decoded._2) 
      decoded._2
    }
  }

  def readFieldIndex(reader: BitStream, lastIndex: Int, bNewWay: Boolean): Int = {
    if (bNewWay) {
      if (reader.readBit()) {
        return lastIndex + 1
      }
    }

    var ret = 0
    if (bNewWay && reader.readBit()) {
      ret = reader.readNumericBits(3) // read 3 bits
    } else {
      ret = reader.readNumericBits(7) // read 7 bits
      (ret & (32 | 64)) match {
        case 32 => ret = (ret & ~96) | (reader.readNumericBits(2) << 5)
        case 64 => ret = (ret & ~96) | (reader.readNumericBits(4) << 5)
        case 96 => ret = (ret & ~96) | (reader.readNumericBits(7) << 5)
        case _  =>
      }

    }

    if (ret == 0xFFF) {
      return -1
    }

    return lastIndex + 1 + ret
  }

  def leave() = {
    // Some destroy shit
  }

}

case class PropertyEntry(flattenedPropEntry: FlattenedPropEntry, index: Int) {

  val intSubject = Subject[Int]()
  val floatSubject = Subject[Float]()
  val vectorSubject = Subject[Vector]()
  val stringSubject = Subject[String]()
  val arraySubject = Subject[Array[Object]]()
 

  /**
   * Return tuple of index of prop and updated prop
   */
  def decode(reader: BitStream, id: Int) = {
    flattenedPropEntry.prop.rawType match {
      case 0 => {
        val value = reader.decodeInt(flattenedPropEntry.prop)
        if(id == 591 && (value == 250 || value == 316)  ) {
          val rr = 3
        }
        intSubject.onNext(value)
      }
      case 1 => {
        val value = reader.decodeFloat(flattenedPropEntry.prop)
        floatSubject.onNext(value)
      }
      case 2 => {
        val value = PropDecoder.decodeVector(reader, flattenedPropEntry.prop)
        vectorSubject.onNext(value)
      }

      case 4 => {
        val value = reader.decodeString(flattenedPropEntry.prop)
        stringSubject.onNext(value)
      }
      case 5 => {
        val value = PropDecoder.decodeArray(reader, flattenedPropEntry)
        arraySubject.onNext(value)
      }
      case 3 => {
        val value = PropDecoder.decodeVectorXY(reader, flattenedPropEntry.prop)
        vectorSubject.onNext(Vector(value.x, value.y, value.z))
      }
    }
    (index,this)
  }
}