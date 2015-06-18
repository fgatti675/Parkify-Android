package com.cahue.iweco;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import java.util.HashMap;

/**
 * Created by Francesco on 14/06/2015.
 */
public class IwecoApp extends Application {

    public static GoogleAnalytics analytics;
    public static Tracker tracker;

    @Override
    public void onCreate() {
        super.onCreate();

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

    public void setId(String id){
        tracker.set("&uid", id);
    }
}
