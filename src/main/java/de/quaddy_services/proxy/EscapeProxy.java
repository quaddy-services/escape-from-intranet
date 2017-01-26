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
import javax.swing.Timer;
import javax.swing.WindowConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.quaddy_services.proxy.events.PortStatusListener;

/**
 *
 */
public class EscapeProxy {

	private static Logger LOGGER;

	public static void main(String[] args) throws IOException {
		initializeLogger();

		LOGGER.info("Start");
		Properties tempProperties = readConfig();

		EscapeProxyConfig tempEscapeProxyConfig = new EscapeProxyConfig(tempProperties);

		final EscapeProxyFrame tempEscapeProxyFrame = new EscapeProxyFrame(tempEscapeProxyConfig, getFrameTitle());

		tempEscapeProxyFrame.setSize(new Dimension(500, 400));
		tempEscapeProxyFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		Thread.setDefaultUncaughtExceptionHandler(new LoggingUncaughtExceptionHandler());
		// See java.awt.EventDispatchThread.handlerPropName
		System.setProperty("sun.awt.exception.handler", LoggingUncaughtExceptionHandler.class.getName());

		trayImage = new Image[] { getImage("offline.png"), getImage("online.png") };

		addExitListener(tempEscapeProxyFrame, tempProperties);
		initSystemTray(tempEscapeProxyFrame, tempEscapeProxyConfig);

		setVisible(tempEscapeProxyFrame, tempEscapeProxyConfig);
		addStatusListener(tempEscapeProxyFrame, tempEscapeProxyConfig);

		new EscapeProxyWorkerAccept(tempEscapeProxyConfig).start();
	}

	/**
	 *
	 */
	private static String getFrameTitle() {
		Properties tempProperties = new Properties();
		InputStream tempIn = EscapeProxy.class.getResourceAsStream("/META-INF/maven/de.quaddy_services/escape-from-intranet/pom.properties");
		if (tempIn != null) {
			try {
				tempProperties.load(tempIn);
				tempIn.close();
			} catch (IOException e) {
				LOGGER.error("Ignore readingerror pom.properties", e);
			}
		}
		String tempVersion = tempProperties.getProperty("version", "?");
		return "Escape from intranet proxy " + tempVersion;
	}

	/**
	 *
	 */
	private static void addStatusListener(EscapeProxyFrame aEscapeProxyFrame, EscapeProxyConfig aEscapeProxyConfig) {

		aEscapeProxyConfig.addStatusListener(new PortStatusListener() {

			@Override
			public void statusChanged(boolean aOkFlag, @SuppressWarnings("unused") String aText) {
				EventQueue.invokeLater(new Runnable() {

					@Override
					public void run() {
						aEscapeProxyFrame.setIconImage(trayImage[aOkFlag ? 1 : 0]);
					}
				});
			}
		});
	}

	/**
	 * Before loading logback.xml, set the properties
	 */
	private static void initializeLogger() {
		String tempDefaultLevel = System.getProperty("defaultLogLevel");
		if (tempDefaultLevel == null) {
			System.setProperty("defaultLogLevel", "info");
		}
		LOGGER = LoggerFactory.getLogger(EscapeProxy.class);
	}

	/**
	 * @return
	 *
	 */
	private static synchronized Properties readConfig() {
		Properties tempProperties = new Properties();
		File tempFile = getFile();
		if (tempFile.exists()) {
			try {
				FileInputStream tempIn = new FileInputStream(tempFile);
				tempProperties.loadFromXML(tempIn);
				tempIn.close();
				LOGGER.info("Loaded config " + tempFile.getAbsolutePath());
				LOGGER.debug("Content={}", tempProperties);
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

	private static Image[] trayImage;

	/**
	 * @param aEscapeProxyConfig
	 *
	 */
	private static void initSystemTray(EscapeProxyFrame aEscapeProxyFrame, EscapeProxyConfig aEscapeProxyConfig) {
		if (SystemTray.isSupported()) {
			EventQueue.invokeLater(new Runnable() {
				/**
				 * Init gui in eventqueue
				 */
				@Override
				public void run() {
					try {
						SystemTray tempSystemTray = SystemTray.getSystemTray();
						TrayIcon tempTrayIcon = new TrayIcon(trayImage[0]);
						tempTrayIcon.addMouseListener(new MouseAdapter() {
							@Override
							public void mouseClicked(@SuppressWarnings("unused") MouseEvent aE) {
								aEscapeProxyFrame.setVisible(true);
								aEscapeProxyFrame.toFront();
								aEscapeProxyFrame.setState(Frame.NORMAL);
							}
						});
						tempSystemTray.add(tempTrayIcon);
					} catch (AWTException e) {
						LOGGER.error("Error", e);
					}
				}
			});
			aEscapeProxyConfig.addStatusListener(new PortStatusListener() {
				private boolean currentStatus = false;

				@Override
				public void statusChanged(boolean aOkFlag, String aText) {
					if (SystemTray.isSupported()) {
						EventQueue.invokeLater(new Runnable() {
							@Override
							public void run() {
								TrayIcon tempTrayIcon = SystemTray.getSystemTray().getTrayIcons()[0];
								tempTrayIcon.setImageAutoSize(true);
								tempTrayIcon.setImage(trayImage[aOkFlag ? 1 : 0]);
								tempTrayIcon.setToolTip(aText);
							}
						});
						if (currentStatus ^ aOkFlag) {
							EventQueue.invokeLater(new Runnable() {
								@Override
								public void run() {
									TrayIcon tempTrayIcon = SystemTray.getSystemTray().getTrayIcons()[0];
									tempTrayIcon.displayMessage("Status change", aText, MessageType.INFO);
								}
							});
						}
						currentStatus = aOkFlag;
					}
					EventQueue.invokeLater(new Runnable() {
						@Override
						public void run() {
							aEscapeProxyFrame.setIconImage(trayImage[aOkFlag ? 1 : 0]);
						}
					});
				}
			});
		}
	}

	/**
	 * @throws IOException
	 *
	 */
	private static Image getImage(String aString) throws IOException {
		InputStream tempIn = EscapeProxy.class.getClassLoader().getResourceAsStream(aString);
		BufferedImage tempImage = ImageIO.read(tempIn);
		return tempImage;
	}

	/**
	 * @param aEscapeProxyConfig
	 *
	 */
	private static void setVisible(final EscapeProxyFrame tempEscapeProxyFrame, EscapeProxyConfig aEscapeProxyConfig) {
		EventQueue.invokeLater(new Runnable() {
			/**
			 *
			 */
			@Override
			public void run() {
				tempEscapeProxyFrame.setIconImage(trayImage[0]);
				tempEscapeProxyFrame.setVisible(true);
				String tempProxyUser = aEscapeProxyConfig.getProxyUser();
				if (tempProxyUser != null && tempProxyUser.length() > 0) {
					tempEscapeProxyFrame.setState(Frame.ICONIFIED);
				}
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
			public void windowClosing(@SuppressWarnings("unused") WindowEvent aE) {
				tempEscapeProxyFrame.setVisible(false);
				Timer tempTimer = new Timer(5000, new ActionListener() {

					@Override
					public void actionPerformed(@SuppressWarnings("unused") ActionEvent aE2) {
						saveConfig(aProperties);
						LOGGER.info("Exit");
						System.exit(0);
					}
				});
				tempTimer.start();
				if (SystemTray.isSupported()) {
					TrayIcon tempTrayIcon = SystemTray.getSystemTray().getTrayIcons()[0];
					tempTrayIcon.displayMessage("Shutdown", "localhost proxy\nis shutting down...", MessageType.WARNING);
				}
			}

			/**
			 *
			 */
			@Override
			public void windowDeactivated(WindowEvent aE) {
				super.windowDeactivated(aE);
				saveConfig(aProperties);
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
	protected synchronized static void saveConfig(Properties aProperties) {
		try {
			File tempFile = getFile();
			LOGGER.info("Save config " + tempFile.getAbsolutePath());
			OutputStream tempOut = new FileOutputStream(tempFile);
			aProperties.storeToXML(tempOut, "");
			tempOut.close();
		} catch (IOException e) {
			LOGGER.error("error", e);
		}
	}
}
