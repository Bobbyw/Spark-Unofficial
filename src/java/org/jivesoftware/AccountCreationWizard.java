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

import org.jivesoftware.resource.Res;
import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.spark.component.TitlePanel;
import org.jivesoftware.spark.util.DummySSLSocketFactory;
import org.jivesoftware.spark.util.ModelUtil;
import org.jivesoftware.spark.util.ResourceUtils;
import org.jivesoftware.spark.util.SwingWorker;
import org.jivesoftware.sparkimpl.settings.local.LocalPreferences;
import org.jivesoftware.sparkimpl.settings.local.SettingsManager;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JTextField;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AccountCreationWizard extends JPanel {
    private JLabel usernameLabel = new JLabel();
    private JTextField usernameField = new JTextField();

    private JLabel passwordLabel = new JLabel();
    private JPasswordField passwordField = new JPasswordField();

    private JLabel confirmPasswordLabel = new JLabel();
    private JPasswordField confirmPasswordField = new JPasswordField();

    private JLabel serverLabel = new JLabel();
    private JTextField serverField = new JTextField();

    private JButton createAccountButton = new JButton();
    private JButton closeButton = new JButton();

    private JDialog dialog;

    private boolean registered;
    private XMPPConnection connection = null;
    private JProgressBar progressBar;


    public AccountCreationWizard() {
        // Associate Mnemonics
        ResourceUtils.resLabel(usernameLabel, usernameField, Res.getString("label.username") + ":");
        ResourceUtils.resLabel(passwordLabel, passwordField, Res.getString("label.password") + ":");
        ResourceUtils.resLabel(confirmPasswordLabel, confirmPasswordField, Res.getString("label.confirm.password") + ":");
        ResourceUtils.resLabel(serverLabel, serverField, Res.getString("label.server") + ":");
        ResourceUtils.resButton(createAccountButton, Res.getString("button.create.account"));

        setLayout(new GridBagLayout());

        // Add component to UI
        add(usernameLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
        add(usernameField, new GridBagConstraints(1, 0, 3, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 150, 0));

        add(passwordLabel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
        add(passwordField, new GridBagConstraints(1, 1, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));

        add(confirmPasswordLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
        add(confirmPasswordField, new GridBagConstraints(1, 2, 3, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));

        add(serverLabel, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
        add(serverField, new GridBagConstraints(1, 3, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));

        progressBar = new JProgressBar();


        add(progressBar, new GridBagConstraints(1, 4, 3, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
        progressBar.setVisible(false);
        add(createAccountButton, new GridBagConstraints(2, 5, 1, 1, 1.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));


        ResourceUtils.resButton(closeButton, Res.getString("button.close"));
        add(closeButton, new GridBagConstraints(3, 5, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));


        createAccountButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                createAccount();
            }
        });

        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                dialog.dispose();
            }
        });
    }

    public String getUsername() {
        return StringUtils.escapeNode(usernameField.getText().toLowerCase());
    }

    public String getPassword() {
        return new String(passwordField.getPassword());
    }

    public String getConfirmPassword() {
        return new String(confirmPasswordField.getPassword());
    }

    public String getServer() {
        return serverField.getText();
    }

    public boolean isPasswordValid() {
        return getPassword().equals(getConfirmPassword());
    }

    public void createAccount() {
        boolean errors = false;
        String errorMessage = "";

        if (!ModelUtil.hasLength(getUsername())) {
            errors = true;
            usernameField.requestFocus();
            errorMessage = Res.getString("message.username.error");
        }
        else if (!ModelUtil.hasLength(getPassword())) {
            errors = true;
            errorMessage = Res.getString("message.password.error");
        }
        else if (!ModelUtil.hasLength(getConfirmPassword())) {
            errors = true;
            errorMessage = Res.getString("message.confirmation.password.error");
        }
        else if (!ModelUtil.hasLength(getServer())) {
            errors = true;
            errorMessage = Res.getString("message.account.error");
        }
        else if (!isPasswordValid()) {
            errors = true;
            errorMessage = Res.getString("message.confirmation.password.error");
        }

        if (errors) {
            JOptionPane.showMessageDialog(this, errorMessage, Res.getString("title.create.problem"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        final Component ui = this;
        progressBar.setIndeterminate(true);
        progressBar.setStringPainted(true);
        progressBar.setString(Res.getString("message.registering", getServer()));
        progressBar.setVisible(true);
        final SwingWorker worker = new SwingWorker() {
            int errorCode;


            public Object construct() {
                try {
                    createAccountButton.setEnabled(false);
                    connection = getConnection();
                }
                catch (XMPPException e) {
                    return e;
                }
                try {
                    final AccountManager accountManager = new AccountManager(connection);
                    accountManager.createAccount(getUsername(), getPassword());
                }
                catch (XMPPException e) {
                    XMPPError error = e.getXMPPError();
                    if (error != null) {
                        errorCode = error.getCode();
                    }
                    else {
                        errorCode = 500;
                    }

                }
                return "ok";
            }

            public void finished() {
                progressBar.setVisible(false);
                if (connection == null) {
                    if (ui.isShowing()) {
                        createAccountButton.setEnabled(true);
                        JOptionPane.showMessageDialog(ui, Res.getString("message.connection.failed", getServer()), Res.getString("title.create.problem"), JOptionPane.ERROR_MESSAGE);
                        createAccountButton.setEnabled(true);
                    }
                    return;
                }

                if (errorCode == 0) {
                    accountCreationSuccessful();
                }
                else {
                    accountCreationFailed(errorCode);
                }
            }
        };

        worker.start();
    }

    private void accountCreationFailed(int errorCode) {
        String message = Res.getString("message.create.account");
        if (errorCode == 409) {
            message = Res.getString("message.already.exists");
            usernameField.setText("");
            usernameField.requestFocus();
        }
        JOptionPane.showMessageDialog(this, message, Res.getString("title.create.problem"), JOptionPane.ERROR_MESSAGE);
        createAccountButton.setEnabled(true);
    }

    private void accountCreationSuccessful() {
        registered = true;
        JOptionPane.showMessageDialog(this, Res.getString("message.account.created"), Res.getString("title.account.created"), JOptionPane.INFORMATION_MESSAGE);
        dialog.dispose();
    }

    public void invoke(JFrame parent) {
        dialog = new JDialog(parent, Res.getString("title.create.new.account"), true);

        TitlePanel titlePanel = new TitlePanel(Res.getString("title.account.create.registration"), Res.getString("message.account.create"), null, true);
        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.getContentPane().add(titlePanel, BorderLayout.NORTH);
        dialog.getContentPane().add(this, BorderLayout.CENTER);
        dialog.pack();
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    private XMPPConnection getConnection() throws XMPPException {
        LocalPreferences localPref = SettingsManager.getLocalPreferences();
        XMPPConnection con = null;

        // Get connection

        int port = localPref.getXmppPort();

        String serverName = getServer();

        int checkForPort = serverName.indexOf(":");
        if (checkForPort != -1) {
            String portString = serverName.substring(checkForPort + 1);
            if (ModelUtil.hasLength(portString)) {
                // Set new port.
                port = Integer.valueOf(portString);
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
            con = new XMPPConnection(config);
        }

        if (con != null) {
            con.connect();
        }
        return con;

    }

    public boolean isRegistered() {
        return registered;
    }
}

