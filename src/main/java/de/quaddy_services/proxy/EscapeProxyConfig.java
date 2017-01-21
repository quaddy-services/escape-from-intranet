package de.quaddy_services.proxy;

import java.io.IOException;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Properties;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.event.EventListenerList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.quaddy_services.proxy.events.CheckPortListener;
import de.quaddy_services.proxy.events.PortStatusListener;

/**
 *
 */
public class EscapeProxyConfig implements Serializable {

	private static final Logger LOGGER = LoggerFactory.getLogger(EscapeProxyConfig.class);

	private Properties properties;

	/**
	 *
	 */
	public EscapeProxyConfig(Properties aProperties) {
		properties = aProperties;
	}

	/**
	 * @see #proxyHost
	 */
	public String getProxyHost() {
		return properties.getProperty("ProxyHost");
	}

	/**
	 * @see #proxyHost
	 */
	public void setProxyHost(String aProxyHost) {
		properties.setProperty("ProxyHost", aProxyHost);
	}

	/**
	 * @see #proxyUser
	 */
	public String getProxyUser() {
		return properties.getProperty("ProxyUser");
	}

	/**
	 * @see #proxyUser
	 */
	public void setProxyUser(String aProxyUser) {
		properties.setProperty("ProxyUser", aProxyUser);
	}

	/**
	 * @see #proxyPassword
	 */
	public String getProxyPassword() {
		String tempProperty = properties.getProperty("ProxyPassword");
		if (tempProperty == null) {
			return null;
		}
		return decrypt(tempProperty);
	}

	/**
	 *
	 */
	private String decrypt(String aProperty) {
		try {
			byte[] tempEncoded = getKey();
			SecretKey secret = new SecretKeySpec(tempEncoded, "AES");
			Cipher cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.DECRYPT_MODE, secret);

			byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(aProperty));

			return new String(decryptedBytes);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
			LOGGER.error("error", e);
			return null;
		}
	}

	/**
	 * @see #proxyPassword
	 */
	public void setProxyPassword(String aProxyPassword) {
		properties.setProperty("ProxyPassword", encrypt(aProxyPassword));
	}

	/**
	 *
	 */
	private String encrypt(String aProxyPassword) {
		try {
			byte[] tempEncoded = getKey();

			SecretKey secret = new SecretKeySpec(tempEncoded, "AES");
			Cipher cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.ENCRYPT_MODE, secret);

			byte[] encryptedBytes = cipher.doFinal(aProxyPassword.getBytes());

			return Base64.getEncoder().encodeToString(encryptedBytes);

		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
			LOGGER.error("error", e);
			return null;
		}
	}

	/**
	 *
	 */
	private byte[] getKey() throws NoSuchAlgorithmException {
		String tempKeyString = properties.getProperty("key");
		if (tempKeyString != null) {
			return Base64.getDecoder().decode(tempKeyString);
		}
		LOGGER.info("Create key...");
		KeyGenerator kgen = KeyGenerator.getInstance("AES");
		kgen.init(128); // 192 and 256 bits may not be available
		SecretKey tempKey = kgen.generateKey();
		byte[] tempEncoded = tempKey.getEncoded();
		properties.setProperty("key", Base64.getEncoder().encodeToString(tempEncoded));
		return tempEncoded;
	}

	public String getLocalPort() {
		String tempLocalPort = properties.getProperty("localPort");
		if (tempLocalPort==null) {
			return "0";
		}
		try {
			return Integer.valueOf(tempLocalPort).toString();
		} catch (NumberFormatException e) {
			LOGGER.debug("Ignore ",e);
			return "0";
		}
	}

	public void setLocalPort(String aPort) {
		if (aPort == null) {
			properties.setProperty("localPort",null);
		} else {
			try {
				properties.setProperty("localPort", Integer.valueOf(aPort).toString());
			} catch (NumberFormatException e) {
				LOGGER.debug("Ignore ",e);
			}
		}

	}

	private EventListenerList events = new EventListenerList();
	/**
	 *
	 */
	public void fireCheckPortEvent() {
		CheckPortListener[] tempListenerList = events.getListeners(CheckPortListener.class);
		for (CheckPortListener tempListener : tempListenerList) {
			tempListener.checkPort(Integer.valueOf(getLocalPort()));
		}
	}

	/**
	 *
	 */
	public void addStatusListener(PortStatusListener aStatusListener) {
		events.add(PortStatusListener.class, aStatusListener);
	}
}
