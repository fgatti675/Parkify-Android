package com.cahue.iweco;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;

import com.cahue.iweco.activityRecognition.ActivityRecognitionService;
import com.cahue.iweco.util.PreferencesUtil;

/**
 * This fragment shows the preferences for the first header.
 */
public class PreferencesFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make sure default values are applied.  In a real app, you would
        // want this in a shared function that is used to retrieve the
        // SharedPreferences wherever they are needed.
//            PreferenceManager.setDefaultValues(getActivity(),
//                    R.xml.advanced_preferences, false);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.fragmented_preferences);
    }

    @Override
    public void onSharedPreferenceChanged(@NonNull SharedPreferences sharedPreferences,
                                          @NonNull String key) {

        if (key.equals(PreferencesUtil.PREF_MOVEMENT_RECOGNITION)) {
            boolean enableBtPreferences = sharedPreferences.getBoolean(PreferencesUtil.PREF_MOVEMENT_RECOGNITION, true);
            updateBtPreferencesState(enableBtPreferences);

            if (enableBtPreferences) {
                ActivityRecognitionService.startIfNoBT(getActivity());
            } else {
                ActivityRecognitionService.stop(getActivity());
            }
        }
    }

    private void updateBtPreferencesState(boolean enableBtPreferences) {
        getPreferenceManager().findPreference(PreferencesUtil.PREF_MOVEMENT_RECOGNITION_NOTIFICATION).setEnabled(enableBtPreferences);
        getPreferenceManager().findPreference(PreferencesUtil.PREF_BT_ON_ENTER_VEHICLE).setEnabled(enableBtPreferences);
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);


        updateBtPreferencesState(sharedPreferences.getBoolean(PreferencesUtil.PREF_MOVEMENT_RECOGNITION, true));
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }
}
