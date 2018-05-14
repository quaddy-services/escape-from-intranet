package de.quaddy_services.proxy;

import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.event.EventListenerList;

import de.quaddy_services.proxy.events.CheckPortListener;
import de.quaddy_services.proxy.events.LogEventListener;
import de.quaddy_services.proxy.events.PortStatusListener;
import de.quaddy_services.proxy.logging.Logger;
import de.quaddy_services.proxy.logging.LoggerFactory;

/**
 *
 */
public class EscapeProxyConfig implements Serializable {

    private static final long serialVersionUID = -2919717071053609165L;

    private static final Logger LOGGER = LoggerFactory.getLogger(EscapeProxyConfig.class);

    private final Properties properties;

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
        final String tempProperty = properties.getProperty("ProxyPassword");
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
            final byte[] tempEncoded = getKey();
            final SecretKey secret = new SecretKeySpec(tempEncoded, "AES");
            final Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secret);

            final byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(aProperty));

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
            final byte[] tempEncoded = getKey();

            final SecretKey secret = new SecretKeySpec(tempEncoded, "AES");
            final Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secret);

            final byte[] encryptedBytes = cipher.doFinal(aProxyPassword.getBytes());

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
        final String tempKeyString = properties.getProperty("key");
        if (tempKeyString != null) {
            return Base64.getDecoder().decode(tempKeyString);
        }
        LOGGER.info("Create key...");
        final KeyGenerator kgen = KeyGenerator.getInstance("AES");
        kgen.init(128); // 192 and 256 bits may not be available
        final SecretKey tempKey = kgen.generateKey();
        final byte[] tempEncoded = tempKey.getEncoded();
        properties.setProperty("key", Base64.getEncoder().encodeToString(tempEncoded));
        return tempEncoded;
    }

    public String getLocalPort() {
        final String tempLocalPort = properties.getProperty("localPort");
        if (tempLocalPort == null) {
            return "0";
        }
        try {
            return Integer.valueOf(tempLocalPort).toString();
        } catch (final NumberFormatException e) {
            LOGGER.debug("Ignore ", e);
            return "0";
        }
    }

    public void setLocalPort(String aPort) {
        if (aPort == null) {
            properties.setProperty("localPort", null);
        } else {
            try {
                properties.setProperty("localPort", Integer.valueOf(aPort).toString());
            } catch (final NumberFormatException e) {
                LOGGER.debug("Ignore ", e);
            }
        }

    }

    private final EventListenerList events = new EventListenerList();

    private final LinkedList<String> log = new LinkedList<String>();

    /**
     *
     */
    public void fireCheckPortEvent() {
        final CheckPortListener[] tempListenerList = events.getListeners(CheckPortListener.class);
        for (final CheckPortListener tempListener : tempListenerList) {
            tempListener.checkPort(Integer.valueOf(getLocalPort()));
        }
    }

    /**
     *
     */
    public void addStatusListener(PortStatusListener aStatusListener) {
        events.add(PortStatusListener.class, aStatusListener);
    }

    /**
     *
     */
    public void addCheckPortListener(CheckPortListener aCheckPortListener) {
        events.add(CheckPortListener.class, aCheckPortListener);
    }

    /**
     *
     */
    public void firePortStatus(boolean anOkFlag, String aStatus) {
        final PortStatusListener[] tempListenerList = events.getListeners(PortStatusListener.class);
        for (final PortStatusListener tempListener : tempListenerList) {
            tempListener.statusChanged(anOkFlag, aStatus);
        }
        fireLogEvent(aStatus);
    }

    /**
     *
     */
    public String getProxyPort() {
        final String tempPort = properties.getProperty("ProxyPort");
        if (tempPort == null) {
            return "0";
        }
        try {
            return Integer.valueOf(tempPort).toString();
        } catch (final NumberFormatException e) {
            LOGGER.debug("Ignore ", e);
            return "0";
        }

    }

    public void setProxyPort(String aPort) {
        if (aPort == null) {
            properties.setProperty("ProxyPort", null);
        } else {
            try {
                properties.setProperty("ProxyPort", Integer.valueOf(aPort).toString());
            } catch (final NumberFormatException e) {
                LOGGER.debug("Ignore ", e);
            }
        }
    }

    /**
     *
     */
    public void fireLogEvent(String aMsg) {
        LOGGER.info(aMsg);
        final LocalTime tempTime = LocalTime.now();
        // #1 Take a copy to avoid Nullpointers when iterating
        List<String> tempCopyOfLog;
        synchronized (log) {
            log.add(tempTime + ":" + aMsg);
            if (log.size() > 20) {
                log.removeFirst();
            }
            tempCopyOfLog = new ArrayList<>(log);
        }
        final LogEventListener[] tempListenerList = events.getListeners(LogEventListener.class);
        for (final LogEventListener tempListener : tempListenerList) {
            tempListener.updatedLog(tempCopyOfLog);
        }
    }

    /**
     *
     */
    public void addLogEventListener(LogEventListener aLogEventListener) {
        events.add(LogEventListener.class, aLogEventListener);
    }
}
