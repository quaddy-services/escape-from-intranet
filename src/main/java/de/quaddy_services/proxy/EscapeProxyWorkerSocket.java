package de.quaddy_services.proxy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

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
		List<String> tempHeader;
		try {
			tempHeader = readHeader(socket);
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

		if (tempHeader.size() == 0) {
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
		String tempFirstLine = tempHeader.get(0);
		StringTokenizer tempFirstLineTokens = new StringTokenizer(tempFirstLine, " ");
		String tempFirstLine1 = tempFirstLineTokens.nextToken();
		if (tempFirstLine1.equals("CONNECT")) {
			// https
			String tempHostAndPort = tempFirstLineTokens.nextToken();
			String tempHttpVersion = tempFirstLineTokens.nextToken();
			Proxy tempProxy = new Proxy(Type.HTTP, new InetSocketAddress(config.getProxyHost(), Integer.valueOf(config.getProxyPort())));
			Authenticator.setDefault(new Authenticator() {
				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(config.getProxyUser(), config.getProxyPassword().toCharArray());
				}
			});
			Socket tempTargetSocket = new Socket(tempProxy);
			StringTokenizer tempHostAndPortTokens = new StringTokenizer(tempHostAndPort, ":");
			String tempHost = tempHostAndPortTokens.nextToken();
			String tempPort = tempHostAndPortTokens.nextToken();
			try {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Connect to " + tempHost + ":" + tempPort + " via " + tempProxy);
				}
				try {
					tempTargetSocket.connect(new InetSocketAddress(tempHost, Integer.valueOf(tempPort)));
				} catch (Error e) {
					if (e.getCause() instanceof InvocationTargetException) {
						InvocationTargetException tempInvocationTargetException = (InvocationTargetException) e.getCause();
						if (tempInvocationTargetException.getTargetException() instanceof IOException) {
							// Extract the original IOException, see
							// http://stackoverflow.com/questions/40921296/java-proxy-authentication-ioexception-not-being-thrown/
							throw (IOException) tempInvocationTargetException.getTargetException();
						}
					}
					// rethrow the original error
					throw e;
				}
			} catch (IOException | RuntimeException e) {
				LOGGER.error("Error connecting to " + tempHost + ":" + tempPort + " via " + tempProxy, e);
				try {
					OutputStreamWriter outputStreamWriter = new OutputStreamWriter(socket.getOutputStream(), "UTF-8");
					String tempMessage = e.getMessage();
					// Maybe: Unable to tunnel through proxy. Proxy returns "HTTP/1.0 407 Proxy Authentication Required"
					int tempHttpPos = tempMessage.indexOf("HTTP/");
					if (tempHttpPos >= 0) {
						// Reply with original Message
						outputStreamWriter.write(tempMessage.substring(tempHttpPos) + "\r\n");
					} else {
						outputStreamWriter.write(tempHttpVersion + " 404 " + tempMessage + "\r\n");
					}
					outputStreamWriter.write("Proxy-agent: escape-from-intranet\r\n");
					outputStreamWriter.write("\r\n");
					outputStreamWriter.flush();
				} catch (IOException e2) {
					LOGGER.error("Error sending response to " + socket, e2);
				}
				return;
			}
			try {
				OutputStreamWriter outputStreamWriter = new OutputStreamWriter(socket.getOutputStream(), "UTF-8");
				outputStreamWriter.write(tempHttpVersion + " 200 Connection established\r\n");
				outputStreamWriter.write("Proxy-agent: escape-from-intranet\r\n");
				outputStreamWriter.write("\r\n");
				outputStreamWriter.flush();
			} catch (IOException e) {
				LOGGER.error("Error sending response to " + socket, e);
			}
		} else {
			// http
		}
	}

	/**
	 *
	 */
	private List<String> readHeader(Socket aSocket) throws IOException {
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
	private static void forwardData(Socket inputSocket, Socket outputSocket) {
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
			e.printStackTrace(); // TODO: implement catch
		}
	}
}
