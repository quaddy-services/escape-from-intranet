package de.quaddy_services.proxy;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

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

				String tempMsg = "Got connection from " + tempSocket.getInetAddress();
				LOGGER.info(tempMsg);

				EscapeProxyWorkerSocket tempEscapeProxyWorkerSocket = new EscapeProxyWorkerSocket(config, tempSocket);
				tempEscapeProxyWorkerSocket.start();
			} catch (IOException | RuntimeException e) {
				LOGGER.error("Socket " + serverSocket + " finished " + e);
				LOGGER.debug("Details", e);
				return;
			}
		}
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
