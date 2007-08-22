/**
 * $Revision: $
 * $Date: $
 *
 * Copyright (C) 2006 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Lesser Public License (LGPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.sparkimpl.preference.notifications;

import org.jivesoftware.resource.Res;
import org.jivesoftware.spark.component.VerticalFlowLayout;
import org.jivesoftware.spark.util.ResourceUtils;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

/**
 * Represents the UI for handling notification preferences within Spark.
 *
 * @author Derek DeMoro
 */
public class NotificationsUI extends JPanel {

    private JCheckBox toasterBox;
    private JCheckBox windowFocusBox;
    private JCheckBox offlineNotificationBox;
    private JCheckBox onlineNotificationBox;
    private JCheckBox betaCheckBox;

    public NotificationsUI() {
        setLayout(new VerticalFlowLayout());

        setBorder(BorderFactory.createTitledBorder(Res.getString("group.notification.options")));

        toasterBox = new JCheckBox();
        ResourceUtils.resButton(toasterBox, Res.getString("checkbox.show.toaster"));
        add(toasterBox);

        windowFocusBox = new JCheckBox();
        ResourceUtils.resButton(windowFocusBox, Res.getString("checkbox.window.to.front"));
        add(windowFocusBox);

        offlineNotificationBox = new JCheckBox("Notify when a user goes offline.");
        add(offlineNotificationBox);

        onlineNotificationBox = new JCheckBox("Notify when a user comes online.");
        add(onlineNotificationBox);

        betaCheckBox = new JCheckBox( Res.getString("menuitem.check.for.updates"));
        add(betaCheckBox);
    }

    public void setShowToaster(boolean show) {
        toasterBox.setSelected(show);
    }

    public boolean showToaster() {
        return toasterBox.isSelected();
    }

    public void setShowWindowPopup(boolean popup) {
        windowFocusBox.setSelected(popup);
    }

    public boolean shouldWindowPopup() {
        return windowFocusBox.isSelected();
    }

    public void setOfflineNotification(boolean notify) {
        offlineNotificationBox.setSelected(notify);
    }

    public boolean isOfflineNotificationOn() {
        return offlineNotificationBox.isSelected();
    }

    public void setOnlineNotification(boolean notify) {
        onlineNotificationBox.setSelected(notify);
    }

    public boolean isOnlineNotificationOn() {
        return onlineNotificationBox.isSelected();
    }

    public void setCheckForBeta(boolean check) {
        betaCheckBox.setSelected(check);
    }

    public boolean isBetaCheckingEnabled() {
        return betaCheckBox.isSelected();
    }
}
