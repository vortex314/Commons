package be.limero.util;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

public class LogHandler {

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

	static public StreamHandler buildsoh(Formatter formatter) {
		StreamHandler soh = new StreamHandler(System.out, formatter);
		soh.setLevel(Level.ALL); // Default StdOut Setting
		return soh;
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
