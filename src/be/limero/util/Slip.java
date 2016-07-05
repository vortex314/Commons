package be.limero.util;

import java.util.logging.Logger;

public class Slip extends Bytes {
	private static final Logger log = Logger.getLogger(Slip.class.getName());
	final static byte END = (byte) 0xC0;
	final static byte ESC = (byte) 0xDB;
	final static byte ESC_END = (byte) 0xDC;
	final static byte ESC_ESC = (byte) 0xDD;
	boolean escaped;

	public Slip(int size) {
		super(size);
		escaped = false;
	}

	public void reset() {
		clear();
		escaped = false;
	}

	public boolean fill(byte b) {
		if (b == END) {
			if (length() > 0) {
				return true;
			}
		} else if (b == ESC) {
			escaped = true;
		} else if (b == ESC_ESC && escaped) {
			write(ESC);
			escaped = false;
		} else if (b == ESC_END && escaped) {
			write(END);
			escaped = false;
		} else {
			write(b);
		}
		return false;
	}
	

	public static Bytes encode(Bytes bytes) {
		bytes.offset(0);
		Bytes sf = new Bytes(bytes.length() + 100);
		sf.write(END);
		while (bytes.hasData()) {
			byte b = bytes.read();
			if (b == END) {
				sf.write(ESC);
				sf.write(ESC_END);
			} else if (b == ESC) {
				sf.write(ESC);
				sf.write(ESC_ESC);
			} else {
				sf.write(b);
			}
		}
		sf.write(END);
		return sf;

	}

	public static Bytes decode(Bytes sf) {
		Bytes bytes = new Bytes(sf.length());
		sf.offset(0);
		while (sf.hasData()) {
			byte b = sf.read();
			if (b == ESC) {
				bytes.write(sf.read() ^ 0x20);
			} else {
				bytes.write(b);
			}
		}
		return bytes;
	}

	public static Bytes addCrc(Bytes bytes) {
		byte[] crc = Fletcher16(bytes);
		Bytes result=new Bytes(bytes.length()+2);
		result.write(crc[0]);
		result.write(crc[1]);
		return result;
	}
	
	public static void removeCrc(Bytes bytes){
		if ( bytes.length()>2) {
			bytes.used(bytes.used()-2);
		}
	}

	public static byte[] Fletcher16(Bytes bytes) {
		short sum1 = 0;
		short sum2 = 0;
		bytes.offset(0);
		for (int i = 0; i < bytes.length(); i++) {
			int b = bytes.read();
			if (b < 0)
				b = 256 + b;
			sum1 = (short) ((sum1 + b) % 255);
			sum2 = (short) ((sum2 + sum1) % 255);
//			System.out.printf(" %X : %X , %X \n", b,sum1, sum2);
		}
		return new byte[] { (byte) sum2, (byte) sum1 };
	}

	// _________________________________________________________________________

	public static boolean isGoodCrc(Bytes bytes) // PUBLIC
	// _________________________________________________________________________
	{
		Bytes sub = bytes.sub(0, bytes.length() - 2);
		byte[] crc = Fletcher16(sub);
		if (bytes.peek(bytes.length() - 2) == crc[1])
			if (bytes.peek(bytes.length() - 1) == crc[0])
				return true;
		return false;
	}

	public static void main(String[] args) {
		LogHandler.buildLogger();

		Bytes bytes = new Bytes(100);
		bytes.write(new byte[] { 1, 2, 3, 4, 5, END, ESC });
		log.info(bytes.toString());
		bytes = Slip.encode(bytes);
		log.info(bytes.toString());
		bytes = Slip.decode(bytes);
		log.info(bytes.toString());

	}

}
