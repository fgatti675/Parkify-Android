package com.cahue.iweco.util;

/**
 * Created by f.gatti.gomez on 06.08.17.
 */

public class NotificationChannelsUtils {

//    public static final String JUST_PARKED_CHANNEL_ID = BuildConfig.APPLICATION_ID + ".just_parked";
//    public static final String ACT_RECOG_CHANNEL_ID = BuildConfig.APPLICATION_ID + ".activity_recognition";
//    public static final String DEBUG_CHANNEL_ID = BuildConfig.APPLICATION_ID + ".debug";
//
//    public static void createDebugNotificationChannel(Context context) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
//            if (mNotifyMgr.getNotificationChannel(DEBUG_CHANNEL_ID) != null)
//                return;
//            /* Create or update. */
//            NotificationChannel channel = new NotificationChannel(DEBUG_CHANNEL_ID, "Activity recognition debug",
//                    NotificationManager.IMPORTANCE_LOW);
//            mNotifyMgr.createNotificationChannel(channel);
//        }
//    }
//
//    public static void createDefaultNotificationChannels(Context context) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
//
//            if (mNotifyMgr.getNotificationChannel(JUST_PARKED_CHANNEL_ID) != null)
//                return;
//            /* Create or update. */
//            NotificationChannel channel = new NotificationChannel(JUST_PARKED_CHANNEL_ID, context.getString(R.string.just_parked_channel_description),
//                    NotificationManager.IMPORTANCE_LOW);
//            mNotifyMgr.createNotificationChannel(channel);
//
//            /* Create or update. */
//            channel = new NotificationChannel(ACT_RECOG_CHANNEL_ID, context.getString(R.string.activity_recog_channel_description),
//                    NotificationManager.IMPORTANCE_LOW);
//            mNotifyMgr.createNotificationChannel(channel);
//        }
//    }
}
