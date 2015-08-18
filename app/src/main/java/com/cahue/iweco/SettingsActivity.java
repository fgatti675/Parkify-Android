package com.cahue.iweco;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.cahue.iweco.activityRecognition.ActivityRecognitionService;
import com.cahue.iweco.util.PreferencesUtil;

/**
 * Created by f.gatti.gomez on 08/07/15.
 */
public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_preferences);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        toolbar.setTitle(R.string.preferences);
        toolbar.setNavigationIcon(R.drawable.ic_action_navigation_arrow_back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        ViewCompat.setElevation(toolbar, getResources().getDimension(R.dimen.elevation));

//        // Display the fragment as the main content.
//        getFragmentManager().beginTransaction()
//                .replace(android.R.id.content, new PreferencesFragment())
//                .commit();
    }

//    /**
//     * Populate the activity with the top-level headers.
//     */
//    @Override
//    public void onBuildHeaders(List<Header> target) {
//        loadHeadersFromResource(R.xml.preference_headers, target);
//    }
//

    /**
     * This fragment shows the preferences for the first header.
     */
    public static class PreferencesFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
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
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                              String key) {

            if (key.equals(PreferencesUtil.PREF_MOVEMENT_RECOGNITION)) {
                boolean enableBtPreferences = sharedPreferences.getBoolean(PreferencesUtil.PREF_MOVEMENT_RECOGNITION, true);
                updateBtPreferencesState(enableBtPreferences);

                if (enableBtPreferences) {
                    ActivityRecognitionService.startIfNecessary(getActivity());
                } else {
                    ActivityRecognitionService.stop(getActivity());
                }
            }
        }

        private void updateBtPreferencesState(boolean enableBtPreferences) {
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

}

