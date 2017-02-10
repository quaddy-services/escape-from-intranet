package de.quaddy_services.proxy.logging;

/**
 *
 */
public class Logger {

	private String prefix;
	private FileLogger fileLogger;
	private Boolean debugEnabled;
	private Boolean traceEnabled;

	/**
	 *
	 */
	public Logger(String aPrefix, FileLogger aFile) {
		prefix = aPrefix;
		fileLogger = aFile;
	}

	/**
	 *
	 */
	public void info(String aMessage) {
		info(aMessage, (Throwable) null);
	}

	/**
	 *
	 */
	public void error(String aMessage, Throwable aE) {
		fileLogger.log(prefix, "error", aMessage, aE);
	}

	/**
	 *
	 */
	public void debug(String aMessage, Throwable aE) {
		if (isDebugEnabled()) {
			fileLogger.log(prefix, "debug", aMessage, aE);
		}
	}

	/**
	 *
	 */
	public void debug(String aString, Object anObject) {
		if (isDebugEnabled()) {
			debug(aString.replaceFirst("\\{\\}", String.valueOf(anObject)));
		}
	}

	/**
	 *
	 */
	public boolean isDebugEnabled() {
		if (debugEnabled == null) {
			if (isTraceEnabled()) {
				debugEnabled = true;
			} else {
				String tempDefaultLevel = System.getProperty("defaultLogLevel");
				debugEnabled = "debug".equalsIgnoreCase(tempDefaultLevel);
			}
		}
		return debugEnabled;
	}

	public boolean isTraceEnabled() {
		if (traceEnabled == null) {
			String tempDefaultLevel = System.getProperty("defaultLogLevel");
			traceEnabled = "trace".equalsIgnoreCase(tempDefaultLevel);
		}
		return traceEnabled;
	}

	/**
	 *
	 */
	public void debug(String aString) {
		debug(aString, (Throwable) null);
	}

	/**
	 *
	 */
	public void info(String aMessage, Throwable aE) {
		fileLogger.log(prefix, "info", aMessage, aE);

	}

	/**
	 *
	 */
	public void error(String aString) {
		error(aString, (Throwable) null);
	}

	/**
	 *
	 */
	public void debug(String aString, Object aO1, Object aO2) {
		debug(aString.replaceFirst("\\{\\}", String.valueOf(aO1)).replaceFirst("\\{\\}", String.valueOf(aO2)));
	}

	/**
	 *
	 */
	public void trace(String aString) {
		if (isTraceEnabled()) {
			fileLogger.log(prefix, "trace", aString, (Throwable) null);
		}
	}

}
