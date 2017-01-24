package de.quaddy_services.proxy.events;

import java.util.EventListener;
import java.util.List;

/**
 *
 */
public interface LogEventListener extends EventListener{

	/**
	 *
	 */
	void updatedLog(List<String> aLog);

}
