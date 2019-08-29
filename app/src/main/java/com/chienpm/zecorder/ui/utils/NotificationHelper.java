package com.chienpm.zecorder.ui.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

public class NotificationHelper {
    public static final String CHANNEL_ID = "com.chienpm.zecorder";
    public static final String CHANNEL_NAME = "Zecorder Service";
    private static final String CHANNEL_DESCRTIPTION = "Zecorder_Channel_Description";

    private static final NotificationHelper ourInstance = new NotificationHelper();



    public static NotificationHelper getInstance() {
        return ourInstance;
    }

    private NotificationHelper() {
    }

    public void createNotificationChannel(Context context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance);
            channel.setDescription(CHANNEL_DESCRTIPTION);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
