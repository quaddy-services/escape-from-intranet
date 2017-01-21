package de.quaddy_services.proxy;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.SystemTray;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import javax.swing.WindowConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 */
public class EscapeProxy {

	private static Logger LOGGER;

	public static void main(String[] args) {
		initializeLogger();

		LOGGER.info("Start");
		Properties tempProperties = readConfig();

		EscapeProxyConfig tempEscapeProxyConfig = new EscapeProxyConfig(tempProperties);

		final EscapeProxyFrame tempEscapeProxyFrame = new EscapeProxyFrame(tempEscapeProxyConfig);

		tempEscapeProxyFrame.setSize(new Dimension(500, 400));
		tempEscapeProxyFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		Thread.setDefaultUncaughtExceptionHandler(new LoggingUncaughtExceptionHandler());
		// See java.awt.EventDispatchThread.handlerPropName
		System.setProperty("sun.awt.exception.handler", LoggingUncaughtExceptionHandler.class.getName());

		addExitListener(tempEscapeProxyFrame, tempProperties);
		initSystemTray(tempEscapeProxyFrame);

		setVisible(tempEscapeProxyFrame);

		new EscapeProxyWorkerAccept(tempEscapeProxyConfig).start();
	}
	/**
	 * Before loading logback.xml, set the properties
	 */
	private static void initializeLogger() {
		String tempDefaultLevel = System.getProperty("defaultLogLevel");
		if (tempDefaultLevel ==null) {
			System.setProperty("defaultLogLevel","info");
		}
		LOGGER = LoggerFactory.getLogger(EscapeProxy.class);
	}



	/**
	 * @return
	 *
	 */
	private static Properties readConfig() {
		Properties tempProperties = new Properties();
		File tempFile = getFile();
		if (tempFile.exists()) {
			try {
				FileInputStream tempIn = new FileInputStream(tempFile);
				tempProperties.loadFromXML(tempIn);
				tempIn.close();
				LOGGER.info("Loaded config "+tempFile.getAbsolutePath());
				LOGGER.debug("Content={}",tempProperties);
			} catch (IOException e) {
				LOGGER.error("error", e);
			}
		}
		return tempProperties;
	}

	/**
	 *
	 */
	private static File getFile() {
		File tempFile = new File(System.getProperty("user.home") + "/" + "escape-from-intranet.xml");
		return tempFile;
	}

	/**
	 *
	 */
	private static void initSystemTray(EscapeProxyFrame aEscapeProxyFrame) {
		if (SystemTray.isSupported()) {
			// TODO
		}
	}

	/**
	 *
	 */
	private static void setVisible(final EscapeProxyFrame tempEscapeProxyFrame) {
		EventQueue.invokeLater(new Runnable() {
			/**
			 *
			 */
			@Override
			public void run() {
				tempEscapeProxyFrame.setVisible(true);
			}
		});
	}

	/**
	 * @param aProperties
	 *
	 */
	private static void addExitListener(final EscapeProxyFrame tempEscapeProxyFrame, Properties aProperties) {
		tempEscapeProxyFrame.addWindowListener(new WindowAdapter() {
			/**
			 *
			 */
			@Override
			public void windowClosing(WindowEvent aE) {
				saveConfig(aProperties);
				super.windowClosing(aE);
				LOGGER.info("Exit");
				System.exit(0);
			}
		});
	}

	/**
	 *
	 */
	protected static void saveConfig(Properties aProperties) {
		try {
			File tempFile = getFile();
			LOGGER.info("Save config "+tempFile.getAbsolutePath());
			OutputStream tempOut = new FileOutputStream(tempFile);
			aProperties.storeToXML(tempOut, "");
			tempOut.close();
		} catch (IOException e) {
			LOGGER.error("error", e);
		}
	}
}
