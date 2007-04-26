/**
 * $Revision: $
 * $Date: $
 *
 * Copyright (C) 2006 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Lesser Public License (LGPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.spark.ui.conferences;

import org.jivesoftware.resource.Res;
import org.jivesoftware.resource.SparkRes;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.spark.ChatManager;
import org.jivesoftware.spark.ChatNotFoundException;
import org.jivesoftware.spark.SparkManager;
import org.jivesoftware.spark.component.TitlePanel;
import org.jivesoftware.spark.ui.ChatRoom;
import org.jivesoftware.spark.ui.RosterPickList;
import org.jivesoftware.spark.ui.rooms.GroupChatRoom;
import org.jivesoftware.spark.util.ModelUtil;
import org.jivesoftware.spark.util.ResourceUtils;
import org.jivesoftware.spark.util.SwingWorker;
import org.jivesoftware.spark.util.log.Log;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

final class InvitationDialog extends JPanel {
    private JLabel roomsLabel = new JLabel();
    private JTextField roomsField = new JTextField();

    private JLabel messageLabel = new JLabel();
    private JTextField messageField = new JTextField();

    private JLabel inviteLabel = new JLabel();


    private DefaultListModel invitedUsers = new DefaultListModel();
    private JList invitedUserList = new JList(invitedUsers);

    private JDialog dlg;

    private GridBagLayout gridBagLayout1 = new GridBagLayout();

    public InvitationDialog() {
        setLayout(gridBagLayout1);

        add(roomsLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
        add(roomsField, new GridBagConstraints(1, 0, 3, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));


        add(messageLabel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
        add(messageField, new GridBagConstraints(1, 1, 3, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));

        JLabel jidLabel = new JLabel();
        final JTextField jidField = new JTextField();
        JButton addJIDButton = new JButton();
        JButton browseButton = new JButton();
        ResourceUtils.resButton(addJIDButton, Res.getString("button.add"));
        ResourceUtils.resButton(browseButton, Res.getString("button.roster"));
        ResourceUtils.resLabel(jidLabel, jidField, Res.getString("label.add.jid"));

        add(jidLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
        add(jidField, new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
        add(addJIDButton, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
        add(browseButton, new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));

        addJIDButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                String jid = jidField.getText();
                String server = StringUtils.parseBareAddress(jid);
                if (server == null || server.indexOf("@") == -1) {
                    JOptionPane.showMessageDialog(dlg, Res.getString("message.enter.valid.jid"), Res.getString("title.error"), JOptionPane.ERROR_MESSAGE);
                    jidField.setText("");
                    jidField.requestFocus();
                    return;
                }
                else {
                    if (!invitedUsers.contains(jid)) {
                        invitedUsers.addElement(jid);
                    }
                    jidField.setText("");
                }
            }
        });

        browseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                RosterPickList browser = new RosterPickList();
                Collection col = browser.showRoster(dlg);

                Iterator iter = col.iterator();
                while (iter.hasNext()) {
                    String jid = (String)iter.next();
                    if (!invitedUsers.contains(jid)) {
                        invitedUsers.addElement(jid);
                    }

                }
            }
        });


        add(inviteLabel, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
        add(new JScrollPane(invitedUserList), new GridBagConstraints(1, 3, 3, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));

        // Add Resource Utils
        ResourceUtils.resLabel(messageLabel, messageField, Res.getString("label.message") + ":");
        ResourceUtils.resLabel(roomsLabel, roomsField, Res.getString("label.room") + ":");
        inviteLabel.setText(Res.getString("label.invited.users"));

        messageField.setText(Res.getString("message.please.join.in.conference"));

        // Add Listener to list
        invitedUserList.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent mouseEvent) {
                if (mouseEvent.isPopupTrigger()) {
                    showPopup(mouseEvent);
                }
            }

            public void mousePressed(MouseEvent mouseEvent) {
                if (mouseEvent.isPopupTrigger()) {
                    showPopup(mouseEvent);
                }
            }
        });
    }

    private void showPopup(MouseEvent e) {
        final JPopupMenu popup = new JPopupMenu();
        final int index = invitedUserList.locationToIndex(e.getPoint());

        Action removeAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                invitedUsers.remove(index);
            }
        };

        removeAction.putValue(Action.NAME, Res.getString("menuitem.remove"));
        removeAction.putValue(Action.SMALL_ICON, SparkRes.getImageIcon(SparkRes.SMALL_DELETE));

        popup.add(removeAction);

        popup.show(invitedUserList, e.getX(), e.getY());

    }

    public void inviteUsersToRoom(final String serviceName, String roomName, Collection jids) {
        roomsField.setText(roomName);


        JFrame parent = SparkManager.getChatManager().getChatContainer().getChatFrame();
        if (parent == null || !parent.isVisible()) {
            parent = SparkManager.getMainWindow();
        }

        // Add jids to user list
        if (jids != null) {
            Iterator iter = jids.iterator();
            while (iter.hasNext()) {
                invitedUsers.addElement(iter.next());
            }
        }

        final JOptionPane pane;


        TitlePanel titlePanel;

        // Create the title panel for this dialog
        titlePanel = new TitlePanel(Res.getString("title.invite.to.conference"), Res.getString("message.invite.users.to.conference"), SparkRes.getImageIcon(SparkRes.BLANK_IMAGE), true);

        // Construct main panel w/ layout.
        final JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(titlePanel, BorderLayout.NORTH);

        // The user should only be able to close this dialog.
        Object[] options = {Res.getString("invite"), Res.getString("cancel")};
        pane = new JOptionPane(this, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, options, options[0]);

        mainPanel.add(pane, BorderLayout.CENTER);

        final JOptionPane p = new JOptionPane();

        dlg = p.createDialog(parent, Res.getString("title.conference.rooms"));
        dlg.setModal(false);

        dlg.pack();
        dlg.setSize(500, 450);
        dlg.setResizable(true);
        dlg.setContentPane(mainPanel);
        dlg.setLocationRelativeTo(parent);


        PropertyChangeListener changeListener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                String value = (String)pane.getValue();
                if (Res.getString("cancel").equals(value)) {
                    pane.setValue(JOptionPane.UNINITIALIZED_VALUE);
                    dlg.dispose();
                }
                else if (Res.getString("invite").equals(value)) {
                    final String roomTitle = roomsField.getText();
                    int size = invitedUserList.getModel().getSize();

                    if (size == 0) {
                        JOptionPane.showMessageDialog(dlg, Res.getString("message.specify.users.to.join.conference"), Res.getString("title.error"), JOptionPane.ERROR_MESSAGE);
                        pane.setValue(JOptionPane.UNINITIALIZED_VALUE);
                        return;
                    }

                    if (!ModelUtil.hasLength(roomTitle)) {
                        JOptionPane.showMessageDialog(dlg, Res.getString("message.no.room.to.join.error"), Res.getString("title.error"), JOptionPane.ERROR_MESSAGE);
                        pane.setValue(JOptionPane.UNINITIALIZED_VALUE);
                        return;
                    }
                    String roomName = "";

                    // Add all rooms the user is in to list.
                    ChatManager chatManager = SparkManager.getChatManager();
                    Iterator rooms = chatManager.getChatContainer().getChatRooms().iterator();
                    while (rooms.hasNext()) {
                        ChatRoom chatRoom = (ChatRoom)rooms.next();
                        if (chatRoom instanceof GroupChatRoom) {
                            GroupChatRoom groupRoom = (GroupChatRoom)chatRoom;
                            if (groupRoom.getRoomname().equals(roomTitle)) {
                                roomName = groupRoom.getMultiUserChat().getRoom();
                                break;
                            }
                        }
                    }
                    String message = messageField.getText();
                    final String messageText = message != null ? message : Res.getString("message.please.join.in.conference");

                    if (invitedUsers.getSize() > 0) {
                        invitedUserList.setSelectionInterval(0, invitedUsers.getSize() - 1);
                    }


                    GroupChatRoom chatRoom = null;
                    try {
                        chatRoom = SparkManager.getChatManager().getGroupChat(roomName);
                    }
                    catch (ChatNotFoundException e1) {
                        dlg.setVisible(false);
                        final List jidList = new ArrayList();
                        Object[] jids = invitedUserList.getSelectedValues();
                        final int no = jids != null ? jids.length : 0;
                        for (int i = 0; i < no; i++) {
                            jidList.add(jids[i]);
                        }

                        SwingWorker worker = new SwingWorker() {
                            public Object construct() {
                                try {
                                    Thread.sleep(15);
                                }
                                catch (InterruptedException e2) {
                                    Log.error(e2);
                                }
                                return "ok";
                            }

                            public void finished() {
                                try {
                                    ConferenceUtils.createPrivateConference(serviceName, messageText, roomTitle, jidList);
                                }
                                catch (XMPPException e2) {
                                    JOptionPane.showMessageDialog(pane, ConferenceUtils.getReason(e2), Res.getString("title.error"), JOptionPane.ERROR_MESSAGE);
                                }
                            }
                        };

                        worker.start();
                        pane.setValue(JOptionPane.UNINITIALIZED_VALUE);
                        return;
                    }

                    pane.setValue(JOptionPane.UNINITIALIZED_VALUE);
                    dlg.dispose();

                    Object[] values = invitedUserList.getSelectedValues();
                    final int no = values != null ? values.length : 0;
                    for (int i = 0; i < no; i++) {
                        String jid = (String)values[i];
                        chatRoom.getMultiUserChat().invite(jid, message != null ? message : Res.getString("message.please.join.in.conference"));
                        String nickname = SparkManager.getUserManager().getUserNicknameFromJID(jid);
                        chatRoom.getTranscriptWindow().insertNotificationMessage("Invited " + nickname, ChatManager.NOTIFICATION_COLOR);
                    }

                }
            }
        };

        pane.addPropertyChangeListener(changeListener);

        dlg.setVisible(true);
        dlg.toFront();
        dlg.requestFocus();
    }
}
