package de.quaddy_services.proxy;

import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import de.quaddy_services.proxy.events.PortStatusListener;

/**
 *
 */
public class EscapeProxyFrame extends JFrame {
	/**
	 *
	 */
	private static final JPasswordField PROXY_PASSWORD = new JPasswordField();

	/**
	 *
	 */
	private static final JTextField PROXY_HOST = new JTextField();

	/**
	 *
	 */
	private static final JTextField PROXY_USER = new JTextField();

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private static final JTextField LOCAL_PORT = new JTextField();

	private static final JLabel LOCAL_PORT_STATUS = new JLabel();

	private static final JTextField PROXY_PORT = new JTextField();

	private EscapeProxyConfig config;

	/**
	 * @param aEscapeProxyConfig
	 *
	 */
	public EscapeProxyFrame(EscapeProxyConfig aEscapeProxyConfig) {
		super("escape-from-intranet");
		config = aEscapeProxyConfig;

		config.addStatusListener(createStatusListener());

		initGui();
	}

	/**
	 *
	 */
	private PortStatusListener createStatusListener() {
		return new PortStatusListener() {
			@Override
			public void statusChanged(boolean aOkFlag, String aText) {
				EventQueue.invokeLater(new Runnable() {

					@Override
					public void run() {
						LOCAL_PORT_STATUS.setText(aText);
						if (aOkFlag) {
							LOCAL_PORT_STATUS.setBackground(Color.GREEN.brighter());
						} else {
							LOCAL_PORT_STATUS.setBackground(Color.RED.brighter());
						}
					}
				});
			}
		};
	}

	/**
	 *
	 */
	private void initGui() {
		GridBagConstraints tempGbc1 = new GridBagConstraints();
		tempGbc1.gridx = 0;
		tempGbc1.gridy = 0;
		tempGbc1.weightx = 1;
		tempGbc1.fill = GridBagConstraints.HORIZONTAL;
		tempGbc1.anchor = GridBagConstraints.EAST;

		GridBagConstraints tempGbc2 = new GridBagConstraints();
		tempGbc2.gridx = 1;
		tempGbc2.gridy = 0;
		tempGbc2.weightx = 2;
		tempGbc2.fill = GridBagConstraints.HORIZONTAL;
		tempGbc2.anchor = GridBagConstraints.WEST;

		setLayout(new GridBagLayout());

		add(createLabel("Listen on localhost:"), tempGbc1);
		add(LOCAL_PORT, tempGbc2);
		addSetterGetter(LOCAL_PORT, config::setLocalPort, config::getLocalPort);
		LOCAL_PORT.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(@SuppressWarnings("unused") FocusEvent aE) {
				config.fireCheckPortEvent();
			}
		});

		tempGbc1.gridy++;
		tempGbc2.gridy++;

		tempGbc1.gridwidth = 3;
		add(LOCAL_PORT_STATUS, tempGbc1);
		LOCAL_PORT_STATUS.setOpaque(true);
		tempGbc1.gridwidth = 1;

		tempGbc1.gridy++;
		tempGbc2.gridy++;

		add(createLabel("ProxyHost:"), tempGbc1);
		add(PROXY_HOST, tempGbc2);
		addSetterGetter(PROXY_HOST, config::setProxyHost, config::getProxyHost);

		tempGbc1.gridy++;
		tempGbc2.gridy++;

		add(createLabel("ProxyPort:"), tempGbc1);
		add(PROXY_PORT, tempGbc2);
		addSetterGetter(PROXY_PORT, config::setProxyPort, config::getProxyPort);

		tempGbc1.gridy++;
		tempGbc2.gridy++;

		add(createLabel("ProxyUser:"), tempGbc1);
		add(PROXY_USER, tempGbc2);
		addSetterGetter(PROXY_USER, config::setProxyUser, config::getProxyUser);

		tempGbc1.gridy++;
		tempGbc2.gridy++;

		add(createLabel("ProxyPassword:"), tempGbc1);
		add(PROXY_PASSWORD, tempGbc2);
		addSetterGetter(PROXY_PASSWORD, config::setProxyPassword, config::getProxyPassword);

		tempGbc1.gridy++;
		tempGbc2.gridy++;

		tempGbc1.gridwidth = 3;
		tempGbc1.weighty = 1;
		tempGbc1.fill = GridBagConstraints.BOTH;
		add(new JTextArea("Log"), tempGbc1);
	}

	/**
	 *
	 */
	private void addSetterGetter(JTextField aTextField, Consumer<String> aSetter, Supplier<String> aGetter) {
		aTextField.setText(aGetter.get());
		aTextField.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent aE) {
				aSetter.accept(aTextField.getText());
			}

			@Override
			public void keyReleased(KeyEvent aE) {
				aSetter.accept(aTextField.getText());
			}

			@Override
			public void keyPressed(KeyEvent aE) {
				aSetter.accept(aTextField.getText());
			}
		});
	}

	/**
	 *
	 */
	private Component createLabel(String aString) {
		JLabel tempJLabel = new JLabel(aString);
		tempJLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		return tempJLabel;
	}
}
