package be.limero.util;
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.util.logging.Logger;

/**
 *
 * @author lieven2
 */
public class Cbor extends Bytes {
	private static final Logger log = Logger.getLogger(Cbor.class.getName());

	public class Token {

		CborType type;
		long _length;
		double _double;
		float _float;
		boolean _bool;
		long _long;
		String str;
		Bytes bytes;
	}

	static int byteToInt(byte b) {
		return b & 0xFF;
	}

	static byte intToByte(int i) {
		return (byte) i;
	}

	public interface Listener {

		void onToken(Token t);
	}

	public static enum CborType {
		C_PINT, C_NINT, C_BYTES, C_STRING, C_ARRAY, C_MAP, C_TAG, C_SPECIAL, C_BOOL, C_FLOAT, C_DOUBLE, C_BREAK, C_NILL, C_ERROR
	};

	public Cbor(int size) {
		super(size);
	}

	public Cbor(byte[] src) {
		super(src);
		offset(0);
	}

	public Cbor addf(String format, Object... objects) {
		for (int i = 0; i < format.length(); i++) {
			if (objects[i] == null)
				addNull();
			else {
				switch (format.charAt(i)) {
				case 'i': {
					add((Integer) objects[i]);
					break;
				}
				case 'u': {
					add((Integer) objects[i]);
					break;
				}
				case 'B': {
					add((Bytes) objects[i]);
					break;
				}
				case 'S': {
					add((String) objects[i]);
					break;
				}
				case 'l': {
					add((Long) objects[i]);
					break;
				}
				case 'b': {
					add((Boolean) objects[i]);
					break;
				}
				case 'f': {
					add((Float) objects[i]);
					break;
				}
				case 'd': {
					add((Double) objects[i]);
					break;
				}
				}
			}
		}
		return this;
	}

	// <type:5><minor:3>[<length:0-64>][<data:0-n]
	// if minor<24 => length=0
	static int tokenSize[] = { 1, 2, 4, 8 };

	public Token readToken() {
		Token token = new Token();
		int minor;

		if (!hasData()) {
			token.type = CborType.C_ERROR;
			return token;
		}

		byte hdr = read();
		int major = (hdr & 0xFF) >> 5;
		token.type = CborType.values()[major];
		minor = hdr & 0x1F;
		if (minor < 24) {
			token._length = minor;
			token._long = minor;
		} else if (minor < 28) {
			token._length = tokenSize[minor - 24];
			if (token._length <= 4) {
				token._long = getInt((int) token._length);
				token._length = token._long;
			} else {
				token._length = getLong((int) token._length);
			}
		} else if (minor < 31) {
			token.type = CborType.C_ERROR;
			return token;
		} else {
			token._length = Long.MAX_VALUE; // suppoze very big length will be
											// stopped by BREAK, side effect
											// limited arrays and maps can also
											// be breaked
		}
		if (token.type == CborType.C_STRING) {
			token.str = new String();
			for (int i = 0; i < token._length; i++) {
				token.str += (char) read();
			}
		} else if (token.type == CborType.C_BYTES) {
			token.bytes = new Bytes((int) token._length);
			for (int i = 0; i < token._length; i++) {
				token.bytes.write((char) read());
			}
		} else if (token.type == CborType.C_SPECIAL) {
			switch (minor) {
			case 21: // TRUE
			{
				token.type = CborType.C_BOOL;
				token._bool = true;
				break;
			}
			case 20: // FALSE
			{
				token.type = CborType.C_BOOL;
				token._bool = false;
				break;
			}
			case 22: // NILL
			{
				token.type = CborType.C_NILL;
				break;
			}
			case 26: // FLOAT32
			{
				token.type = CborType.C_FLOAT;
				token._float = Float.intBitsToFloat((int) token._long);
				break;
			}
			case 27: // FLOAT64
			{
				token.type = CborType.C_DOUBLE;
				token._double = Double.longBitsToDouble(token._long);
				break;
			}

			case 31: // BREAK
			{
				token.type = CborType.C_BREAK;
				break;
			}
			}
		}
		return token;
	}

	protected void skipToken() {
		Token token = readToken();
		if (token.type == CborType.C_STRING || token.type == CborType.C_BYTES) {
			offset(offset() + (int) token._length);
		}
	}

	int getInt(int length) {
		int l = 0;
		while (length > 0) {
			l <<= 8;
			l += read() & 0xFF; // important & OxFF
			length--;
		}
		return l;
	}

	long getLong(int length) {
		long l = 0;
		while (length > 0) {
			l <<= 8;
			l += read() & 0xFF;
			length--;
		}
		return l;
	}

	CborType tokenToString(StringBuilder str) {
		Token token;
		token = readToken();
		if (token.type == CborType.C_ERROR) {
			return CborType.C_ERROR;
		}
		switch (token.type) {
		case C_PINT: {
			str.append(token._long);
			return CborType.C_PINT;
		}
		case C_NINT: {
			long v = -token._long;
			str.append(v);
			return CborType.C_NINT;
		}
		case C_BYTES: {
			str.append("0x");
			str.append(token.bytes.toString());
			return CborType.C_BYTES;
		}
		case C_STRING: {
			str.append("\"");
			str.append(token.str);
			str.append("\"");
			return CborType.C_STRING;
		}
		case C_MAP: {
			long count = token._length;
			str.append("{");
			for (int i = 0; i < count; i++) {
				if (i != 0) {
					str.append(",");
				}
				CborType rc = tokenToString(str);
				if (rc == CborType.C_ERROR) {
					str.append("_PARSING ERROR_");
					return CborType.C_ERROR;
				} else if (rc == CborType.C_BREAK) {
					break;
				}
				str.append(":");
				tokenToString(str);
			}
			str.append("}");
			return CborType.C_MAP;
		}
		case C_ARRAY: {
			long count = token._length;
			str.append("[");
			for (int i = 0; i < count; i++) {
				if (i != 0) {
					str.append(",");
				}
				CborType rc = tokenToString(str);
				if (rc == CborType.C_ERROR) {
					str.append("_PARSING ERROR_");
					return CborType.C_ERROR;
				} else if (rc == CborType.C_BREAK) {
					break;
				}
			}
			str.append("]");
			return CborType.C_ARRAY;
		}
		case C_TAG: {
			long count = token._length;
			str.append("(");
			str.append(count);
			str.append(":");
			tokenToString(str);
			str.append(")");
			return CborType.C_TAG;
		}
		case C_BOOL: {
			str.append(token._bool);
			return CborType.C_BOOL;
		}
		case C_NILL: {
			str.append("null");
			return CborType.C_NILL;
		}
		case C_FLOAT: {
			str.append(token._float);
			return CborType.C_FLOAT;
		}
		case C_DOUBLE: {
			str.append(token._double);
			return CborType.C_FLOAT;
		}
		case C_BREAK: {
			return CborType.C_BREAK;
		}
		case C_SPECIAL: {
			return CborType.C_ERROR;
		}
		default: // avoid warnings about additional types > 7
		{
			return CborType.C_ERROR;
		}
		}

	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		CborType ct;
		offset(0);
		while (hasData()) {
			ct = tokenToString(str);
			if (ct == CborType.C_BREAK || ct == CborType.C_ERROR) {
				return str.toString();
			}
			if (hasData()) {
				str.append(",");
			}
		}
		return str.toString();
	}

	public Bytes getBytes() {
		Token token = readToken();
		if (token.type == CborType.C_BYTES || token.type == CborType.C_STRING) {
			return token.bytes;
		}
		return null;
	}

	public String getString() {
		Token token = readToken();
		if (token.type == CborType.C_STRING) {
			return token.str;
		}
		return null;
	}

	public Integer getInteger() {
		Token token = readToken();
		if (token.type == CborType.C_PINT || token.type == CborType.C_NINT) {
			return (int) token._long;
		}
		return null;
	}

	CborType parse(Listener listener) {
		Token token;
		token = new Token();
		while (hasData()) {
			token._long = 0;
			token = readToken();
			if (token.type == CborType.C_ERROR) {
				return CborType.C_ERROR;
			}
			switch (token.type) {
			case C_PINT: {
				listener.onToken(token);
				break;
			}
			case C_NINT: {
				listener.onToken(token);
				break;
			}
			case C_BYTES: {
				token.bytes = new Bytes((int) token._length);
				for (int i = 0; i < token._length; i++) {
					token.bytes.write(read());
				}
				listener.onToken(token);
				break;
			}
			case C_STRING: {
				token.str = new String();
				for (int i = 0; i < token._length; i++) {
					token.str += read();
				}
				listener.onToken(token);
				break;
			}
			case C_MAP: {
				listener.onToken(token);
				int count = (int) token._length;
				for (int i = 0; i < count; i++) {
					parse(listener);
					if (parse(listener) == CborType.C_BREAK) {
						break;
					}
					parse(listener);
				}
				break;
			}
			case C_ARRAY: {
				listener.onToken(token);
				int count = (int) token._length;
				for (int i = 0; i < count; i++) {
					if (parse(listener) == CborType.C_BREAK) {
						break;
					}
				}
				break;
			}
			case C_TAG: {
				listener.onToken(token);
				parse(listener);
				break;
			}
			case C_BOOL: {
				listener.onToken(token);
				break;
			}
			case C_NILL:
			case C_BREAK: {
				listener.onToken(token);
				break;
			}
			case C_FLOAT: {
				listener.onToken(token);
				break;
			}
			case C_DOUBLE: {
				;
				listener.onToken(token);
				break;
			}
			case C_SPECIAL: {
				listener.onToken(token);
			}
			default: // avoid warnings about additional types > 7
			{
				return CborType.C_ERROR;
			}
			}
		}
		;
		return token.type;

	}

	void addHeader(CborType major, long minor) {
		int hdr = 0;
		if (major == CborType.C_FLOAT) {
			major = CborType.C_SPECIAL;
			minor = 26;
		} else if (major == CborType.C_DOUBLE) {
			major = CborType.C_SPECIAL;
			minor = 27;
		}
		;
		hdr = major.ordinal();
		hdr <<= 5;
		hdr += minor;
		write((byte) hdr);
	}

	void addToken(CborType ctype, int length, long value) {
		addHeader(ctype, 0);
		byte buffer[] = new byte[length];
		for (int i = 0; i < length; i++) {
			buffer[i] = (byte) (value & 0xFF);
			value >>= 8;
		}
		for (int i = length - 1; i >= 0; i--) {
			write(buffer[i]);
		}
	}

	public void addToken(CborType ctype, long value) {
		if (value < 24) {
			addHeader(ctype, value);
		} else if (value < 256) {
			addHeader(ctype, 24);
			write(value);
		} else if (value < 65536) {
			addHeader(ctype, 25);
			write(value >> 8);
			write(value);
		} else if (value < 4294967296L) {
			addHeader(ctype, 26);
			write(value >> 24);
			write(value >> 16);
			write(value >> 8);
			write(value);
		} else {
			addHeader(ctype, 27);
			write(value >> 56);
			write(value >> 48);
			write(value >> 40);
			write(value >> 32);
			write(value >> 24);
			write(value >> 16);
			write(value >> 8);
			write(value);
		}
	}

	public Cbor add(Integer i) {
		if (i == null) {
			addNull();
		} else {
			if (i >= 0) {
				addToken(CborType.C_PINT, i);
			} else {
				addToken(CborType.C_NINT, -1 - i);
			}
		}
		return this;
	}

	public Cbor add(float f) {
		int l = Float.floatToIntBits(f);
		addToken(CborType.C_FLOAT, 4, l);
		return this;
	}

	public Cbor add(double d) {
		long l = Double.doubleToLongBits(d);
		addToken(CborType.C_DOUBLE, 8, l);
		return this;
	}

	public Cbor add(Bytes b) {
		if (b == null) {
			addNull();
		} else {
			addToken(CborType.C_BYTES, b.length());
			b.offset(0);
			while (b.hasData()) {
				write(b.read());
			}
		}
		return this;
	}

	public Cbor add(String str) {
		addToken(CborType.C_STRING, str.length());
		for (int i = 0; i < str.length(); i++) {
			write(str.charAt(i));
		}
		return this;
	}

	public Cbor add(long i64) {
		if (i64 >= 0) {
			addToken(CborType.C_PINT, (long) i64);
		} else {
			addToken(CborType.C_NINT, (long) -1 - i64);
		}
		return this;
	}

	public Cbor add(boolean b) {
		if (b) {
			addHeader(CborType.C_SPECIAL, 21);
		} else {
			addHeader(CborType.C_SPECIAL, 20);
		}
		return this;
	}

	public Cbor addMap(int size) {
		if (size < 0) {
			addHeader(CborType.C_MAP, 31);
		} else {
			addToken(CborType.C_MAP, size);
		}
		return this;
	}

	public Cbor addArray(int size) {
		if (size < 0) {
			addHeader(CborType.C_ARRAY, 31);
		} else {
			addToken(CborType.C_ARRAY, size);
		}
		return this;
	}

	public Cbor addTag(int nr) {
		addToken(CborType.C_TAG, nr);
		return this;
	}

	public Cbor addBreak() {
		addHeader(CborType.C_SPECIAL, 31);
		return this;
	}

	public Cbor addNull() {
		addHeader(CborType.C_SPECIAL, 22);
		return this;
	}

	public String toHex() {
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < used; i++) {
			if (i != 0)
				str.append(' ');
			str.append(String.format("%02X", data[i]));
		}
		return str.toString();
	}

	public Cbor addKey(int key) {
		add(key);
		return this;
	}

	public int gotoKey(int field, int index) {
		offset(0);
		int count = 0;
		Integer lastField;
		while (hasData()) {
			lastField = getInteger();
			if (lastField == null)
				break;
			if (lastField == field) {
				count++;
				if (count > index)
					return offset();
			}
			skipToken();
		}
		return offset();
	}

	public int gotoKey(int field) {
		return gotoKey(field, 0);
	}

	public Cbor addField(int field, Object value) {
		addKey(field);
		if (value instanceof String)
			add((String) value);
		if (value instanceof Integer)
			add((Integer) value);
		if (value instanceof Bytes)
			add((Bytes) value);
		return null;
	}

	public static void main(String[] args) {
		Util.buildLogger();
		Cbor cbor = new Cbor(1000);
		cbor.addf("iiSbliddB", 1, 2, "hi------------------------------------------------------ih", true, 1L, null, 1.2,
				(double) 3.0, new Bytes(new byte[] { 1, 2, 3 }));
		log.info(cbor.toString());
		log.info(cbor.toHex());
	}
}
