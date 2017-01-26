package de.quaddy_services.proxy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.quaddy_services.proxy.logging.Logger;
import de.quaddy_services.proxy.logging.LoggerFactory;

/**
 *
 */
public class EscapeProxyWorkerSocket extends Thread {

	private static final Logger LOGGER = LoggerFactory.getLogger(EscapeProxyWorkerSocket.class);

	private static Map<String, ProxyDecision> proxyDecisionCache = new HashMap<>();

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

		Socket tempProxySocket = null;
		Socket tempDirectSocket = null;
		final Socket tempFinalTargetSocket;

		URL tempUrl = getUrlFromHeader(tempHeaders);

		ProxyDecision tempProxyDecision = getProxyDecision(tempUrl);

		if (ProxyDecision.PROXY.equals(tempProxyDecision)) {
			tempProxySocket = getProxySocket(tempHeaders);
			if (tempProxySocket == null) {
				// Try a direct connection
				tempDirectSocket = getDirectSocket(tempHeaders, tempUrl);
				if (tempDirectSocket == null) {
					replyWithBadGateway("Proxy and Direct not responding");
					return;
				} else {
					tempProxyDecision = ProxyDecision.DIRECT;
				}
			} else {
				tempProxyDecision = ProxyDecision.PROXY;
			}
		} else if (ProxyDecision.DIRECT.equals(tempProxyDecision) || tempProxyDecision == null) {
			tempDirectSocket = getDirectSocket(tempHeaders, tempUrl);
			if (tempDirectSocket == null) {
				// try the proxy
				tempProxySocket = getProxySocket(tempHeaders);
				if (tempProxySocket == null) {
					replyWithBadGateway("Direct and Proxy not responding");
					return;
				} else {
					tempProxyDecision = ProxyDecision.PROXY;
				}
			} else {
				tempProxyDecision = ProxyDecision.DIRECT;
			}
		}
		setProxyDecision(tempUrl, tempProxyDecision);

		config.fireLogEvent(tempUrl + " via " + tempProxyDecision);

		tempFinalTargetSocket = tempProxySocket != null ? tempProxySocket : tempDirectSocket;
		if (tempFinalTargetSocket == null) {
			LOGGER.error("can not happen.");
			return;
		}

		new Thread() {
			@Override
			public void run() {
				forwardData(tempFinalTargetSocket, socket);
			}
		}.start();
		new Thread() {
			@Override
			public void run() {
				forwardData(socket, tempFinalTargetSocket);
			}
		}.start();
	}

	/**
	 *
	 */
	private Socket getDirectSocket(List<String> aHeaders, URL aUrl) {
		if (aUrl != null) {
			try {
				String tempProtocol = aUrl.getProtocol();
				String tempHost = aUrl.getHost();
				int tempPort = aUrl.getPort();
				if ("http".equalsIgnoreCase(tempProtocol)) {
					Socket tempDirectSocket = new Socket();
					tempDirectSocket.connect(new InetSocketAddress(tempHost, tempPort), 5000);
					forwardHeaders(aHeaders, tempDirectSocket, false);
					return tempDirectSocket;
				} else if ("https".equalsIgnoreCase(tempProtocol)) {
					// Use normal socket to let original application do the handshake.
					// https://github.com/tomakehurst/wiremock/issues/62
					// "The reason is that HTTPS proxies don't attempt to decrypt the exchange, but make a CONNECT request to the destination and effectively become TCP tunnels."
					//							SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
					//							tempSSLSocket = (SSLSocket) factory.createSocket(tempHost, tempPort);
					//							tempSSLSocket.startHandshake();
					Socket tempSSLSocket = new Socket();
					tempSSLSocket.connect(new InetSocketAddress(tempHost, tempPort), 5000);
					// do not send headers ad it is a "connect" request.
					try {
						// Just reply with "connection ok"
						OutputStreamWriter outputStreamWriter = new OutputStreamWriter(socket.getOutputStream(), "UTF-8");
						outputStreamWriter.write("HTTP/1.1 200 Connection established" + "\r\n");
						outputStreamWriter.write("Proxy-agent: escape-from-intranet\r\n");
						outputStreamWriter.write("\r\n");
						outputStreamWriter.flush();
					} catch (IOException e2) {
						LOGGER.error("Error sending response to " + socket, e2);
						return null;
					}
					return tempSSLSocket;
				} else {
					LOGGER.error("Unknown protocol " + aUrl);
					return null;
				}
			} catch (IOException eDirect) {
				LOGGER.info("Direct " + aUrl + " not reachable " + eDirect);
				LOGGER.debug("Cannot reach direct ", eDirect);
				return null;
			}
		} else {
			LOGGER.debug("Did not find URL in {}", aHeaders);
			return null;
		}
	}

	/**
	 *
	 */
	private Socket getProxySocket(List<String> aHeaders) {
		String tempProxyHost = config.getProxyHost();
		Integer tempProxyPort = Integer.valueOf(config.getProxyPort());
		try {
			Socket tempProxySocket = new Socket();
			tempProxySocket.connect(new InetSocketAddress(tempProxyHost, tempProxyPort), 5000);
			forwardHeaders(aHeaders, tempProxySocket, true);
			return tempProxySocket;
		} catch (IOException eProxy) {
			LOGGER.info("Proxy " + tempProxyHost + ":" + tempProxyPort + " not reachable " + eProxy);
			LOGGER.debug("Cannot reach proxy ", eProxy);
			return null;
		}
	}

	/**
	 * @param aHeaders
	 * @throws IOException
	 *
	 */
	private void forwardHeaders(List<String> aHeaders, Socket aSocket, boolean anAddProxyAuthentication) throws IOException {

		OutputStream tempProxyOut = aSocket.getOutputStream();
		for (String tempHeader : aHeaders) {
			if (tempHeader.toLowerCase().startsWith("proxy-authorization:")) {
				LOGGER.debug("Skip original {}", tempHeader);
				continue;
			}
			LOGGER.debug("Forward {}", tempHeader);
			tempProxyOut.write((tempHeader + "\r\n").getBytes());
		}
		if (anAddProxyAuthentication) {
			String tempAuth = new String(Base64.getEncoder().encode((config.getProxyUser() + ":" + config.getProxyPassword()).getBytes()));
			String tempProxyHeaderAuth = "Proxy-Authorization: Basic " + tempAuth;
			LOGGER.debug("Add {}", tempProxyHeaderAuth);
			tempProxyOut.write((tempProxyHeaderAuth + "\r\n").getBytes());
		}
		tempProxyOut.write(("\r\n").getBytes());
		tempProxyOut.flush();
	}

	/**
	 *
	 */
	private static synchronized ProxyDecision getProxyDecision(URL aUrl) {
		return proxyDecisionCache.get(aUrl.getHost());
	}

	/**
	 *
	 */
	private static synchronized void setProxyDecision(URL aUrl, ProxyDecision aProxyDecision) {
		proxyDecisionCache.put(aUrl.getHost(), aProxyDecision);
	}

	/**
	 *
	 */
	private void replyWithBadGateway(String aText) {
		try {
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(socket.getOutputStream(), "UTF-8");
			String tempResponseMessage = "HTTP/1.0" + " " + java.net.HttpURLConnection.HTTP_BAD_GATEWAY + " " + aText;
			config.fireLogEvent(aText);
			outputStreamWriter.write(tempResponseMessage + "\r\n");
			outputStreamWriter.write("Proxy-agent: escape-from-intranet\r\n");
			outputStreamWriter.write("\r\n");
			outputStreamWriter.flush();
		} catch (IOException e) {
			LOGGER.error("Error sending error reply", e);
		}
	}

	/**
	 *
	 */
	private URL getUrlFromHeader(List<String> aHeaders) {
		// Try a direct connection
		// firstline e.g.: GET http://www.google.com HTTP/1.1
		String tempFirstLine = aHeaders.get(0);
		if (tempFirstLine.toLowerCase().startsWith("connect ")) {
			// CONNECT www.google.com:443 HTTP/1.1
			int tempSpace1 = tempFirstLine.indexOf(" ");
			int tempSpace2 = tempFirstLine.indexOf(" ", tempSpace1 + 1);
			if (tempSpace1 > 0 && tempSpace2 > 0) {
				URL tempUrl;
				try {
					String tempString = "https://" + tempFirstLine.substring(tempSpace1 + 1, tempSpace2);
					tempUrl = new URL(tempString);
					if (tempUrl.getPort() == -1) {
						tempUrl = new URL(tempUrl.getProtocol(), tempUrl.getHost(), 443, tempUrl.getFile());
					}
					return tempUrl;
				} catch (MalformedURLException eUrl) {
					LOGGER.info("Direct URL wrong " + tempFirstLine + " " + eUrl);
					LOGGER.debug("Direct URL wrong ", eUrl);
					return null;
				}
			}
		}
		for (String tempHeader : aHeaders) {
			if (tempHeader.toLowerCase().startsWith("host: ")) {
				try {
					URL tempUrl = new URL("http://" + tempHeader.substring(6));
					if (tempUrl.getPort() == -1) {
						tempUrl = new URL(tempUrl.getProtocol(), tempUrl.getHost(), 80, tempUrl.getFile());
					}
					return tempUrl;
				} catch (MalformedURLException eUrl) {
					LOGGER.info("Direct URL wrong " + tempFirstLine + " " + eUrl);
					LOGGER.debug("Direct URL wrong ", eUrl);
					return null;
				}
			}
		}
		return null;
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
								LOGGER.debug("Write to " + outputSocket.getInetAddress().getHostName() + ":" + read + " bytes");
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
			LOGGER.info("Closed" + e);
			LOGGER.debug("Error", e);
		}
		LOGGER.info("finished");
	}
}
