package de.quaddy_services.proxy;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import de.quaddy_services.proxy.events.PortStatusListener;
import de.quaddy_services.proxy.logging.Logger;
import de.quaddy_services.proxy.logging.LoggerFactory;

/**
 *
 */
public class EscapeProxy {

	private static Logger LOGGER;

	private static boolean applicationIsExiting = false;

	private static EscapeProxyFrame tempEscapeProxyFrame;
	private static Properties tempProperties;

	public static void main(String[] args) throws IOException {
		initializeLogger();

		LOGGER.info("Start");
		tempProperties = readConfig();

		final EscapeProxyConfig tempEscapeProxyConfig = new EscapeProxyConfig(tempProperties);

		tempEscapeProxyFrame = new EscapeProxyFrame(tempEscapeProxyConfig, getFrameTitle());

		tempEscapeProxyFrame.setSize(new Dimension(500, 400));
		tempEscapeProxyFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		tempEscapeProxyFrame.setShutdownAndExitAction(new AbstractAction("Shutdown and Exit") {

			private static final long serialVersionUID = 8818325653972860075L;

			@Override
			public void actionPerformed(ActionEvent event) {
				shutdownAndExit();
			}
		});
		tempEscapeProxyFrame.setClearProxyDecisionAction(new AbstractAction("Clear Proxy Decision") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(@SuppressWarnings("unused") ActionEvent event) {
				clearProxyDecision();
			}
		});

		Thread.setDefaultUncaughtExceptionHandler(new LoggingUncaughtExceptionHandler());
		// See java.awt.EventDispatchThread.handlerPropName
		System.setProperty("sun.awt.exception.handler", LoggingUncaughtExceptionHandler.class.getName());

		trayImage = new Image[] { getImage("offline.png"), getImage("online.png") };

		initWindowListener();
		initSystemTray(tempEscapeProxyConfig);

		setVisible(tempEscapeProxyConfig);
		addStatusListener(tempEscapeProxyConfig);

		new EscapeProxyWorkerAccept(tempEscapeProxyConfig).start();
	}

	/**
	 *
	 */
	protected static void clearProxyDecision() {
		EscapeProxyWorkerSocket.clearProxyDecisionCache();
	}

	/**
	 *
	 */
	private static String getFrameTitle() {
		final Properties tempProperties = new Properties();
		final InputStream tempIn = EscapeProxy.class.getResourceAsStream("/META-INF/maven/de.quaddy_services/escape-from-intranet/pom.properties");
		if (tempIn != null) {
			try {
				tempProperties.load(tempIn);
				tempIn.close();
			} catch (final IOException e) {
				LOGGER.error("Ignore readingerror pom.properties", e);
			}
		}
		final String tempVersion = tempProperties.getProperty("version", "?");
		return "Escape from intranet proxy " + tempVersion;
	}

	/**
	 *
	 */
	private static void addStatusListener(EscapeProxyConfig aEscapeProxyConfig) {

		aEscapeProxyConfig.addStatusListener(new PortStatusListener() {

			@Override
			public void statusChanged(boolean aOkFlag, @SuppressWarnings("unused") String aText) {
				EventQueue.invokeLater(new Runnable() {

					@Override
					public void run() {
						tempEscapeProxyFrame.setIconImage(trayImage[aOkFlag ? 1 : 0]);
					}
				});
			}
		});
	}

	/**
	 * Before loading logback.xml, set the properties
	 */
	private static void initializeLogger() {
		final String tempDefaultLevel = System.getProperty("defaultLogLevel");
		if (tempDefaultLevel == null) {
			System.setProperty("defaultLogLevel", "info");
		}
		LOGGER = LoggerFactory.getLogger(EscapeProxy.class);
	}

	/**
	 * @return
	 */
	private static synchronized Properties readConfig() {
		final Properties tempProperties = new Properties();
		final File tempFile = getFile();
		if (tempFile.exists()) {
			try {
				final FileInputStream tempIn = new FileInputStream(tempFile);
				tempProperties.loadFromXML(tempIn);
				tempIn.close();
				LOGGER.info("Loaded config " + tempFile.getAbsolutePath());
				LOGGER.debug("Content={}", tempProperties);
				previouslySavedProperties = new Properties();
				previouslySavedProperties.putAll(tempProperties);
			} catch (final IOException e) {
				LOGGER.error("error", e);
			}
		}
		return tempProperties;
	}

	/**
	 *
	 */
	private static File getFile() {
		final File tempFile = new File(System.getProperty("user.home") + "/" + "escape-from-intranet.xml");
		return tempFile;
	}

	private static Image[] trayImage;

	private static Properties previouslySavedProperties;

	/**
	 * @param aEscapeProxyConfig
	 */
	private static void initSystemTray(EscapeProxyConfig aEscapeProxyConfig) {
		if (SystemTray.isSupported()) {
			EventQueue.invokeLater(new Runnable() {
				/**
				 * Init gui in eventqueue
				 */
				@Override
				public void run() {
					try {
						final SystemTray tempSystemTray = SystemTray.getSystemTray();
						final TrayIcon tempTrayIcon = new TrayIcon(trayImage[0]);
						tempTrayIcon.addMouseListener(new MouseAdapter() {
							@Override
							public void mouseClicked(@SuppressWarnings("unused") MouseEvent aE) {
								tempEscapeProxyFrame.setVisible(true);
								tempEscapeProxyFrame.toFront();
								tempEscapeProxyFrame.setState(Frame.NORMAL);
							}
						});
						tempSystemTray.add(tempTrayIcon);
					} catch (final AWTException e) {
						LOGGER.error("Error", e);
					}
				}
			});
			aEscapeProxyConfig.addStatusListener(new PortStatusListener() {
				private Boolean currentStatus = null;

				@Override
				public void statusChanged(boolean aOkFlag, String aText) {
					if (SystemTray.isSupported()) {
						EventQueue.invokeLater(new Runnable() {
							@Override
							public void run() {
								final TrayIcon tempTrayIcon = SystemTray.getSystemTray().getTrayIcons()[0];
								tempTrayIcon.setImageAutoSize(true);
								tempTrayIcon.setImage(trayImage[aOkFlag ? 1 : 0]);
								tempTrayIcon.setToolTip(aText);
							}
						});
						if (currentStatus == null || currentStatus ^ aOkFlag) {
							// show first status or changes
							EventQueue.invokeLater(new Runnable() {
								@Override
								public void run() {
									final TrayIcon tempTrayIcon = SystemTray.getSystemTray().getTrayIcons()[0];
									tempTrayIcon.displayMessage("Status change", aText, MessageType.INFO);
								}
							});
						}
						currentStatus = aOkFlag;
					}
					EventQueue.invokeLater(new Runnable() {
						@Override
						public void run() {
							tempEscapeProxyFrame.setIconImage(trayImage[aOkFlag ? 1 : 0]);
						}
					});
				}
			});
		}
	}

	/**
	 * @throws IOException
	 */
	private static Image getImage(String aString) throws IOException {
		final InputStream tempIn = EscapeProxy.class.getClassLoader().getResourceAsStream(aString);
		final BufferedImage tempImage = ImageIO.read(tempIn);
		return tempImage;
	}

	/**
	 * @param aEscapeProxyConfig
	 */
	private static void setVisible(EscapeProxyConfig aEscapeProxyConfig) {
		EventQueue.invokeLater(new Runnable() {
			/**
			 *
			 */
			@Override
			public void run() {
				tempEscapeProxyFrame.setIconImage(trayImage[0]);
				tempEscapeProxyFrame.setVisible(true);
				final String tempProxyUser = aEscapeProxyConfig.getProxyUser();
				if (tempProxyUser != null && tempProxyUser.length() > 0) {
					tempEscapeProxyFrame.setState(Frame.ICONIFIED);
				}
			}
		});
	}

	private static void shutdownAndExit() {
		tempEscapeProxyFrame.setVisible(false);
		saveConfig();
		LOGGER.info("Window Closing");
		final Timer tempTimer = new Timer(5000, new ActionListener() {

			@Override
			public void actionPerformed(@SuppressWarnings("unused") ActionEvent aE2) {
				LOGGER.info("Exit");
				setApplicationIsExiting(true);
				System.exit(0);
			}
		});
		tempTimer.start();
		if (SystemTray.isSupported()) {
			final TrayIcon tempTrayIcon = SystemTray.getSystemTray().getTrayIcons()[0];
			tempTrayIcon.displayMessage("Shutdown", "localhost proxy\nis shutting down...", MessageType.WARNING);
		}
	}

	/**
	 * @param aProperties
	 */
	private static void initWindowListener() {
		tempEscapeProxyFrame.addWindowListener(new WindowAdapter() {

			/**
			 * shutdown and exit if no other action is set
			 */
			@Override
			public void windowClosing(@SuppressWarnings("unused") WindowEvent aE) {
				if (tempEscapeProxyFrame.getShutdownAndExitAction() == null) {
					shutdownAndExit();
				} else {
					windowIconified(aE);
					if (SystemTray.isSupported()) {
						final TrayIcon tempTrayIcon = SystemTray.getSystemTray().getTrayIcons()[0];
						tempTrayIcon.displayMessage("Still running", "The proxy\nist still running...", MessageType.INFO);
					}
				}
			}

			/**
			 * called when windows looses focus.
			 */
			@Override
			public void windowDeactivated(WindowEvent aE) {
				saveConfig();
				super.windowDeactivated(aE);
			}

			/**
			 *
			 */
			@Override
			public void windowIconified(WindowEvent aE) {
				super.windowIconified(aE);
				if (SystemTray.isSupported()) {
					tempEscapeProxyFrame.setVisible(false);
				}
			}
		});
	}

	/**
	 *
	 */
	private synchronized static void saveConfig() {
		if (isApplicationIsExiting()) {
			LOGGER.info("Not saving as already Sytem.exit() is in progress");
			return;
		}
		if (previouslySavedProperties == null || !previouslySavedProperties.equals(tempProperties)) {
			saveConfigFile();
		} else {
			LOGGER.debug("Skip saving same properties");
		}
		previouslySavedProperties = new Properties();
		previouslySavedProperties.putAll(tempProperties);
	}

	private static void saveConfigFile() {
		try {
			final File tempFile = getFile();
			LOGGER.info("Save config " + tempFile.getAbsolutePath() + " ...");
			final OutputStream tempOut = new FileOutputStream(tempFile);
			tempProperties.storeToXML(tempOut, "");
			tempOut.close();
			LOGGER.info("Saved config " + tempFile.getAbsolutePath());
		} catch (final IOException e) {
			LOGGER.error("error", e);
		}
	}

	/**
	 * @see #applicationIsExiting
	 */
	public synchronized static boolean isApplicationIsExiting() {
		return applicationIsExiting;
	}

	/**
	 * @see #applicationIsExiting
	 */
	public synchronized static void setApplicationIsExiting(boolean aApplicationIsExiting) {
		applicationIsExiting = aApplicationIsExiting;
	}
}
