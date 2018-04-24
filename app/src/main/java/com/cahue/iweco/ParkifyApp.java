package com.cahue.iweco;

import android.app.Application;
import android.net.TrafficStats;
import android.os.Build;
import android.os.StrictMode;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.cahue.iweco.util.NotificationChannelsUtils;
import com.facebook.ads.AdSettings;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

/**
 * Created by Francesco on 14/06/2015.
 */
public class ParkifyApp extends Application {

    private static Tracker tracker;
    private static ParkifyApp parkifyApp;
    private RequestQueue mRequestQueue;
    private static final int THREAD_ID = 10000;

    private FirebaseAnalytics firebaseAnalytics;

    public static ParkifyApp getParkifyApp() {
        return parkifyApp;
    }

    @Override
    public void onCreate() {

        super.onCreate();

        parkifyApp = this;

        FirebaseRemoteConfig firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();
        firebaseRemoteConfig.setConfigSettings(configSettings);
        firebaseRemoteConfig.setDefaults(R.xml.remote_config_defaults);

        TrafficStats.setThreadStatsTag(THREAD_ID);

        /**
         * Start Google analytics
         */
        GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);

        tracker = analytics.newTracker(getResources().getString(R.string.analytics_id));
        tracker.enableAdvertisingIdCollection(true);

        firebaseAnalytics = FirebaseAnalytics.getInstance(this);

        /**
         * Start volley queue
         */
        // Instantiate the cache
        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // 1MB cap
        // Set up the network to use HttpURLConnection as the HTTP client.
        Network network = new BasicNetwork(new HurlStack());
        // Instantiate the RequestQueue with the cache and network.
        mRequestQueue = new RequestQueue(cache, network);

        // Start the queue
        mRequestQueue.start();

        // Strict mode
        if (BuildConfig.DEBUG) {

            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyFlashScreen()
                    .build());

            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());

            AdSettings.addTestDevice("2e398393636c7cca29281dda912adc42");


        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (BuildConfig.DEBUG) {
                NotificationChannelsUtils.createDebugNotificationChannel(this);
            }
            NotificationChannelsUtils.createDefaultNotificationChannels(this);
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        mRequestQueue.stop();
    }

    public Tracker getTracker() {
        return tracker;
    }

    public void setTrackerUserId(String id) {
        tracker.set("&uid", id);
    }

    public RequestQueue getRequestQueue() {
        return mRequestQueue;
    }

    public FirebaseAnalytics getFirebaseAnalytics() {
        return firebaseAnalytics;
    }
}
