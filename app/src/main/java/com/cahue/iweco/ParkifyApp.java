package com.cahue.iweco;

import android.app.Application;
import android.os.StrictMode;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.facebook.FacebookSdk;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

/**
 * Created by Francesco on 14/06/2015.
 */
public class ParkifyApp extends Application {

    private static GoogleAnalytics analytics;
    private static Tracker tracker;
    private static ParkifyApp parkifyApp;
    private RequestQueue mRequestQueue;

    public static ParkifyApp getParkifyApp() {
        return parkifyApp;
    }

    @Override
    public void onCreate() {

        super.onCreate();

        parkifyApp = this;

        /**
         * Start Facebook SDK
         */
        FacebookSdk.sdkInitialize(getApplicationContext());

        /**
         * Start Google analytics
         */
        analytics = GoogleAnalytics.getInstance(this);
        analytics.setLocalDispatchPeriod(1800);

        tracker = analytics.newTracker(getResources().getString(R.string.analytics_id));
        tracker.enableAdvertisingIdCollection(true);
//        tracker.enableAutoActivityTracking(true);

        // Strict mode
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()   // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }
    }

    public Tracker getTracker() {
        return tracker;
    }

    public void setTrackerUserId(String id) {
        tracker.set("&uid", id);
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            mRequestQueue = Volley.newRequestQueue(getApplicationContext());
        }
        return mRequestQueue;
    }
}
