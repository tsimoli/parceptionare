package decoder

import models.SendTableProperty
import models.SendPropertyFlags
import models.Vector
import stream.BitStream
import models.FlattenedPropEntry

object PropDecoder {
  def decodeVector(reader: BitStream, prop: SendTableProperty): Vector = {
    if ((prop.rawFlags & SendPropertyFlags.Normal) == SendPropertyFlags
      .Normal) {
    }

    var vector = new Vector(0, 0, 0)

    vector.x = reader.decodeFloat(prop);
    vector.y = reader.decodeFloat(prop);

    if (!((prop.rawFlags & SendPropertyFlags.Normal) == SendPropertyFlags.Normal)) {
      vector.z = reader.decodeFloat(prop);
    } else {
      val isNegative = reader.readBit();

      val absolute = vector.x * vector.x + vector.y * vector.y;
      if (absolute < 1.0f) {
        vector.z = Math.sqrt(1 - absolute).toFloat
      } else {
        vector.z = 0f;
      }

      if (isNegative)
        vector.z *= -1;
    }
    vector;
  }

  def decodeVectorXY(reader: BitStream, prop: SendTableProperty): Vector = {
    val x = reader.decodeFloat(prop);
    val y = reader.decodeFloat(prop);

    return new Vector(x, y, 0);
  }
  
  def decodeArray(reader: BitStream, flattenedProp:  FlattenedPropEntry): Array[Object] = {
    var numElements = flattenedProp.prop.numberOfElements
    var maxElements = numElements;

    var numBits = 1;

    while ({ (maxElements >>= 1); maxElements != 0 } ) {
      numBits = numBits + 1;
    }

    val nElements = reader.readNumericBits(numBits);

    var result = new Array[Object](nElements)

    val temp = new FlattenedPropEntry("", flattenedProp.arrayElementProp, null);
    for(i <- 0 until nElements) {
      result(i) = decodeProp(temp, reader)
    }
    
    return result;
  }
  
  def decodeProp(prop:  FlattenedPropEntry, reader: BitStream): Object = {
    val sendProp = prop.prop
    
    prop.prop.rawType match {
      case 0 => reader.decodeInt(prop.prop)
      case 1 => reader.decodeFloat(prop.prop).toString()
      case 2 => decodeVector(reader, prop.prop)
      case 5 => decodeArray(reader, prop)
      case 4 => reader.decodeString(prop.prop).toString()
      case 3 => decodeVectorXY(reader, prop.prop)
    }
   
  }
  
}