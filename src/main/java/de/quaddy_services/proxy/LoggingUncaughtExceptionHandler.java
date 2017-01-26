package de.quaddy_services.proxy;

import java.lang.Thread.UncaughtExceptionHandler;

import de.quaddy_services.proxy.logging.Logger;
import de.quaddy_services.proxy.logging.LoggerFactory;

/**
 *
 */
public class LoggingUncaughtExceptionHandler implements UncaughtExceptionHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(LoggingUncaughtExceptionHandler.class);

	/**
	 *
	 */
	@Override
	public void uncaughtException(Thread aT, Throwable aE) {
		LOGGER.error("Error in " + aT, aE);
	}

}
