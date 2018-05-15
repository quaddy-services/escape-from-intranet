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

	private boolean applicationIsExiting = false;

	private EscapeProxyFrame escapeProxyFrame;
	private Properties properties;

	public static void main(String[] args) {
		initializeLogger();

		EventQueue.invokeLater(() -> {
			try {
				EscapeProxy tempEscapeProxy = new EscapeProxy();
				tempEscapeProxy.mainInEventQueue();
			} catch (IOException e) {
				LOGGER.error("Error initializing", e);
				System.exit(5);
			}
		});

	}

	/**
	 * @throws IOException
	*
	*/
	private void mainInEventQueue() throws IOException {
		LOGGER.info("Start");
		properties = readConfig();

		final EscapeProxyConfig tempEscapeProxyConfig = new EscapeProxyConfig(properties);

		escapeProxyFrame = new EscapeProxyFrame(tempEscapeProxyConfig, getFrameTitle());

		escapeProxyFrame.setSize(new Dimension(500, 400));
		escapeProxyFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		escapeProxyFrame.setShutdownAndExitAction(new AbstractAction("Shutdown and Exit") {

			private static final long serialVersionUID = 8818325653972860075L;

			@Override
			public void actionPerformed(@SuppressWarnings("unused") ActionEvent event) {
				shutdownAndExit();
			}
		});
		escapeProxyFrame.setClearProxyDecisionAction(new AbstractAction("Clear Proxy Decision") {

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
	protected void clearProxyDecision() {
		EscapeProxyWorkerSocket.clearProxyDecisionCache();
	}

	/**
	 *
	 */
	private String getFrameTitle() {
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
	private void addStatusListener(EscapeProxyConfig aEscapeProxyConfig) {

		aEscapeProxyConfig.addStatusListener(new PortStatusListener() {

			@Override
			public void statusChanged(boolean aOkFlag, @SuppressWarnings("unused") String aText) {
				EventQueue.invokeLater(new Runnable() {

					@Override
					public void run() {
						escapeProxyFrame.setIconImage(trayImage[aOkFlag ? 1 : 0]);
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
	private synchronized Properties readConfig() {
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
	private File getFile() {
		final File tempFile = new File(System.getProperty("user.home") + "/" + "escape-from-intranet.xml");
		return tempFile;
	}

	private Image[] trayImage;

	private Properties previouslySavedProperties;

	/**
	 * @param aEscapeProxyConfig
	 */
	private void initSystemTray(EscapeProxyConfig aEscapeProxyConfig) {
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
								escapeProxyFrame.setVisible(true);
								escapeProxyFrame.toFront();
								escapeProxyFrame.setState(Frame.NORMAL);
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
							escapeProxyFrame.setIconImage(trayImage[aOkFlag ? 1 : 0]);
						}
					});
				}
			});
		}
	}

	/**
	 * @throws IOException
	 */
	private Image getImage(String aString) throws IOException {
		final InputStream tempIn = EscapeProxy.class.getClassLoader().getResourceAsStream(aString);
		final BufferedImage tempImage = ImageIO.read(tempIn);
		return tempImage;
	}

	/**
	 * @param aEscapeProxyConfig
	 */
	private void setVisible(EscapeProxyConfig aEscapeProxyConfig) {
		EventQueue.invokeLater(new Runnable() {
			/**
			 *
			 */
			@Override
			public void run() {
				escapeProxyFrame.setIconImage(trayImage[0]);
				escapeProxyFrame.setVisible(true);
				final String tempProxyUser = aEscapeProxyConfig.getProxyUser();
				if (tempProxyUser != null && tempProxyUser.length() > 0) {
					escapeProxyFrame.setState(Frame.ICONIFIED);
				}
			}
		});
	}

	private void shutdownAndExit() {
		escapeProxyFrame.setVisible(false);
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
	private void initWindowListener() {
		escapeProxyFrame.addWindowListener(new WindowAdapter() {

			/**
			 * shutdown and exit if no other action is set
			 */
			@Override
			public void windowClosing(@SuppressWarnings("unused") WindowEvent aE) {
				if (escapeProxyFrame.getShutdownAndExitAction() == null) {
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
					escapeProxyFrame.setVisible(false);
				}
			}
		});
	}

	/**
	 *
	 */
	private synchronized void saveConfig() {
		if (isApplicationIsExiting()) {
			LOGGER.info("Not saving as already Sytem.exit() is in progress");
			return;
		}
		if (previouslySavedProperties == null || !previouslySavedProperties.equals(properties)) {
			saveConfigFile();
		} else {
			LOGGER.debug("Skip saving same properties");
		}
		previouslySavedProperties = new Properties();
		previouslySavedProperties.putAll(properties);
	}

	private void saveConfigFile() {
		try {
			final File tempFile = getFile();
			LOGGER.info("Save config " + tempFile.getAbsolutePath() + " ...");
			final OutputStream tempOut = new FileOutputStream(tempFile);
			properties.storeToXML(tempOut, "");
			tempOut.close();
			LOGGER.info("Saved config " + tempFile.getAbsolutePath());
		} catch (final IOException e) {
			LOGGER.error("error", e);
		}
	}

	/**
	 * @see #applicationIsExiting
	 */
	public synchronized boolean isApplicationIsExiting() {
		return applicationIsExiting;
	}

	/**
	 * @see #applicationIsExiting
	 */
	public synchronized void setApplicationIsExiting(boolean aApplicationIsExiting) {
		applicationIsExiting = aApplicationIsExiting;
	}
}
