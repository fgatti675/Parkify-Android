package com.cahue.iweco;

import android.app.Application;

import com.facebook.FacebookSdk;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

/**
 * Created by Francesco on 14/06/2015.
 */
public class IwecoApp extends Application {

    public static GoogleAnalytics analytics;
    public static Tracker tracker;

    @Override
    public void onCreate() {
        super.onCreate();

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

    public void setTrackerUserId(String id){
        tracker.set("&uid", id);
    }
}
