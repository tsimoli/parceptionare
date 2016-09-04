package stream;

import org.apache.commons.lang3.tuple.Pair;

import models.SendPropertyFlags;
import models.SendTableProperty;

public class BitStream {

	final int[] words;
	int pos;

	public BitStream(byte[] data) {

		// it seems valve started not sending trailing zero bits in a stream
		// under certain circumstances?
		// example match id 714979634
		// append a zero word at the end, which seems to fix it for now
		this.words = new int[(data.length + 7) / 4];
		this.pos = 0;

		int akku = 0;
		for (int i = 0; i < data.length; i++) {
			int shift = 8 * (i & 3);
			int val = ((int) data[i]) & 0xFF;
			akku = akku | (val << shift);
			if ((i & 3) == 3) {
				words[i / 4] = akku;
				akku = 0;
			}
		}
		if ((data.length & 3) != 0) {
			words[data.length / 4] = akku;
		}
		words[words.length - 1] = 0;
	}

	public int peekNumericBits(int num) {
		int l = words[pos >> 5];
		int r = words[(pos + num - 1) >> 5];
		int shift = pos & 31;
		int rebuild = (r << (32 - shift)) | (l >>> shift);
		return (rebuild & ((int) ((long) 1 << num) - 1));
	}

	public int readNumericBits(int num) {
		int result = peekNumericBits(num);
		pos += num;
		return result;
	}

	public boolean readBit() {
		boolean result = peekNumericBits(1) != 0;
		pos += 1;
		return result;
	}

	public byte[] readBits(int num) {
		byte[] result = new byte[(num + 7) / 8];
		int i = 0;
		while (num > 7) {
			num -= 8;
			result[i] = (byte) readNumericBits(8);
			i++;
		}
		if (num != 0) {
			result[i] = (byte) readNumericBits(num);
		}
		return result;
	}

	public String readString(int num) {
		StringBuffer buf = new StringBuffer();
		while (num > 0) {
			char c = (char) readNumericBits(8);
			if (c == 0) {
				break;
			}
			buf.append(c);
			num--;
		}
		return buf.toString();
	}

	public int readVarInt() {
		int run = 0;
		int value = 0;

		while (true) {
			int bits = readNumericBits(8);
			value = value | ((bits & 0x7f) << run);
			run += 7;
			if ((bits >> 7) == 0 || run == 35) {
				break;
			}
		}
		return value;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();

		int min = Math.max(0, (pos - 32) / 32);
		int max = Math.min(words.length - 1, (pos + 63) / 32);
		for (int i = min; i <= max; i++) {
			buf.append(new StringBuffer(String.format("%32s",
					Integer.toBinaryString(words[i])).replace(' ', '0'))
					.reverse());
		}
		buf.insert(pos - min * 32, '*');
		return buf.toString();
	}

	public int readUBitInt() {
		int ret = readNumericBits(6);
		switch (ret & (16 | 32)) {
		case 16:
			ret = (ret & 15) | (readNumericBits(4) << 4);
			break;
		case 32:
			ret = (ret & 15) | (readNumericBits(8) << 4);
			break;
		case 48:
			ret = (ret & 15) | (readNumericBits(32-4) << 4);
			break;
		}
		return ret;
	}

	public Integer decodeInt(SendTableProperty prop) {

		if ((prop.rawFlags() & SendPropertyFlags.VarInt()) == SendPropertyFlags
				.VarInt()) {
			if ((prop.rawFlags() & SendPropertyFlags.ChangesOften()) == SendPropertyFlags
					.ChangesOften()) {
				return readVarInt();
			} else {
				return readVarInt();
			}
		} else {
			if ((prop.rawFlags() & SendPropertyFlags.Unsigned()) == SendPropertyFlags
					.Unsigned()) {
				return readNumericBits(prop.numberOfBits());
			} else {
				return readNumericBits(prop.numberOfBits());
			}
		}

	}

	public float decodeFloat(SendTableProperty prop) {

		float fVal = 0.0f;
		long dwInterp;

		Pair<Boolean, Float> resultPair = decodeSpecialFloat(prop, fVal);
		
		if (resultPair.getLeft())
			return resultPair.getRight();

		fVal = resultPair.getRight();
		dwInterp = readNumericBits(prop.numberOfBits());
		fVal = (float) dwInterp / ((1 << prop.numberOfBits()) - 1);
		fVal = prop.lowValue() + (prop.highValue() - prop.lowValue()) * fVal;

		return fVal;

	}

	
	public String decodeString(SendTableProperty prop) {
		return new String(readBits(readNumericBits(9) * 8));
	}
	
	
	
	public Pair<Boolean, Float> decodeSpecialFloat(SendTableProperty prop, float result) {
		
		if ((prop.rawFlags() & SendPropertyFlags.Coord()) == SendPropertyFlags
				.Coord()) {
			result = ReadBitCoord();
			return Pair.of(true, result);
		} else if ((prop.rawFlags() & SendPropertyFlags.CoordMp()) == SendPropertyFlags
				.CoordMp()) {
			result = ReadBitCoordMP(false, false);
			return Pair.of(true, result);
		} else if ((prop.rawFlags() & SendPropertyFlags.CoordMpLowPrecision()) == SendPropertyFlags
				.CoordMpLowPrecision()) {
			result = ReadBitCoordMP(false, true);
			return Pair.of(true, result);
		} else if ((prop.rawFlags() & SendPropertyFlags.CoordMpIntegral()) == SendPropertyFlags
				.CoordMpIntegral()) {
			result = ReadBitCoordMP(true, false);
			return Pair.of(true, result);
		} else if ((prop.rawFlags() & SendPropertyFlags.NoScale()) == SendPropertyFlags
				.NoScale()) {
			result = Float.intBitsToFloat(readNumericBits(32));
			return Pair.of(true, result);
		} else if ((prop.rawFlags() & SendPropertyFlags.Normal()) == SendPropertyFlags
				.Normal()) {
			result = ReadBitNormal();
			return Pair.of(true, result);
		} else if ((prop.rawFlags() & SendPropertyFlags.CellCoord()) == SendPropertyFlags
				.CellCoord()) {
			result = ReadBitCellCoord(prop.numberOfBits(), false, false);
			return Pair.of(true, result);
		} else if ((prop.rawFlags() & SendPropertyFlags.CellCoordLowPrecision()) == SendPropertyFlags
				.CellCoordLowPrecision()) {
			result = ReadBitCellCoord(prop.numberOfBits(), true, false);
			return Pair.of(true, result);
		} else if ((prop.rawFlags() & SendPropertyFlags.CellCoordIntegral()) == SendPropertyFlags
				.CellCoordIntegral()) {
			result = ReadBitCellCoord(prop.numberOfBits(), false, true);
			return Pair.of(true, result);
		}
		return Pair.of(false, new Float(0));

	}

	int COORD_FRACTIONAL_BITS = 5;
	int COORD_DENOMINATOR = (1 << (COORD_FRACTIONAL_BITS));
	float COORD_RESOLUTION = (1.0f / (COORD_DENOMINATOR));

	int COORD_FRACTIONAL_BITS_MP_LOWPRECISION = 3;
	float COORD_DENOMINATOR_LOWPRECISION = (1 << (COORD_FRACTIONAL_BITS_MP_LOWPRECISION));
	float COORD_RESOLUTION_LOWPRECISION = (1.0f / (COORD_DENOMINATOR_LOWPRECISION));

	float ReadBitCoord() {
		int intVal, fractVal;
		float value = 0;

		boolean isNegative = false;

		// Read the required integer and fraction flags
		intVal = readNumericBits(1);
		fractVal = readNumericBits(1);

		// If we got either parse them, otherwise it's a zero.
		if ((intVal | fractVal) != 0) {
			// Read the sign bit
			isNegative = readBit();

			// If there's an integer, read it in
			if (intVal == 1) {
				// Adjust the integers from [0..MAX_COORD_VALUE-1] to
				// [1..MAX_COORD_VALUE]
				intVal = readNumericBits(14) + 1; // 14 --> Coord int bits
			}

			// If there's a fraction, read it in
			if (fractVal == 1) {
				fractVal = readNumericBits(COORD_FRACTIONAL_BITS);
			}

			value = intVal + ((float) fractVal * COORD_RESOLUTION);

		}

		if (isNegative)
			value *= -1;

		return value;
	}

	float ReadBitCellCoord(int bits, boolean lowPrecision, boolean integral) {
		int intval = 0, fractval = 0;
		float value = 0.0f;

		if (integral) {
			value = readNumericBits(bits);
		} else {
			intval = readNumericBits(bits);
			fractval = readNumericBits(lowPrecision ? COORD_FRACTIONAL_BITS_MP_LOWPRECISION
					: COORD_FRACTIONAL_BITS);

			value = intval
					+ ((float) fractval * (lowPrecision ? COORD_RESOLUTION_LOWPRECISION
							: COORD_RESOLUTION));
		}

		return value;

	}

	
//	String readDataTableString()  {
//		    List<Byte> result = new ArrayList<Byte>();
//
//		      for (int pos = 0; pos < 32000; pos++) {
//
//		    	  byte b = readBits(8)[0];
//		        if ((b == 0) || (b == 10)) break;
//		        result.add(new Byte(b));
//		      }
//
//		    byte[] bytes =  result.toArray(new Byte[result.size()]);
//
//		    new String(bytes, "ASCII");
//
//		  }
	
	float ReadBitCoordMP(boolean isIntegral, boolean isLowPrecision) {
		int intval = 0, fractval = 0;
		float value = 0.0f;
		boolean isNegative = false;

		boolean inBounds = readBit();

		if (isIntegral) {
			// Read the required integer and fraction flags
			intval = readBit() ? 1 : 0;

			// If we got either parse them, otherwise it's a zero.
			if (intval == 1) {
				// Read the sign bit
				isNegative = readBit();

				// If there's an integer, read it in
				// Adjust the integers from [0..MAX_COORD_VALUE-1] to
				// [1..MAX_COORD_VALUE]
				if (inBounds) {
					value = (float) (readNumericBits(11) + 1);
				} else {
					value = (float) (readNumericBits(14) + 1);
				}
			}
		} else {
			// Read the required integer and fraction flags
			intval = readBit() ? 1 : 0;

			// Read the sign bit
			isNegative = readBit();

			// If we got either parse them, otherwise it's a zero.
			if (intval == 1) {

				// If there's an integer, read it in
				// Adjust the integers from [0..MAX_COORD_VALUE-1] to
				// [1..MAX_COORD_VALUE]
				if (inBounds) {
					value = (float) (readNumericBits(11) + 1);
				} else {
					value = (float) (readNumericBits(14) + 1);
				}
			}

			// If there's a fraction, read it in
			fractval = readNumericBits(isLowPrecision ? 3 : 5);

			// Calculate the correct floating point value
			value = intval
					+ ((float) fractval * (isLowPrecision ? COORD_RESOLUTION_LOWPRECISION
							: COORD_RESOLUTION));
		}

		if (isNegative)
			value = -value;

		return value;
	}

	int NORMAL_FRACTIONAL_BITS = 11;
	int NORMAL_DENOMINATOR = ((1 << (NORMAL_FRACTIONAL_BITS)) - 1);
	float NORMAL_RESOLUTION = (1.0f / (NORMAL_DENOMINATOR));

	float ReadBitNormal() {
		boolean isNegative = readBit();

		long fractVal = readNumericBits(NORMAL_FRACTIONAL_BITS);

		float value = (float) fractVal * NORMAL_RESOLUTION;

		if (isNegative)
			value *= -1;

		return value;
	}
	
	
	
	

}
