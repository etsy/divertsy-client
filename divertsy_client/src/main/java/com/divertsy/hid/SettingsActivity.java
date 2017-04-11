package com.divertsy.hid;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.util.Log;

import com.divertsy.hid.utils.WeightRecorder;

import java.util.HashSet;
import java.util.List;

/**
 *  SettingsActivity handles inflating XML preference files and updating data when
 *  the user changes settings in the application.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {
    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof MultiSelectListPreference) {
                // For multi select list preferences we should show a list of the selected options
                MultiSelectListPreference listPreference = (MultiSelectListPreference) preference;
                CharSequence[] values = listPreference.getEntries();
                StringBuilder options = new StringBuilder();
                for(String stream : (HashSet<String>) value) {
                    int index = listPreference.findIndexOfValue(stream);
                    if (index >= 0) {
                        if (options.length() != 0) {
                            options.append(", ");
                        }
                        options.append(values[index]);
                    }
                }

                preference.setSummary(options);
            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        SharedPreferences prefs = preference.getSharedPreferences();
        Object value;
        if (preference instanceof MultiSelectListPreference) {
            value = prefs.getStringSet(preference.getKey(), new HashSet<String>());
        } else {
            value = prefs.getString(preference.getKey(), "");
        }
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, value);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item)) {
                NavUtils.navigateUpFromSameTask(this);
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName)
                || LocationPreferenceFragment.class.getName().equals(fragmentName)
                || SyncPreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            PreferenceManager prefMgr = getPreferenceManager();
            prefMgr.setSharedPreferencesName(WeightRecorder.PREFERENCES_NAME);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);

            Preference addToScalePref = findPreference(WeightRecorder.PREF_ADD_TO_SCALE);
            bindPreferenceSummaryToValue(addToScalePref);
            addToScalePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object value) {
                    ((SwitchPreference) findPreference(WeightRecorder.PREF_USE_BIN_WEIGHT)).setChecked(true);

                    return sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, value);
                }
            });

            findPreference("device_id").setSummary(ScaleApplication.get().getDeviceId());
        }



        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                NavUtils.navigateUpFromSameTask(this.getActivity());
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class LocationPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            PreferenceManager prefMgr = getPreferenceManager();
            prefMgr.setSharedPreferencesName(WeightRecorder.PREFERENCES_NAME);
            addPreferencesFromResource(R.xml.pref_location);
            setHasOptionsMenu(true);

            Preference office = findPreference(WeightRecorder.PREF_OFFICE);
            bindPreferenceSummaryToValue(office);
            office.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object value) {
                    // Set the correct waste streams for this office
                    String stringValue = value.toString();

                    if (getString(R.string.office_custom_choice).equals(stringValue)) {
                        OfficePreference customPicker = new OfficePreference(getActivity(), getPreferenceManager());
                        customPicker.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                            @Override
                            public boolean onPreferenceChange(Preference preference, Object newValue) {
                                Preference officePref = findPreference(WeightRecorder.PREF_OFFICE);
                                return officePref.getOnPreferenceChangeListener().onPreferenceChange(officePref, newValue);
                            }
                        });
                        customPicker.showDialog();
                        return false;
                    }

                    setDriveIdFromOffice(stringValue);

                    return sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, value);
                }
            });

            loadWasteStreamSettings();

            final ListPreference language = (ListPreference) findPreference(WeightRecorder.PREF_LANGUAGE);

            if (language.getEntry() == null){
                language.setSummary(R.string.default_language);
            } else {
                language.setSummary(language.getEntry());
            }

            language.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object value) {
                    language.setValue(value.toString());
                    preference.setSummary(language.getEntry());
                    loadWasteStreamSettings();
                    return false;
                }
            });

        }

        public void loadWasteStreamSettings(){
            final MultiSelectListPreference streamPrefs = (MultiSelectListPreference) findPreference(WeightRecorder.PREF_WASTE_STREAMS);
            WasteStreams wasteStreams = new WasteStreams();
            wasteStreams.loadWasteStreams(getActivity().getApplicationContext());

            streamPrefs.setDefaultValue(wasteStreams.getDefaultStreamValues());
            streamPrefs.setEntries(wasteStreams.getAllStreamNames());
            streamPrefs.setEntryValues(wasteStreams.getAllStreamValues());
            bindPreferenceSummaryToValue(streamPrefs);
        }

        // Places an older Drive ID file which might be associated with an office to the current Drive ID
        public void setDriveIdFromOffice(String office){
            PreferenceManager prefMgr = getPreferenceManager();
            String spf = prefMgr.getSharedPreferencesName();
            prefMgr.setSharedPreferencesName(SyncToDriveService.PREFERENCES_NAME);

            String officeDriveIDkey =  SyncToDriveService.PREF_DRIVE_ID + ":" + office;
            String sDriveID = prefMgr.getSharedPreferences().getString(officeDriveIDkey, "");

            prefMgr.getSharedPreferences().edit()
                    .putString(SyncToDriveService.PREF_DRIVE_ID, sDriveID)
                    .apply();

            prefMgr.setSharedPreferencesName(spf);

        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                NavUtils.navigateUpFromSameTask(this.getActivity());
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class SyncPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            //Make sure to call this since we're using a different
            PreferenceManager prefMgr = getPreferenceManager();
            prefMgr.setSharedPreferencesName(SyncToDriveService.PREFERENCES_NAME);
            addPreferencesFromResource(R.xml.pref_sync);
            setHasOptionsMenu(true);
            SetDriveStringDetails();

            Preference disconButton = findPreference("clear_drive_data");
            if(disconButton != null){
                disconButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
                    @Override
                    public boolean onPreferenceClick(Preference p){
                        Log.v("SETTING", "Clear Google Login");

                        ClearDriveID();

                        // Need to call service
                        Intent i = new Intent(getActivity(), SyncToDriveService.class).putExtra("clear_drive_data", true);
                        getActivity().startService(i);

                        return true;
                    }
                });
            }

        }

        private void SetDriveStringDetails(){
            PreferenceManager prefMgr = getPreferenceManager();
            prefMgr.setSharedPreferencesName(SyncToDriveService.PREFERENCES_NAME);

            findPreference(SyncToDriveService.PREF_DRIVE_ID).setSummary(
                    prefMgr.getSharedPreferences().getString(SyncToDriveService.PREF_DRIVE_ID, "none"));
            findPreference(SyncToDriveService.PREF_DRIVE_ID_LAST_SAVE_TIME).setSummary(
                    prefMgr.getSharedPreferences().getString(SyncToDriveService.PREF_DRIVE_ID_LAST_SAVE_TIME, "never"));

            if (prefMgr.getSharedPreferences().getString(SyncToDriveService.PREF_DRIVE_ID, "").length() < 1){
                findPreference("clear_drive_data").setEnabled(false);
            }
        }

        public String getCurrentOfficeSaveIDPref(){
            PreferenceManager prefMgr = getPreferenceManager();
            String spf = prefMgr.getSharedPreferencesName();
            prefMgr.setSharedPreferencesName(WeightRecorder.PREFERENCES_NAME);
            String sOffice = prefMgr.getSharedPreferences().getString(WeightRecorder.PREF_OFFICE, WeightRecorder.DEFAULT_OFFICE);
            prefMgr.setSharedPreferencesName(spf);
            return SyncToDriveService.PREF_DRIVE_ID + ":" + sOffice;
        }

        // Clears the old Drive Data. Need to commit this for the GUI menu update.
        public void ClearDriveID(){
            Log.v("SETTINGS", "Clearing Drive ID");

            PreferenceManager prefMgr = getPreferenceManager();
            prefMgr.setSharedPreferencesName(SyncToDriveService.PREFERENCES_NAME);
            SharedPreferences mSharedPreferences = prefMgr.getSharedPreferences();
            mSharedPreferences.edit()
                    .putBoolean(SyncToDriveService.PREF_USE_GOOGLE_DRIVE, false)
                    .apply();

            mSharedPreferences.edit()
                    .putString(SyncToDriveService.PREF_DRIVE_ID, "")
                    .apply();



            mSharedPreferences.edit()
                    .putString(getCurrentOfficeSaveIDPref(), "")
                    .apply();

            // Commit here so that we make sure these items are changed before
            // we read the strings again and update the settings page

            mSharedPreferences.edit()
                    .putString(SyncToDriveService.PREF_DRIVE_ID_LAST_SAVE_TIME, "")
                    .commit();

            SetDriveStringDetails();

        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                NavUtils.navigateUpFromSameTask(this.getActivity());
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }


    /**
     * An edit text preference which is used when the custom option is selected for the office
     */
    public static class OfficePreference extends EditTextPreference {

        public OfficePreference(Context context, PreferenceManager preferenceManager) {
            super(context);
            setKey(WeightRecorder.PREF_OFFICE);
            onAttachedToHierarchy(preferenceManager);
            getEditText().setSelectAllOnFocus(true);
        }

        public void showDialog() {
            showDialog(null);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
        }
    }
}
