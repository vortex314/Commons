package be.limero.util;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

public class Util {
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

	public static String bytesToHex(byte[] bytes) {
		StringBuilder hexChars = new StringBuilder();
		for (int j = 0; j < bytes.length; j++) {
			if ( j!= 0 ) hexChars.append(' ');
			int v = bytes[j] & 0xFF;
			hexChars.append(hexArray[v >>> 4]);
			hexChars.append(hexArray[v & 0x0F]);
		}
		return hexChars.toString();
	}
	
	public static String bytesToHex(byte[] bytes,int limit) {
		StringBuilder hexChars = new StringBuilder();
		for (int j = 0; j < limit; j++) {
			if ( j!= 0 ) hexChars.append(' ');
			int v = bytes[j] & 0xFF;
			hexChars.append(hexArray[v >>> 4]);
			hexChars.append(hexArray[v & 0x0F]);
		}
		return hexChars.toString();
	}
	
	static public Logger buildLogger() {
		LogFormatter formatter = new LogFormatter();
		Logger log = Logger.getLogger(""); // TODO update when JDK
											// updatesLogger.getGlobal() is what
											// we would like to use here
		log.setLevel(Level.INFO);
		log.setUseParentHandlers(false); // Turn off any Parent Handlers
		Handler[] handlers = log.getHandlers();
		for (Handler handler : handlers) {
			log.removeHandler(handler);
		}
//		log.addHandler(buildsoh(formatter)); // Add stdOut Handler
		log.addHandler(buildseh(formatter)); // Add stdErr Handler
		return log;
	}

	static public StreamHandler buildseh(Formatter formatter) {

		final StreamHandler seh = new StreamHandler(System.out, formatter) {
			@Override
			public synchronized void publish(final LogRecord record) {
				super.publish(record);
				flush();
			}
		};
		seh.setLevel(Level.ALL); // Default StdErr Setting
		return seh;
	}

}
