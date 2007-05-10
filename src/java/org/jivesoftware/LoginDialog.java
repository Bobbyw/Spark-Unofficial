/**
 * $Revision: $
 * $Date: $
 *
 * Copyright (C) 2006 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Lesser Public License (LGPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.jivesoftware.resource.Default;
import org.jivesoftware.resource.Res;
import org.jivesoftware.resource.SparkRes;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.spark.SessionManager;
import org.jivesoftware.spark.SparkManager;
import org.jivesoftware.spark.Workspace;
import org.jivesoftware.spark.component.RolloverButton;
import org.jivesoftware.spark.util.DummySSLSocketFactory;
import org.jivesoftware.spark.util.Encryptor;
import org.jivesoftware.spark.util.GraphicUtils;
import org.jivesoftware.spark.util.ModelUtil;
import org.jivesoftware.spark.util.ResourceUtils;
import org.jivesoftware.spark.util.SwingWorker;
import org.jivesoftware.spark.util.log.Log;
import org.jivesoftware.sparkimpl.plugin.layout.LayoutSettings;
import org.jivesoftware.sparkimpl.plugin.layout.LayoutSettingsManager;
import org.jivesoftware.sparkimpl.settings.local.LocalPreferences;
import org.jivesoftware.sparkimpl.settings.local.SettingsManager;

import javax.security.auth.Subject;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.security.Principal;
import java.util.Iterator;
import java.util.List;

/**
 * Dialog to log in a user into the Spark Server. The LoginDialog is used only
 * for login in registered users into the Spark Server.
 */
public final class LoginDialog {
    private JFrame loginDialog;
    private static final String BUTTON_PANEL = "buttonpanel"; // NOTRANS
    private static final String PROGRESS_BAR = "progressbar"; // NOTRANS
    private LocalPreferences localPref;

    /**
     * Empty Constructor
     */
    public LoginDialog() {
        localPref = SettingsManager.getLocalPreferences();

        // Check if upgraded needed.
        try {
            checkForOldSettings();
        }
        catch (Exception e) {
            Log.error(e);
        }
    }

    /**
     * Invokes the LoginDialog to be visible.
     *
     * @param parentFrame the parentFrame of the Login Dialog. This is used
     *                    for correct parenting.
     */
    public void invoke(JFrame parentFrame) {
        // Before creating any connections. Update proxy if needed.
        try {
            updateProxyConfig();
        }
        catch (Exception e) {
            Log.error(e);
        }

        LoginPanel loginPanel = new LoginPanel();

        // Construct Dialog
        loginDialog = new JFrame(Default.getString(Default.APPLICATION_NAME));

        loginDialog.setIconImage(SparkRes.getImageIcon(SparkRes.MAIN_IMAGE).getImage());

        final JPanel mainPanel = new LoginBackgroundPanel();
        final GridBagLayout mainLayout = new GridBagLayout();
        mainPanel.setLayout(mainLayout);

        final ImagePanel imagePanel = new ImagePanel();

        mainPanel.add(imagePanel,
                new GridBagConstraints(0, 0, 4, 1,
                        1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 0), 0, 0));

        final String showPoweredBy = Default.getString(Default.SHOW_POWERED_BY);
        if (ModelUtil.hasLength(showPoweredBy) && "true".equals(showPoweredBy)) {
            // Handle Powered By for custom clients.
            final JLabel poweredBy = new JLabel(SparkRes.getImageIcon(SparkRes.POWERED_BY_IMAGE));
            mainPanel.add(poweredBy,
                    new GridBagConstraints(0, 1, 4, 1,
                            1.0, 0.0, GridBagConstraints.NORTHEAST, GridBagConstraints.HORIZONTAL,
                            new Insets(0, 0, 2, 0), 0, 0));

        }

        loginPanel.setOpaque(false);
        mainPanel.add(loginPanel,
                new GridBagConstraints(0, 2, 2, 1,
                        1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 0), 0, 0));

        loginDialog.setContentPane(mainPanel);
        loginDialog.setLocationRelativeTo(parentFrame);

        loginDialog.setResizable(false);
        loginDialog.pack();

        // Center dialog on screen
        GraphicUtils.centerWindowOnScreen(loginDialog);

        // Show dialog
        loginDialog.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                quitLogin();
            }
        });
        if (loginPanel.getUsername().trim().length() > 0) {
            loginPanel.getPasswordField().requestFocus();
        }

        if (!localPref.isStartedHidden() || !localPref.isAutoLogin()) {
            // Make dialog top most.
            loginDialog.setVisible(true);
        }
    }

    /**
     * Define Login Panel implementation.
     */
    private final class LoginPanel extends JPanel implements KeyListener, ActionListener, FocusListener {
        private final JLabel usernameLabel = new JLabel();
        private final JTextField usernameField = new JTextField();

        private final JLabel passwordLabel = new JLabel();
        private final JPasswordField passwordField = new JPasswordField();

        private final JLabel serverLabel = new JLabel();
        private final JTextField serverField = new JTextField();

        private final JCheckBox savePasswordBox = new JCheckBox();
        private final JCheckBox autoLoginBox = new JCheckBox();
        private final RolloverButton loginButton = new RolloverButton();
        private final RolloverButton advancedButton = new RolloverButton();
        private final RolloverButton quitButton = new RolloverButton();

        private final RolloverButton createAccountButton = new RolloverButton();

        private final JLabel progressBar = new JLabel();

        // Panel used to hold buttons
        private final CardLayout cardLayout = new CardLayout(0, 5);
        final JPanel cardPanel = new JPanel(cardLayout);

        final JPanel buttonPanel = new JPanel(new GridBagLayout());
        private final GridBagLayout GRIDBAGLAYOUT = new GridBagLayout();
        private XMPPConnection connection = null;

        private JLabel headerLabel = new JLabel();
        private JLabel accountLabel = new JLabel();
        private JLabel accountNameLabel = new JLabel();
        private JLabel serverNameLabel = new JLabel();
        private JLabel ssoServerLabel = new JLabel();


        LoginPanel() {
            //setBorder(BorderFactory.createTitledBorder("Sign In Now"));
            ResourceUtils.resButton(savePasswordBox, Res.getString("checkbox.save.password"));
            ResourceUtils.resButton(autoLoginBox, Res.getString("checkbox.auto.login"));
            ResourceUtils.resLabel(serverLabel, serverField, Res.getString("label.server"));
            ResourceUtils.resButton(createAccountButton, Res.getString("label.accounts"));

            savePasswordBox.setOpaque(false);
            autoLoginBox.setOpaque(false);
            setLayout(GRIDBAGLAYOUT);

            // Set default visibility
            headerLabel.setVisible(false);
            accountLabel.setVisible(false);
            accountNameLabel.setVisible(false);
            serverNameLabel.setVisible(false);

            headerLabel.setText("Using Single Sign-On (SSO)");
            headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD));
            accountLabel.setText("Account:");
            ssoServerLabel.setText("Server:");
            accountNameLabel.setFont(accountLabel.getFont().deriveFont(Font.BOLD));
            serverNameLabel.setFont(ssoServerLabel.getFont().deriveFont(Font.BOLD));


            accountNameLabel.setForeground(new Color(106, 127, 146));
            serverNameLabel.setForeground(new Color(106, 127, 146));

            add(headerLabel,
                    new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0,
                            GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
            add(accountLabel,
                    new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                            GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
            add(accountNameLabel,
                    new GridBagConstraints(1, 1, 1, 1,
                            1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                            new Insets(5, 5, 0, 5), 0, 0));

            add(usernameLabel,
                    new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                            GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
            add(usernameField,
                    new GridBagConstraints(1, 1, 2, 1,
                            1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                            new Insets(5, 5, 0, 5), 0, 0));

            add(passwordField,
                    new GridBagConstraints(1, 2, 2, 1,
                            1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                            new Insets(5, 5, 0, 5), 0, 0));
            add(passwordLabel,
                    new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                            GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 5, 0));

            // Add Server Field Properties
            add(serverField,
                    new GridBagConstraints(1, 4, 2, 1,
                            1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                            new Insets(5, 5, 0, 5), 0, 0));

            add(serverNameLabel,
                    new GridBagConstraints(1, 4, 2, 1,
                            1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                            new Insets(5, 5, 0, 5), 0, 0));

            add(serverLabel,
                    new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
                            GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 5, 0));

            add(ssoServerLabel,
                    new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
                            GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 5, 0));


            add(savePasswordBox,
                    new GridBagConstraints(1, 5, 2, 1, 1.0, 0.0,
                            GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
            add(autoLoginBox,
                    new GridBagConstraints(1, 6, 2, 1, 1.0, 0.0,
                            GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));

            // Add button but disable the login button initially
            savePasswordBox.addActionListener(this);
            autoLoginBox.addActionListener(this);


            if (!"true".equals(Default.getString(Default.ACCOUNT_DISABLED))) {
                buttonPanel.add(createAccountButton,
                        new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                                GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
            }
            buttonPanel.add(advancedButton,
                    new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                            GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
            buttonPanel.add(loginButton,
                    new GridBagConstraints(3, 0, 4, 1, 1.0, 0.0,
                            GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));

            cardPanel.add(buttonPanel, BUTTON_PANEL);

            cardPanel.setOpaque(false);
            buttonPanel.setOpaque(false);

            ImageIcon icon = new ImageIcon(getClass().getClassLoader().getResource("images/ajax-loader.gif"));
            progressBar.setIcon(icon);
            cardPanel.add(progressBar, PROGRESS_BAR);


            add(cardPanel, new GridBagConstraints(0, 8, 4, 1,
                    1.0, 1.0, GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(2, 2, 2, 2), 0, 0));
            loginButton.setEnabled(false);

            // Add KeyListener
            usernameField.addKeyListener(this);
            passwordField.addKeyListener(this);
            serverField.addKeyListener(this);

            passwordField.addFocusListener(this);
            usernameField.addFocusListener(this);
            serverField.addFocusListener(this);

            // Add ActionListener
            quitButton.addActionListener(this);
            loginButton.addActionListener(this);
            advancedButton.addActionListener(this);

            // Make same size
            GraphicUtils.makeSameSize(usernameField, passwordField);

            // Set progress bar description
            progressBar.setText(Res.getString("message.autenticating"));
            progressBar.setVerticalTextPosition(JLabel.BOTTOM);
            progressBar.setHorizontalTextPosition(JLabel.CENTER);
            progressBar.setHorizontalAlignment(JLabel.CENTER);

            // Set Resources
            ResourceUtils.resLabel(usernameLabel, usernameField, Res.getString("label.username"));
            ResourceUtils.resLabel(passwordLabel, passwordField, Res.getString("label.password"));
            ResourceUtils.resButton(quitButton, Res.getString("button.quit"));
            ResourceUtils.resButton(loginButton, Res.getString("title.login"));
            ResourceUtils.resButton(advancedButton, Res.getString("button.advanced"));

            // Load previous instances
            String userProp = localPref.getUsername();
            String serverProp = localPref.getServer();

            if (userProp != null && serverProp != null) {
                usernameField.setText(StringUtils.unescapeNode(userProp));
                serverField.setText(serverProp);
                serverNameLabel.setText(serverProp);
            }

            // Check Settings
            if (localPref.isSavePassword()) {
                String encryptedPassword = localPref.getPassword();
                if (encryptedPassword != null) {
                    String password = Encryptor.decrypt(encryptedPassword);
                    passwordField.setText(password);
                }
                savePasswordBox.setSelected(true);
                loginButton.setEnabled(true);
            }
            autoLoginBox.setSelected(localPref.isAutoLogin());
            if (autoLoginBox.isSelected()) {
                savePasswordBox.setEnabled(false);
                autoLoginBox.setEnabled(false);
                validateLogin();
                return;
            }

            // Handle arguments
            String username = Spark.getArgumentValue("username");
            String password = Spark.getArgumentValue("password");
            String server = Spark.getArgumentValue("server");

            if (username != null) {
                usernameField.setText(username);
            }

            if (password != null) {
                passwordField.setText(password);
            }

            if (server != null) {
                serverField.setText(server);
            }

            if (username != null && server != null && password != null) {
                savePasswordBox.setEnabled(false);
                autoLoginBox.setEnabled(false);
                validateLogin();
            }

            createAccountButton.addActionListener(this);

            final String lockedDownURL = Default.getString(Default.HOST_NAME);
            if (ModelUtil.hasLength(lockedDownURL)) {
                serverField.setEnabled(false);
                serverField.setText(lockedDownURL);
            }

            useSSO(localPref.isSSOEnabled());
        }

        /**
         * Returns the username the user defined.
         *
         * @return the username.
         */
        private String getUsername() {
            return StringUtils.escapeNode(usernameField.getText().trim());
        }

        /**
         * Returns the password specified by the user.
         *
         * @return the password.
         */
        private String getPassword() {
            return new String(passwordField.getPassword());
        }

        /**
         * Returns the server name specified by the user.
         *
         * @return the server name.
         */
        private String getServerName() {
            return serverField.getText().trim();
        }

        /**
         * ActionListener implementation.
         *
         * @param e the ActionEvent
         */
        public void actionPerformed(ActionEvent e) {

            if (e.getSource() == quitButton) {
                quitLogin();
            }
            else if (e.getSource() == createAccountButton) {
                AccountCreationWizard createAccountPanel = new AccountCreationWizard();
                createAccountPanel.invoke(loginDialog);

                if (createAccountPanel.isRegistered()) {
                    usernameField.setText(createAccountPanel.getUsername());
                    passwordField.setText(createAccountPanel.getPassword());
                    serverField.setText(createAccountPanel.getServer());
                    loginButton.setEnabled(true);
                }
            }
            else if (e.getSource() == loginButton) {
                validateLogin();
            }
            else if (e.getSource() == advancedButton) {
                final LoginSettingDialog loginSettingsDialog = new LoginSettingDialog();
                loginSettingsDialog.invoke(loginDialog);
                useSSO(localPref.isSSOEnabled());
            }
            else if (e.getSource() == savePasswordBox) {
                autoLoginBox.setEnabled(savePasswordBox.isSelected());

                if (!savePasswordBox.isSelected()) {
                    autoLoginBox.setSelected(false);
                }
            }
            else if (e.getSource() == autoLoginBox) {
                if (autoLoginBox.isSelected()) {
                    savePasswordBox.setSelected(true);
                }
            }
        }

        /**
         * KeyListener implementation.
         *
         * @param e the KeyEvent to process.
         */
        public void keyTyped(KeyEvent e) {
            validate(e);
        }

        public void keyPressed(KeyEvent e) {

            // Do nothing.
        }

        public void keyReleased(KeyEvent e) {
            validateDialog();
        }

        /**
         * Checks the users input and enables/disables the login button depending on state.
         */
        private void validateDialog() {
            loginButton.setEnabled(ModelUtil.hasLength(getUsername()) && ModelUtil.hasLength(getPassword())
                    && ModelUtil.hasLength(getServerName()));
        }

        /**
         * Validates key input.
         *
         * @param e the keyEvent.
         */
        private void validate(KeyEvent e) {
            if (loginButton.isEnabled() && e.getKeyChar() == KeyEvent.VK_ENTER) {
                validateLogin();
            }
        }

        public void focusGained(FocusEvent e) {
            Object o = e.getSource();
            if (o instanceof JTextComponent) {
                ((JTextComponent)o).selectAll();
            }
        }

        public void focusLost(FocusEvent e) {
        }

        /**
         * Enables/Disables the editable components in the login screen.
         *
         * @param editable true to enable components, otherwise false to disable.
         */
        private void enableComponents(boolean editable) {

            // Need to set both editable and enabled for best behavior.
            usernameField.setEditable(editable);
            usernameField.setEnabled(editable);

            passwordField.setEditable(editable);
            passwordField.setEnabled(editable);

            serverField.setEditable(editable);
            serverField.setEnabled(editable);

            if (editable) {
                // Reapply focus to username field
                passwordField.requestFocus();
            }
        }

        /**
         * Displays the progress bar.
         *
         * @param visible true to display progress bar, false to hide it.
         */
        private void setProgressBarVisible(boolean visible) {
            if (visible) {
                cardLayout.show(cardPanel, PROGRESS_BAR);
                // progressBar.setIndeterminate(true);
            }
            else {
                cardLayout.show(cardPanel, BUTTON_PANEL);
            }
        }

        /**
         * Validates the users login information.
         */
        private void validateLogin() {
            final SwingWorker loginValidationThread = new SwingWorker() {
                public Object construct() {

                    boolean loginSuccessfull = login();
                    if (loginSuccessfull) {
                        progressBar.setText(Res.getString("message.connecting.please.wait"));

                        // Startup Spark
                        startSpark();

                        // dispose login dialog
                        loginDialog.dispose();

                        // Show ChangeLog if we need to.
                        // new ChangeLogDialog().showDialog();
                    }
                    else {
                        savePasswordBox.setEnabled(true);
                        autoLoginBox.setEnabled(true);
                        enableComponents(true);
                        setProgressBarVisible(false);
                    }
                    return Boolean.valueOf(loginSuccessfull);
                }
            };

            // Start the login process in seperate thread.
            // Disable textfields
            enableComponents(false);

            // Show progressbar
            setProgressBarVisible(true);

            loginValidationThread.start();
        }

        public JPasswordField getPasswordField() {
            return passwordField;
        }

        public Dimension getPreferredSize() {
            final Dimension dim = super.getPreferredSize();
            dim.height = 200;
            return dim;
        }

        public void useSSO(boolean use) {
            if (use) {
                usernameField.setVisible(false);
                passwordField.setVisible(false);
                savePasswordBox.setVisible(false);
                usernameLabel.setVisible(false);
                passwordLabel.setVisible(false);
                serverField.setVisible(false);
                autoLoginBox.setVisible(false);
                serverLabel.setVisible(false);

                headerLabel.setVisible(true);
                accountLabel.setVisible(true);
                accountNameLabel.setVisible(true);
                serverNameLabel.setVisible(true);
                ssoServerLabel.setVisible(true);


                String server = localPref.getServer();
                if (ModelUtil.hasLength(server)) {
                    serverNameLabel.setText(localPref.getServer());
                    serverField.setText(localPref.getServer());
                }

                System.setProperty("java.security.krb5.debug", "true");
                System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");
                GSSAPIConfiguration config = new GSSAPIConfiguration();
                Configuration.setConfiguration(config);

                LoginContext lc = null;
                try {
                    lc = new LoginContext("GetPrincipal");
                    lc.login();
                    Subject mySubject = lc.getSubject();


                    for (Principal p : mySubject.getPrincipals()) {
                        String name = p.getName();
                        int indexOne = name.indexOf("@");
                        if (indexOne != -1) {
                            String realmName = name.substring(0, indexOne);
                            accountNameLabel.setText(realmName);
                            usernameField.setText(realmName);
                        }
                        loginButton.setEnabled(true);
                    }
                }
                catch (LoginException le) {
                    Log.debug(le.getMessage());
                    useSSO(false);
                }
            }
            else {
                autoLoginBox.setVisible(true);
                usernameField.setVisible(true);
                passwordField.setVisible(true);
                savePasswordBox.setVisible(true);
                usernameLabel.setVisible(true);
                passwordLabel.setVisible(true);
                serverLabel.setVisible(true);
                serverField.setVisible(true);

                headerLabel.setVisible(false);
                accountLabel.setVisible(false);
                serverNameLabel.setVisible(false);
                accountNameLabel.setVisible(false);

                System.setProperty("java.security.krb5.debug", "false");
                System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");
                Configuration.setConfiguration(null);

                validateDialog();
            }


        }

        /**
         * Login to the specified server using username, password, and workgroup.
         * Handles error representation as well as logging.
         *
         * @return true if login was successful, false otherwise
         */
        private boolean login() {
            final SessionManager sessionManager = SparkManager.getSessionManager();


            boolean hasErrors = false;
            String errorMessage = null;

            // Handle specifyed Workgroup
            String serverName = getServerName();

            if (!hasErrors) {
                localPref = SettingsManager.getLocalPreferences();

                SmackConfiguration.setPacketReplyTimeout(localPref.getTimeOut() * 1000);

                // Get connection
                try {
                    int port = localPref.getXmppPort();

                    int checkForPort = serverName.indexOf(":");
                    if (checkForPort != -1) {
                        String portString = serverName.substring(checkForPort + 1);
                        if (ModelUtil.hasLength(portString)) {
                            // Set new port.
                            port = Integer.valueOf(portString).intValue();
                        }
                    }

                    boolean useSSL = localPref.isSSL();
                    boolean hostPortConfigured = localPref.isHostAndPortConfigured();

                    ConnectionConfiguration config = null;

                    if (useSSL) {
                        if (!hostPortConfigured) {
                            config = new ConnectionConfiguration(serverName, 5223);
                            config.setSocketFactory(new DummySSLSocketFactory());
                        }
                        else {
                            config = new ConnectionConfiguration(localPref.getXmppHost(), port, serverName);
                            config.setSocketFactory(new DummySSLSocketFactory());
                        }
                    }
                    else {
                        if (!hostPortConfigured) {
                            config = new ConnectionConfiguration(serverName);
                        }
                        else {
                            config = new ConnectionConfiguration(localPref.getXmppHost(), port, serverName);
                        }


                    }

                    if (config != null) {
                        config.setReconnectionAllowed(true);
                        boolean compressionEnabled = localPref.isCompressionEnabled();
                        config.setCompressionEnabled(compressionEnabled);
                        connection = new XMPPConnection(config);
                    }

                    connection.connect();

                    String resource = localPref.getResource();
                    if (!ModelUtil.hasLength(resource)) {
                        resource = "spark";
                    }
                    connection.login(getUsername(), getPassword(), resource, false);

                    sessionManager.setServerAddress(connection.getServiceName());
                    sessionManager.initializeSession(connection, getUsername(), getPassword());
                    sessionManager.setJID(connection.getUser());

                }
                catch (Exception xee) {
                    if (!loginDialog.isVisible()) {
                        loginDialog.setVisible(true);
                    }
                    if (xee instanceof XMPPException) {
                        XMPPException xe = (XMPPException)xee;
                        final XMPPError error = xe.getXMPPError();
                        int errorCode = 0;
                        if (error != null) {
                            errorCode = error.getCode();
                        }
                        if (errorCode == 401) {
                            errorMessage = SparkRes.getString(SparkRes.INVALID_USERNAME_PASSWORD);
                        }
                        else if (errorCode == 502 || errorCode == 504) {
                            errorMessage = SparkRes.getString(SparkRes.SERVER_UNAVAILABLE);
                        }
                        else if (errorCode == 409) {
                            errorMessage = Res.getString("label.conflict.error");
                        }
                        else {
                            errorMessage = SparkRes.getString(SparkRes.UNRECOVERABLE_ERROR);
                        }
                    }
                    else {
                        errorMessage = SparkRes.getString(SparkRes.UNRECOVERABLE_ERROR);
                    }

                    // Log Error
                    Log.warning("Exception in Login:", xee);
                    hasErrors = true;
                }
            }
            if (hasErrors) {
                progressBar.setVisible(false);
                //progressBar.setIndeterminate(false);

                // Show error dialog
                if (loginDialog.isVisible()) {

                    JOptionPane.showMessageDialog(loginDialog, errorMessage, SparkRes.getString(SparkRes.ERROR_DIALOG_TITLE),
                            JOptionPane.ERROR_MESSAGE);

                }
                setEnabled(true);
                return false;
            }

            // Since the connection and workgroup are valid. Add a ConnectionListener
            connection.addConnectionListener(SparkManager.getSessionManager());

            // Persist information
            localPref.setUsername(getUsername());

            // Check to see if the password should be saved.
            if (savePasswordBox.isSelected()) {
                String encodedPassword = null;
                try {
                    encodedPassword = Encryptor.encrypt(getPassword());
                    localPref.setPassword(encodedPassword);
                }
                catch (Exception e) {
                    Log.error("Error encrypting password.", e);
                }
            }
            else {
                localPref.setPassword("");
            }

            localPref.setSavePassword(savePasswordBox.isSelected());
            localPref.setAutoLogin(autoLoginBox.isSelected());
            localPref.setServer(serverField.getText());


            SettingsManager.saveSettings();

            return !hasErrors;
        }
    }

    /**
     * If the user quits, just shut down the
     * application.
     */
    private void quitLogin() {
        System.exit(1);
    }

    /**
     * Initializes Spark and initializes all plugins.
     */
    private void startSpark() {
        // Invoke the MainWindow.
        final MainWindow mainWindow = MainWindow.getInstance();

        /*
        if (tray != null) {
            // Remove trayIcon
            tray.removeTrayIcon(trayIcon);
        }
        */
        // Creates the Spark  Workspace and add to MainWindow
        Workspace workspace = Workspace.getInstance();

        LayoutSettings settings = LayoutSettingsManager.getLayoutSettings();
        int x = settings.getMainWindowX();
        int y = settings.getMainWindowY();
        int width = settings.getMainWindowWidth();
        int height = settings.getMainWindowHeight();

        LocalPreferences pref = SettingsManager.getLocalPreferences();
        if (pref.isDockingEnabled()) {
            JSplitPane splitPane = mainWindow.getSplitPane();
            workspace.getCardPanel().setMinimumSize(null);
            splitPane.setLeftComponent(workspace.getCardPanel());
            SparkManager.getChatManager().getChatContainer().setMinimumSize(null);
            splitPane.setRightComponent(SparkManager.getChatManager().getChatContainer());
            int dividerLoc = settings.getSplitPaneDividerLocation();
            if (dividerLoc != -1) {
                mainWindow.getSplitPane().setDividerLocation(dividerLoc);
            }
            else {
                mainWindow.getSplitPane().setDividerLocation(240);
            }

            mainWindow.getContentPane().add(splitPane, BorderLayout.CENTER);
        }
        else {
            mainWindow.getContentPane().add(workspace.getCardPanel(), BorderLayout.CENTER);
        }

        if (x == 0 && y == 0) {
            // Use Default size
            mainWindow.setSize(310, 520);

            // Center Window on Screen
            GraphicUtils.centerWindowOnScreen(mainWindow);
        }
        else {
            mainWindow.setBounds(x, y, width, height);
        }

        if (loginDialog.isVisible()) {
            mainWindow.setVisible(true);
        }

        loginDialog.setVisible(false);

        // Build the layout in the workspace
        workspace.buildLayout();
    }

    /**
     * Updates System properties with Proxy configuration.
     *
     * @throws Exception thrown if an exception occurs.
     */
    private void updateProxyConfig() throws Exception {
        if (ModelUtil.hasLength(Default.getString(Default.PROXY_PORT)) && ModelUtil.hasLength(Default.getString(Default.PROXY_HOST))) {
            String port = Default.getString(Default.PROXY_PORT);
            String host = Default.getString(Default.PROXY_HOST);
            System.setProperty("socksProxyHost", host);
            System.setProperty("socksProxyPort", port);
            return;
        }

        boolean proxyEnabled = localPref.isProxyEnabled();
        if (proxyEnabled) {
            String host = localPref.getHost();
            String port = localPref.getPort();
            String username = localPref.getProxyUsername();
            String password = localPref.getProxyPassword();
            String protocol = localPref.getProtocol();

            if (protocol.equals("SOCKS")) {
                System.setProperty("socksProxyHost", host);
                System.setProperty("socksProxyPort", port);

                if (ModelUtil.hasLength(username) && ModelUtil.hasLength(password)) {
                    System.setProperty("java.net.socks.username", username);
                    System.setProperty("java.net.socks.password", password);
                }
            }
            else {
                System.setProperty("http.proxyHost", host);
                System.setProperty("http.proxyPort", port);
                System.setProperty("https.proxyHost", host);
                System.setProperty("https.proxyPort", port);


                if (ModelUtil.hasLength(username) && ModelUtil.hasLength(password)) {
                    System.setProperty("http.proxyUser", username);
                    System.setProperty("http.proxyPassword", password);
                }

            }
        }
    }

    /**
     * Defines the background to use with the Login panel.
     */
    public class LoginBackgroundPanel extends JPanel {
        final ImageIcon icons = Default.getImageIcon(Default.LOGIN_DIALOG_BACKGROUND_IMAGE);

        /**
         * Empty constructor.
         */
        public LoginBackgroundPanel() {

        }

        /**
         * Uses an image to paint on background.
         *
         * @param g the graphics.
         */
        public void paintComponent(Graphics g) {
            Image backgroundImage = icons.getImage();
            double scaleX = getWidth() / (double)backgroundImage.getWidth(null);
            double scaleY = getHeight() / (double)backgroundImage.getHeight(null);
            AffineTransform xform = AffineTransform.getScaleInstance(scaleX, scaleY);
            ((Graphics2D)g).drawImage(backgroundImage, xform, this);
        }
    }

    /**
     * The image panel to display the Spark Logo.
     */
    public class ImagePanel extends JPanel {

        private final ImageIcon icons = Default.getImageIcon(Default.MAIN_IMAGE);

        /**
         * Uses the Spark logo to paint as the background.
         *
         * @param g the graphics to use.
         */
        public void paintComponent(Graphics g) {
            final Image backgroundImage = icons.getImage();
            final double scaleX = getWidth() / (double)backgroundImage.getWidth(null);
            final double scaleY = getHeight() / (double)backgroundImage.getHeight(null);
            AffineTransform xform = AffineTransform.getScaleInstance(scaleX, scaleY);
            ((Graphics2D)g).drawImage(backgroundImage, xform, this);
        }

        public Dimension getPreferredSize() {
            final Dimension size = super.getPreferredSize();
            size.width = icons.getIconWidth();
            size.height = icons.getIconHeight();
            return size;
        }
    }

    /**
     * Checks for historic Spark settings and upgrades the user.
     *
     * @throws Exception thrown if an error occurs.
     */
    private void checkForOldSettings() throws Exception {
        // Check for old settings.xml
        File settingsXML = new File(Spark.getUserSparkHome(), "/settings.xml");
        if (settingsXML.exists()) {
            SAXReader saxReader = new SAXReader();
            Document pluginXML = null;
            try {
                pluginXML = saxReader.read(settingsXML);
            }
            catch (DocumentException e) {
                Log.error(e);
                return;
            }

            List plugins = pluginXML.selectNodes("/settings");
            Iterator iter = plugins.iterator();
            while (iter.hasNext()) {
                Element plugin = (Element)iter.next();

                String password = plugin.selectSingleNode("password").getText();
                localPref.setPassword(password);

                String username = plugin.selectSingleNode("username").getText();
                localPref.setUsername(username);

                String server = plugin.selectSingleNode("server").getText();
                localPref.setServer(server);

                String autoLogin = plugin.selectSingleNode("autoLogin").getText();
                localPref.setAutoLogin(Boolean.parseBoolean(autoLogin));

                String savePassword = plugin.selectSingleNode("savePassword").getText();
                localPref.setSavePassword(Boolean.parseBoolean(savePassword));
            }

            // Delete settings File
            settingsXML.delete();
        }
    }


}
