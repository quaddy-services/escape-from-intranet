package de.quaddy_services.proxy;

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
		new ProxyTestApp().testHttpsGoogle();
	}

	public void testHttpsGoogle() throws IOException {
		URL tempUrl = new URL("https://www.google.com");

		Proxy tempProxy = new Proxy(Type.HTTP, new InetSocketAddress(3128));
		URLConnection tempConnection = tempUrl.openConnection(tempProxy);

		InputStream in = tempConnection.getInputStream();

		for (int i = 0; i < 100; i++) {
			byte[] tempB = new byte[100000];
			int tempRead = in.read(tempB);
			if (tempRead > 0) {
				System.out.println("Result=" + new String(tempB, 0, tempRead));
			}
		}

	}

}
