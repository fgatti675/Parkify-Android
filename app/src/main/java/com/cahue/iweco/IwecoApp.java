package com.cahue.iweco;

import android.app.Application;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.facebook.FacebookSdk;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

/**
 * Created by Francesco on 14/06/2015.
 */
public class IwecoApp extends Application {

    public static GoogleAnalytics analytics;
    public static Tracker tracker;

    private RequestQueue mRequestQueue;

    private static IwecoApp iwecoApp;

    @Override
    public void onCreate() {
        super.onCreate();

        iwecoApp = this;
        
        FacebookSdk.sdkInitialize(getApplicationContext());

        analytics = GoogleAnalytics.getInstance(this);
        analytics.setLocalDispatchPeriod(1800);

        tracker = analytics.newTracker(getResources().getString(R.string.analytics_id));
        tracker.enableExceptionReporting(true);
        tracker.enableAdvertisingIdCollection(true);
        tracker.enableAutoActivityTracking(true);

    }

    public synchronized Tracker getTracker() {
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

    public static IwecoApp getIwecoApp() {
        return iwecoApp;
    }
}
