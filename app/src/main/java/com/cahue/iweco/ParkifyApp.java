package com.cahue.iweco;

import android.app.Application;
import android.os.StrictMode;
import android.util.Log;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.Volley;
import com.facebook.FacebookSdk;
import com.facebook.ads.AdSettings;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

/**
 * Created by Francesco on 14/06/2015.
 */
public class ParkifyApp extends Application {

    private static Tracker tracker;
    private static ParkifyApp parkifyApp;
    private RequestQueue mRequestQueue;

    public static ParkifyApp getParkifyApp() {
        return parkifyApp;
    }

    @Override
    public void onCreate() {

        long initTime = System.currentTimeMillis();

        super.onCreate();

        parkifyApp = this;

        /**
         * Start Facebook SDK
         */
        FacebookSdk.sdkInitialize(getApplicationContext());

        /**
         * Start Google analytics
         */
        GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);

        tracker = analytics.newTracker(getResources().getString(R.string.analytics_id));
        tracker.enableAdvertisingIdCollection(true);

        /**
         * Start volley queue
         */
        // getApplicationContext() is key, it keeps you from leaking the
        // Activity or BroadcastReceiver if someone passes one in.
        mRequestQueue = Volley.newRequestQueue(getApplicationContext());
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

        Log.d("App speed", "App init time : " + (System.currentTimeMillis() - initTime));
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
}
