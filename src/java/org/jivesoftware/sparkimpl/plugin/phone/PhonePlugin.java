/**
 * $Revision: $
 * $Date: $
 *
 * Copyright (C) 2006 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Lesser Public License (LGPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.sparkimpl.plugin.phone;

import org.jivesoftware.phone.client.BasePhoneEventListener;
import org.jivesoftware.phone.client.HangUpEvent;
import org.jivesoftware.phone.client.OnPhoneEvent;
import org.jivesoftware.phone.client.PhoneActionException;
import org.jivesoftware.phone.client.PhoneClient;
import org.jivesoftware.phone.client.RingEvent;
import org.jivesoftware.phone.client.action.PhoneActionIQProvider;
import org.jivesoftware.phone.client.event.PhoneEventPacketExtensionProvider;
import org.jivesoftware.resource.Res;
import org.jivesoftware.resource.SparkRes;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.spark.SparkManager;
import org.jivesoftware.spark.plugin.ContextMenuListener;
import org.jivesoftware.spark.plugin.Plugin;
import org.jivesoftware.spark.ui.ChatRoom;
import org.jivesoftware.spark.ui.ChatRoomButton;
import org.jivesoftware.spark.ui.ChatRoomListenerAdapter;
import org.jivesoftware.spark.ui.ContactItem;
import org.jivesoftware.spark.ui.ContactList;
import org.jivesoftware.spark.ui.rooms.ChatRoomImpl;
import org.jivesoftware.spark.util.ModelUtil;
import org.jivesoftware.spark.util.ResourceUtils;
import org.jivesoftware.spark.util.SwingWorker;
import org.jivesoftware.spark.util.log.Log;
import org.jivesoftware.sparkimpl.plugin.alerts.SparkToaster;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

public class PhonePlugin implements Plugin {
    public static PhoneClient phoneClient;
    private DialPanel dialPanel;
//    private Alert incomingDialog;
    private JFrame dialDialog;

    public void initialize() {
        ProviderManager.addExtensionProvider("phone-event", "http://jivesoftware.com/xmlns/phone", new PhoneEventPacketExtensionProvider());
        ProviderManager.addIQProvider("phone-action", "http://jivesoftware.com/xmlns/phone", new PhoneActionIQProvider());

        final XMPPConnection con = SparkManager.getConnection();

        /*
        SwingWorker worker = new SwingWorker() {
            public Object construct() {
                try {
                    phoneClient = new PhoneClient(con);

                    // Add BaseListener
                    phoneClient.addEventListener(new PhoneListener());
                }
                catch (Exception e) {
                    // Ignore because the user does not have support.
                    //Log.debug(e);
                }
                return phoneClient;
            }

            public void finished() {
                if (phoneClient != null) {
                    setupPhoneSystem();
                }
            }
        };

        worker.start();
        */
    }

    private void setupPhoneSystem() {
        // Add Dial Menu
        final JMenu viewMenu = SparkManager.getMainWindow().getMenuByName(Res.getString("menuitem.actions"));
        JMenuItem dialNumberMenu = new JMenuItem(SparkRes.getImageIcon(SparkRes.ON_PHONE_IMAGE));
        ResourceUtils.resButton(dialNumberMenu, Res.getString("button.dial.number"));

        // Add Listener
        dialNumberMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dialPanel = new DialPanel();
                dialPanel.getDialButton().addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        String number = dialPanel.getNumberToDial();
                        if (ModelUtil.hasLength(number)) {
                            dialPanel.setText(Res.getString("message.calling", number));
                            dialPanel.changeToRinging();
                            callExtension(number);

                        }

                    }
                });

                dialDialog = PhoneDialog.invoke(dialPanel, Res.getString("title.dial.phone"), Res.getString("message.number.to.call"), null);
                dialPanel.getDialField().requestFocusInWindow();

                dialPanel.getDialField().addKeyListener(new KeyAdapter() {
                    public void keyPressed(KeyEvent e) {
                        if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                            try {
                                String number = dialPanel.getNumberToDial();
                                if (ModelUtil.hasLength(number)) {
                                    dialPanel.setText(Res.getString("message.calling", number));
                                    dialPanel.changeToRinging();
                                    callExtension(number);

                                }
                                e.consume();
                            }
                            catch (Exception ex) {
                                Log.error(ex);
                            }
                        }
                    }
                });
            }

        });
        viewMenu.add(dialNumberMenu);

        // Add ChatRoomListener to call users based on JID
        SparkManager.getChatManager().addChatRoomListener(new ChatRoomListenerAdapter() {
            public void chatRoomOpened(final ChatRoom room) {
                if (room instanceof ChatRoomImpl) {
                    final ChatRoomButton callButton = new ChatRoomButton("", SparkRes.getImageIcon(SparkRes.TELEPHONE_24x24));
                    callButton.setToolTipText(Res.getString("tooltip.place.a.call"));
                    final ChatRoomImpl chatRoom = (ChatRoomImpl)room;
                    boolean phoneEnabled = false;
                    try {
                        phoneEnabled = phoneClient.isPhoneEnabled(StringUtils.parseBareAddress(chatRoom.getParticipantJID()));
                    }
                    catch (Exception e) {
                        Log.error(e);
                    }

                    if (phoneEnabled) {
                        room.getToolBar().addChatRoomButton(callButton);
                        callButton.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                callJID(chatRoom.getParticipantJID());
                            }
                        });
                    }
                }
            }
        });

        ContactList contactList = SparkManager.getWorkspace().getContactList();
        contactList.addContextMenuListener(new ContextMenuListener() {
            public void poppingUp(Object object, final JPopupMenu popup) {
                if (object instanceof ContactItem) {
                    final ContactItem item = (ContactItem)object;

                    boolean phoneEnabled = false;


                    try {
                        phoneEnabled = phoneClient.isPhoneEnabled(item.getFullJID());
                    }
                    catch (Exception e) {
                        Log.error("There was an error retrieving phone information.", e);
                    }

                    if (phoneEnabled) {
                        Action callAction = new AbstractAction() {
                            public void actionPerformed(ActionEvent e) {
                                callJID(item.getFullJID());
                            }
                        };

                        callAction.putValue(Action.NAME, "Call");
                        callAction.putValue(Action.SMALL_ICON, SparkRes.getImageIcon(SparkRes.ON_PHONE_IMAGE));
                        popup.add(callAction);
                    }
                }
            }

            public void poppingDown(JPopupMenu popup) {

            }

            public boolean handleDefaultAction(MouseEvent e) {
                return false;
            }
        });
    }

    private class PhoneListener extends BasePhoneEventListener {

        public void handleOnPhone(OnPhoneEvent event) {
            if (dialDialog != null) {
                dialDialog.setVisible(false);
            }
            /*
                  if (incomingDialog != null) {
                      incomingDialog.hidePopupImmediately();
                  }
            */
        }

        public void handleHangUp(HangUpEvent event) {
            if (dialDialog != null) {
                dialDialog.setVisible(false);
            }
            /*
                  if (incomingDialog != null) {
                      incomingDialog.hidePopupImmediately();
                  }

            */

        }

        public void handleRing(RingEvent event) {
            IncomingCall incomingCall = new IncomingCall();
            boolean idExists = false;
            if (ModelUtil.hasLength(event.getCallerIDName())) {
                incomingCall.setCallerName(event.getCallerIDName());
                idExists = true;
            }

            if (ModelUtil.hasLength(event.getCallerID())) {
                incomingCall.setCallerNumber(event.getCallerID());
                idExists = true;
            }

            if (!idExists) {
                incomingCall.setCallerName(Res.getString("message.no.caller.id"));
            }

            SparkToaster toasterManager = new SparkToaster();

            toasterManager.setTitle("Incoming Phone Call");
            toasterManager.setDisplayTime(5000);
            toasterManager.showToaster(SparkRes.getImageIcon(SparkRes.ON_PHONE_IMAGE));
            toasterManager.setComponent(incomingCall);

        }
    }

    public void callExtension(final String number) {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    phoneClient.dialByExtension(number);
                }
                catch (PhoneActionException e) {
                    Log.error(e);
                }
            }
        });

        thread.start();
    }

    public void callJID(final String jid) {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    phoneClient.dialByJID(jid);
                }
                catch (PhoneActionException e) {
                    Log.error(e);
                }
            }
        });

        thread.start();


    }


    public static PhoneClient getPhoneClient() {
        return phoneClient;
    }

    public void shutdown() {

    }

    public boolean canShutDown() {
        return true;
    }

    public void uninstall() {
        // Do nothing.
    }


}
