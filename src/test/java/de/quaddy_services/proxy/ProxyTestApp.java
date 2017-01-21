package de.quaddy_services.proxy;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URL;
import java.net.URLConnection;

/**
 *
 */
public class ProxyTestApp {

	public static void main(String[] args) throws IOException {
		new ProxyTestApp().testHttpGoogle();
	}

	public void testHttpGoogle() throws IOException {
		URL tempUrl = new URL("https://www.google.com");
		Proxy tempProxy = new Proxy(Type.HTTP, new InetSocketAddress(3128));
		URLConnection tempConnection = tempUrl.openConnection(tempProxy);

		InputStream in = tempConnection.getInputStream();

		byte[] tempB = new byte[1000];
		int tempRead = in.read(tempB);
		System.out.println("Result=" + new String(tempB, 0, tempRead));

		assertTrue(true);
	}

}
