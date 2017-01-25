package de.quaddy_services.proxy;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.quaddy_services.proxy.events.CheckPortListener;

/**
 *
 */
public class EscapeProxyWorkerAccept extends Thread {

	private static final Logger LOGGER = LoggerFactory.getLogger(EscapeProxyWorkerAccept.class);

	private EscapeProxyConfig config;

	private ServerSocket serverSocket;

	/**
	 *
	 */
	public EscapeProxyWorkerAccept(EscapeProxyConfig aEscapeProxyConfig) {
		config = aEscapeProxyConfig;
		addListeners(config);
	}

	/**
	 *
	 */
	private void addListeners(EscapeProxyConfig aConfig) {
		aConfig.addCheckPortListener(createCheckPortListener());
	}

	/**
	 *
	 */
	private CheckPortListener createCheckPortListener() {
		return new CheckPortListener() {
			/**
			 *
			 */
			@Override
			public void checkPort(int aPort) {
				checkForPortUpdate(aPort);
			}
		};
	}

	/**
	 *
	 */
	@Override
	public void run() {
		while (true) {
			Integer tempPort = Integer.valueOf(config.getLocalPort());
			if (tempPort == 0) {
				LOGGER.info("No port, waiting...");
				synchronized (this) {
					serverSocket = null;
					try {
						LOGGER.info("wait for port set");
						wait();
						LOGGER.info("continue");
						continue;
					} catch (InterruptedException e) {
						LOGGER.info("Error", e);
						return;
					}
				}
			} else {
				try {
					LOGGER.info("Create server Socket for " + tempPort);
					serverSocket = new ServerSocket(tempPort);
				} catch (IOException e) {
					LOGGER.info("Error opening port " + tempPort + e);
					LOGGER.debug("Details", e);
					config.firePortStatus(false, "Error opening port " + tempPort + ": " + e);
					synchronized (this) {
						try {
							LOGGER.info("wait for port change");
							wait(60000);
							LOGGER.info("continue");
							continue;
						} catch (InterruptedException e2) {
							LOGGER.info("Error", e2);
							return;
						}
					}
				}
				acceptConnections(serverSocket);
			}
		}

	}

	/**
	 *
	 */
	private void acceptConnections(ServerSocket aServerSocket) {
		config.firePortStatus(true, "Listening on " + aServerSocket);
		while (true) {
			try {
				Socket tempSocket = aServerSocket.accept();

				InetAddress tempInetAddress = tempSocket.getInetAddress();
				String tempMsg = "Got connection from " + tempInetAddress;
				LOGGER.info(tempMsg);

				if (isLocalhost(tempInetAddress)) {
					EscapeProxyWorkerSocket tempEscapeProxyWorkerSocket = new EscapeProxyWorkerSocket(config, tempSocket);
					tempEscapeProxyWorkerSocket.start();
				} else {
					config.fireLogEvent(tempInetAddress+" not localhost, closing " + tempSocket);
					tempSocket.close();
				}
			} catch (IOException | RuntimeException e) {
				LOGGER.error("Socket " + serverSocket + " finished " + e);
				LOGGER.debug("Details", e);
				return;
			}
		}
	}

	private Set<String> isLocalhostCache = new HashSet<>();
	/**
	 *
	 */
	private boolean isLocalhost(InetAddress aInetAddress) {
		String tempHostAddress = aInetAddress.getHostAddress();
		if (isLocalhostCache.contains(tempHostAddress)) {
			return true;
		}
		boolean tempLocalhostDetect = isLocalhostDetect(aInetAddress);
		if (tempLocalhostDetect) {
			isLocalhostCache.add(tempHostAddress);
		}
		return tempLocalhostDetect;
	}

	/**
	 *
	 */
	private boolean isLocalhostDetect(InetAddress aInetAddress) {
		Enumeration<NetworkInterface> tempNetworkInterfaces;
		try {
			tempNetworkInterfaces = NetworkInterface.getNetworkInterfaces();
		} catch (SocketException e) {
			LOGGER.error("Error, assuming localhost", e);
			return true;
		}
		if (tempNetworkInterfaces != null) {
			while (tempNetworkInterfaces.hasMoreElements()) {
				NetworkInterface tempNetworkInterface = tempNetworkInterfaces.nextElement();
				Enumeration<InetAddress> tempInetAddresses = tempNetworkInterface.getInetAddresses();
				while (tempInetAddresses.hasMoreElements()) {
					InetAddress tempInetAddress = tempInetAddresses.nextElement();
					if (tempInetAddress.equals(aInetAddress)) {
						LOGGER.debug("Match {} on {}", tempInetAddress, tempNetworkInterface);
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 *
	 */
	private void checkForPortUpdate(int aPort) {
		synchronized (this) {
			if (serverSocket == null) {
				LOGGER.info("Start on port " + aPort);
				notifyAll();
			} else {
				if (serverSocket.getLocalPort() != aPort) {
					LOGGER.info("Detected Port change " + aPort);
					try {
						serverSocket.close();
					} catch (IOException e) {
						LOGGER.error("Could not close Socket " + serverSocket, e);
					}
					serverSocket = null;
					notifyAll();
				}
			}
		}
	}
}
