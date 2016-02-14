package com.cahue.iweco.util;

import android.support.annotation.Nullable;
import android.util.Log;

import com.cahue.iweco.BuildConfig;
import com.cahue.iweco.ParkifyApp;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

public class Tracking {

    public static final String CATEGORY_TUTORIAL = "Tutorial";

    public static final String CATEGORY_LOGIN = "Login";

    public static final String CATEGORY_MAP = "Map";

    public static final String CATEGORY_CAR_MANAGER = "Car Manager";

    public static final String CATEGORY_NAVIGATION_DRAWER = "Navigation drawer";

    public static final String CATEGORY_RATING_DIALOG = "Rating Dialog";

    public static final String ACTION_DO_LOGIN = "Do login";

    public static final String ACTION_SKIP_LOGIN = "Skip login";

    public static final String ACTION_CAR_SELECTED = "Car selected";

    public static final String ACTION_FREE_SPOT_SELECTED = "Spot selected";

    public static final String ACTION_CAR_EDIT = "Car edited";

    public static final String LABEL_FACEBOOK_LOGIN = "Facebook login";

    public static final String LABEL_GOOGLE_LOGIN = "Google login";

    public static final String LABEL_SELECTED_FROM_MARKER = "Selected from marker";

    public static final String LABEL_SELECTED_FROM_DRAWER = "Selected from drawer";

    public static final String ACTION_CAR_MANAGER_CLICK = "Car manager click";
    public static final String ACTION_FACEBOOK_INVITE_CLICK = "Facebook invite click";
    public static final String ACTION_SETTINGS_CLICK = "Settings click";
    public static final String ACTION_DONATION_CLICK = "Donation click";
    public static final String ACTION_SIGN_OUT = "Sign out";
    public static final String ACTION_HELP_CLICK = "Help click";

    public static final String ACTION_ACCEPT = "Accepted";
    public static final String ACTION_DISMISS = "Dismissed";


    public static void sendView(String screenName) {
        if (BuildConfig.DEBUG) return;

        getTracker().setScreenName(screenName);
        Log.i("Tracking", "View: " + screenName);
        HitBuilders.ScreenViewBuilder builder = new HitBuilders.ScreenViewBuilder();
        getTracker().send(builder.build());

    }

    public static void sendEvent(String category, String action) {
        sendEvent(category, action, null, null);
    }

    public static void sendEvent(String category, String action, String label) {
        sendEvent(category, action, label, null);
    }

    public static void sendEvent(String category, String action, @Nullable String label, @Nullable Long value) {
        if (BuildConfig.DEBUG) return;

        Log.i("Tracking", "Event: " + action + " " + category + " " + label + " " + value);
        HitBuilders.EventBuilder builder = new HitBuilders.EventBuilder();
        builder.setAction(action);
        builder.setCategory(category);
        if (label != null) builder.setLabel(label);
        if (value != null) builder.setValue(value == null ? -1 : value);

        getTracker().send(builder.build());

    }

    public static void sendException(String location, String message, boolean fatal) {
        if (BuildConfig.DEBUG) return;

        Log.i("Tracking", "Event: " + location + " " + message);

        // Build and send exception.
        getTracker().send(new HitBuilders.ExceptionBuilder()
                .setDescription(location + " : " + message)
                .setFatal(fatal)
                .build());

    }

    public static void setTrackerUserId(String trackerUserId) {
        ParkifyApp.getParkifyApp().setTrackerUserId(trackerUserId);
    }

    private static Tracker getTracker() {
        return ParkifyApp.getParkifyApp().getTracker();
    }
}
