package com.polyvi.xface.extension;

import org.json.JSONArray;
import org.json.JSONException;

import android.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import com.polyvi.xface.util.XLog;

public class XStatusBarNotificationExt extends XExtension {

    private static final String CLASS_NAME = XStatusBarNotificationExt.class
            .getSimpleName();
    private static final String COMMAND_NOTIFY = "notify";
    private static final String COMMAND_CLEAR = "clear";
    private static final String COMMAND_CLEARALL = "clearAll";

    private NotificationManager mNotificationManager = null;

    @Override
    public void sendAsyncResult(String result) {
    }

    @Override
    public boolean isAsync(String action) {
        return true;
    }

    @Override
    public XExtensionResult exec(String action, JSONArray args,
            XCallbackContext callbackCtx) throws JSONException {
        if (COMMAND_NOTIFY.equals(action)) {
            String tag = args.getString(0);
            String title = args.getString(1);
            String body = args.getString(2);
            int flag = args.getInt(3);
            XLog.d(CLASS_NAME, "Notification: " + tag + ", " + title + ", "
                    + body + ", " + flag);
            showNotification(tag, title, body, flag);
        } else if (COMMAND_CLEAR.equals(action)) {
            String tag = args.getString(0);
            XLog.d(CLASS_NAME, "Notification cancel: " + tag);
            clearNotification(tag);
        } else if (COMMAND_CLEARALL.equals(action)) {
            XLog.d(CLASS_NAME, "Notification cancel all");
            clearAllNotifications();
        } else {
            return new XExtensionResult(XExtensionResult.Status.INVALID_ACTION);
        }
        return new XExtensionResult(XExtensionResult.Status.OK);
    }

    /**
     * Displays status bar notification
     *
     * @param tag
     *            Notification tag.
     * @param contentTitle
     *            Notification title
     * @param contentText
     *            Notification text
     * */
    public void showNotification(CharSequence tag, CharSequence contentTitle,
            CharSequence contentText, int flag) {
        Context context = getContext();
        Notification noti = buildNotification(context, tag, contentTitle,
                contentText, flag);
        getMyNotificationManager().notify(tag.hashCode(), noti);
    }

    /**
     * Cancels a single notification by tag.
     *
     * @param tag
     *            Notification tag to cancel.
     */
    public void clearNotification(String tag) {
        getMyNotificationManager().cancel(tag.hashCode());
    }

    /**
     * Removes all Notifications from the status bar.
     */
    public void clearAllNotifications() {
        getMyNotificationManager().cancelAll();
    }

    /**
     * create a notification
     *
     * @param context
     *            context
     * @param contentTitle
     *            Notification title
     * @param contentText
     *            Notification text
     * @param flag
     *            Notification flag
     * */

    public Notification buildNotification(Context context, CharSequence tag,
            CharSequence contentTitle, CharSequence contentText, int flag) {
        int icon = R.drawable.btn_star;
        long when = System.currentTimeMillis();
        Notification noti = new Notification();
        noti.icon = icon;
        noti.tickerText = contentText;
        noti.when = when;
        noti.flags |= flag;

        PackageManager pm = context.getPackageManager();
        Intent notificationIntent = pm.getLaunchIntentForPackage(context
                .getPackageName());
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        notificationIntent.putExtra("notificationTag", tag);

        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, 0);
        noti.setLatestEventInfo(context, contentTitle, contentText,
                contentIntent);
        return noti;
    }

    /**
     * get mNotificationManager(a instance of NotificationManager). if mNotificationManager is null, initialize it
     * */
    private NotificationManager getMyNotificationManager() {
        if (null == this.mNotificationManager) {
            String ns = Context.NOTIFICATION_SERVICE;
            Context context = getContext();
            this.mNotificationManager = (NotificationManager) context
                    .getSystemService(ns);
        }
        return this.mNotificationManager;
    }
}
