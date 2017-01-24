package de.quaddy_services.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
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
		new ProxyTestApp().testHttpsGoogle();
	}

	public void testHttpGoogle() throws IOException {
		String tempHttp = "http://www.google.com";
		readViaProxy(tempHttp);

	}

	public void testHttpsGoogle() throws IOException {
		String tempHttp = "https://www.google.com";
		readViaProxy(tempHttp);

	}

	/**
	 *
	 */
	private void readViaProxy(String tempHttp) throws MalformedURLException, IOException {
		System.out.println();
		System.out.println(tempHttp);
		System.out.println();

		URL tempUrl = new URL(tempHttp);

		Proxy tempProxy = new Proxy(Type.HTTP, new InetSocketAddress(3128));
		HttpURLConnection tempConnection = (HttpURLConnection) tempUrl.openConnection(tempProxy);

		tempConnection.setConnectTimeout(30000);
		tempConnection.setReadTimeout(30000);

		InputStream in;
		try {
			 in = tempConnection.getInputStream();
		} catch (IOException e) {
			System.err.println("ResponseCode=" + tempConnection.getResponseCode());
			System.err.println("ResponseMessage=" + tempConnection.getResponseMessage());
			throw e;
		}

		byte[] tempB = new byte[1000];
		int tempRead = in.read(tempB);
		int tempTotal = tempRead;
		if (tempRead > 0) {
			System.out.println("Result=" + new String(tempB, 0, tempRead));
			while (tempRead > 0) {
				tempRead = in.read(tempB);
				tempTotal+=tempRead;
				System.out.println("Read another " + tempRead + " bytes");
			}
		}
		System.out.println("Total="+tempTotal);
		in.close();
	}

}
