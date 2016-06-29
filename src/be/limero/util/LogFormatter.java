package be.limero.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

public final class LogFormatter extends Formatter {

	private static final String LINE_SEPARATOR = System.getProperty("line.separator");

	class MyHandler extends StreamHandler {

	}

	@Override
	public String format(LogRecord record) {
		String src = record.getSourceClassName();
		if ( src==null ) src="";
		src = src.substring(src.lastIndexOf('.') + 1);
		StringBuilder sb = new StringBuilder();
		sb.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS ").format(new Date(record.getMillis())))

				// sb.append(new Date(record.getMillis()))
				.append("|").append(record.getLevel().getLocalizedName().substring(0, 4)).append("|")
				.append(record.getThreadID())
				.append(String.format("%30.30s", src + "." + record.getSourceMethodName())).append(" | ")
				.append(formatMessage(record)).append(LINE_SEPARATOR);

		if (record.getThrown() != null) {
			try {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				record.getThrown().printStackTrace(pw);
				pw.close();
				sb.append(sw.toString());
			} catch (Exception ex) {
				// ignore
			}
		}
		// System.out.println(sb.toString());
		// System.out.println(sb.toString());

		return sb.toString();
	}

	public static void Init() {
		// Handler[] handlers =
		// LogManager.getLogManager().getLogger("global").getHandlers();
		// LogManager.getLogManager().getLoggerNames()
		Enumeration<String> si = LogManager.getLogManager().getLoggerNames();
		while (si.hasMoreElements()) {
			String name = si.nextElement();
//			System.out.println("+" + name + "+");
			Handler[] handlers = LogManager.getLogManager().getLogger(name).getHandlers();
			for (Handler handler : handlers) {
				LogManager.getLogManager().getLogger(name).removeHandler(handler);
//				handler.setFormatter(new LogFormatter());
			}
		}

		LogManager.getLogManager().getLogger("").setUseParentHandlers(false);
		StreamHandler sh = new StreamHandler(System.out, new LogFormatter());
		LogManager.getLogManager().getLogger("").addHandler(sh);

	}

}
