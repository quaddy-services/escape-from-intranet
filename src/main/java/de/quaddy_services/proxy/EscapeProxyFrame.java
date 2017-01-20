package de.quaddy_services.proxy;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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

/**
 *
 */
public class EscapeProxyFrame extends JFrame {
	/**
	 *
	 */
	private static final JPasswordField PASSWORD = new JPasswordField();

	/**
	 *
	 */
	private static final JTextField HOST = new JTextField();

	/**
	 *
	 */
	private static final JTextField USER = new JTextField();

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private EscapeProxyConfig config;

	/**
	 * @param aEscapeProxyConfig
	 *
	 */
	public EscapeProxyFrame(EscapeProxyConfig aEscapeProxyConfig) {
		super("escape-from-intranet");
		config = aEscapeProxyConfig;

		initGui();
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

		add(createLabel("Host:"), tempGbc1);
		add(HOST, tempGbc2);
		addSetterGetter(HOST, config::setProxyHost, config::getProxyHost);

		tempGbc1.gridy++;
		tempGbc2.gridy++;

		add(createLabel("User:"), tempGbc1);
		add(USER, tempGbc2);
		addSetterGetter(USER, config::setProxyUser, config::getProxyUser);

		tempGbc1.gridy++;
		tempGbc2.gridy++;

		add(createLabel("Password:"), tempGbc1);
		add(PASSWORD, tempGbc2);
		addSetterGetter(PASSWORD, config::setProxyPassword, config::getProxyPassword);

		tempGbc1.gridy++;
		tempGbc2.gridy++;

		tempGbc1.gridwidth=2;
		tempGbc1.weighty=1;
		tempGbc1.fill=GridBagConstraints.BOTH;
		add(new JTextArea("Log"),tempGbc1);
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
