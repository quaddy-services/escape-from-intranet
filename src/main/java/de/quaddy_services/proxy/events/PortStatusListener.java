package de.quaddy_services.proxy.events;

import java.util.EventListener;

/**
 *
 */
public interface PortStatusListener extends EventListener{

	/**
	 *
	 */
	void statusChanged(boolean aOkFlag, String aText);

}
