package de.quaddy_services.proxy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class EscapeProxyWorkerSocket extends Thread {

	private static final Logger LOGGER = LoggerFactory.getLogger(EscapeProxyWorkerSocket.class);

	private EscapeProxyConfig config;
	private Socket socket;

	/**
	 *
	 */
	public EscapeProxyWorkerSocket(EscapeProxyConfig aConfig, Socket aSocket) {
		config = aConfig;
		socket = aSocket;
	}

	/**
	 *
	 */
	@Override
	public void run() {
		LOGGER.debug("Started with {}", socket);
		List<String> tempHeaders;
		try {
			tempHeaders = readHeaders(socket);
		} catch (IOException e) {
			LOGGER.error("Error", e);
			try {
				OutputStreamWriter outputStreamWriter = new OutputStreamWriter(socket.getOutputStream(), "UTF-8");
				outputStreamWriter.write("HTTP/1.0 502 Error " + e.getMessage() + " \r\n");
				outputStreamWriter.write("Proxy-agent: escape-from-intranet\r\n");
				outputStreamWriter.write("\r\n");
				outputStreamWriter.flush();
			} catch (IOException e2) {
				LOGGER.error("Error sending response to " + socket, e2);
			}
			return;
		}

		if (tempHeaders.size() == 0) {
			LOGGER.error("Missing header");
			try {
				OutputStreamWriter outputStreamWriter = new OutputStreamWriter(socket.getOutputStream(), "UTF-8");
				outputStreamWriter.write("HTTP/1.0 502 Header messing\r\n");
				outputStreamWriter.write("Proxy-agent: escape-from-intranet\r\n");
				outputStreamWriter.write("\r\n");
				outputStreamWriter.flush();
			} catch (IOException e2) {
				LOGGER.error("Error sending response to " + socket, e2);
			}
			return;
		}

		Socket tempTargetSocket;
		try {
			tempTargetSocket = new Socket(config.getProxyHost(), Integer.valueOf(config.getProxyPort()));
			OutputStream tempProxyOut = tempTargetSocket.getOutputStream();
			boolean tempProxyAuthAlreadySent = false;
			for (String tempHeader : tempHeaders) {
				if (tempHeader.toLowerCase().startsWith("proxy-authorization:")) {
					LOGGER.debug("Take Proxy auth from original request {}", tempHeader);
					tempProxyAuthAlreadySent = true;
				}
				LOGGER.debug("Forward {}", tempHeader);
				tempProxyOut.write((tempHeader + "\r\n").getBytes());
			}
			if (!tempProxyAuthAlreadySent) {
				String tempAuth = new String(Base64.getEncoder().encode((config.getProxyUser() + ":" + config.getProxyPassword()).getBytes()));
				String tempProxyHeaderAuth = "Proxy-Authorization: Basic " + tempAuth;
				LOGGER.debug("Add {}", tempProxyHeaderAuth);
				tempProxyOut.write((tempProxyHeaderAuth + "\r\n").getBytes());
			}
			tempProxyOut.write(("\r\n").getBytes());
			tempProxyOut.flush();
		} catch (IOException | RuntimeException e) {
			LOGGER.error("Error connecting to proxy via " + config.getProxyHost() + " " + config.getProxyPort(), e);
			try {
				OutputStreamWriter outputStreamWriter = new OutputStreamWriter(socket.getOutputStream(), "UTF-8");
				String tempMessage = e.getMessage();
				// Maybe: Unable to tunnel through proxy. Proxy returns "HTTP/1.0 407 Proxy Authentication Required"
				int tempHttpPos = tempMessage.indexOf("HTTP/");
				if (tempHttpPos >= 0) {
					// Reply with original Message
					outputStreamWriter.write(tempMessage.substring(tempHttpPos) + "\r\n");
				} else {
					outputStreamWriter.write("HTTP/1.0" + " 404 " + tempMessage + "\r\n");
				}
				outputStreamWriter.write("Proxy-agent: escape-from-intranet\r\n");
				outputStreamWriter.write("\r\n");
				outputStreamWriter.flush();
			} catch (IOException e2) {
				LOGGER.error("Error sending response to " + socket, e2);
			}
			return;
		}

		new Thread() {
			@Override
			public void run() {
				forwardData(tempTargetSocket, socket);
			}
		}.start();
		new Thread() {
			@Override
			public void run() {
				forwardData(socket, tempTargetSocket);
			}
		}.start();
	}

	/**
	 *
	 */
	private List<String> readHeaders(Socket aSocket) throws IOException {
		List<String> tempLines = new ArrayList<String>();
		InputStream tempInputStream = aSocket.getInputStream();
		int tempByte = tempInputStream.read();
		int tempCrLfCount = 0;
		ByteArrayOutputStream tempOut = new ByteArrayOutputStream();
		while (tempByte != -1) {
			if (tempByte == '\r' || tempByte == '\n') {
				byte[] tempByteArray = tempOut.toByteArray();
				if (tempByteArray.length > 0) {
					String tempLine = new String(tempByteArray, "UTF-8");
					LOGGER.debug("Received:" + tempLine);
					tempLines.add(tempLine);
					tempOut.reset();
				}
				tempCrLfCount++;
				if (tempCrLfCount == 4) {
					break;
				}
			} else {
				tempCrLfCount = 0;
				tempOut.write(tempByte);
			}
			tempByte = tempInputStream.read();
		}
		return tempLines;
	}

	/**
	 * http://stackoverflow.com/questions/9357585/creating-a-java-proxy-server-that-accepts-https
	 */
	private void forwardData(Socket inputSocket, Socket outputSocket) {
		Thread tempCurrentThread = Thread.currentThread();
		String tempName = inputSocket.getInetAddress().getHostName() + ">" + outputSocket.getInetAddress().getHostName() + "-" + tempCurrentThread.getName();
		tempCurrentThread.setName(tempName);
		LOGGER.info("Start forwarding...");
		try {
			InputStream inputStream = inputSocket.getInputStream();
			try {
				OutputStream outputStream = outputSocket.getOutputStream();
				try {
					byte[] buffer = new byte[4096];
					int read;
					do {
						read = inputStream.read(buffer);
						if (read > 0) {
							if (LOGGER.isDebugEnabled()) {
								LOGGER.debug("Write to " + outputSocket + ":" + read + " bytes");
								if (LOGGER.isTraceEnabled()) {
									LOGGER.trace(new String(buffer, 0, read));
								}
							}
							outputStream.write(buffer, 0, read);
							if (inputStream.available() < 1) {
								outputStream.flush();
							}
						}
					} while (read >= 0);
				} finally {
					if (!outputSocket.isOutputShutdown()) {
						outputSocket.shutdownOutput();
					}
				}
			} finally {
				if (!inputSocket.isInputShutdown()) {
					inputSocket.shutdownInput();
				}
			}
		} catch (IOException e) {
			LOGGER.info("Closed"+ e);
			LOGGER.debug("Error", e);
		}
	}
}
