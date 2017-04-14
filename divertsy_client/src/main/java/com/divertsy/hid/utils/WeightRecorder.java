package com.divertsy.hid.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

/**
 *  WeightRecorder gets and stores values in shared preferences
 */
public class WeightRecorder {

    private static final String TAG = WeightRecorder.class.getName();

    public static final String PREFERENCES_NAME = "ScalePrefs";
    public static final String PREF_ADD_TO_SCALE = "add_to_scale";
    public static final String PREF_OFFICE = "office";
    public static final String PREF_USE_BIN_WEIGHT = "use_bin_weight";
    public static final String PREF_WASTE_STREAMS = "waste_streams";
    public static final String PREF_TARE_AFTER_ADD = "tare_after_add";
    public static final String PREF_USE_BEACONS = "use_beacons";
    public static final String PREF_LANGUAGE = "language";

    public static final String DEFAULT_OFFICE = "UNKNOWN";
    public static final String NO_SAVED_DATA = "Unknown Last Upload";
    private static final String PREF_LAST_SAVED_DATA = "last_saved_data";

    SharedPreferences mSharedPreferences;

    public WeightRecorder(Context context) {
        mSharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public String getLastRecordedWeight(){
        return mSharedPreferences.getString(PREF_LAST_SAVED_DATA, NO_SAVED_DATA);
    }

    public void saveAsLastRecordedWeight(String weight, String trashType){
        String saveText;
        SimpleDateFormat s = new SimpleDateFormat("E MM-dd HH:mm");
        String sdate = s.format(new Date());

        saveText = weight + " " + trashType + " @ " + sdate;

        mSharedPreferences.edit()
                .putString(PREF_LAST_SAVED_DATA, saveText)
                .apply();
    }

    public void setUseBeacons(Boolean value){
        mSharedPreferences.edit()
                .putBoolean(PREF_USE_BEACONS, value)
                .apply();
    }

    private double getSavedDouble(String key, double defaultValue) {
        return Double.valueOf(mSharedPreferences.getString(key, Double.toString(defaultValue)));
    }

    public Double getDefaultWeight() {
        if (useBinWeight()) {
            return getSavedDouble(PREF_ADD_TO_SCALE, 0);
        }
        return 0.0;
    }

    public String getOffice() {
        return mSharedPreferences.getString(PREF_OFFICE, DEFAULT_OFFICE);
    }

    public boolean useBinWeight() {
        return mSharedPreferences.getBoolean(PREF_USE_BIN_WEIGHT, false);
    }

    public boolean tareAfterAdd() {
        return mSharedPreferences.getBoolean(PREF_TARE_AFTER_ADD, false);
    }

    public boolean useBluetoothBeacons() {
        return mSharedPreferences.getBoolean(PREF_USE_BEACONS, false);
    }

    public Set<String> getEnabledStreams() {
        return mSharedPreferences.getStringSet(PREF_WASTE_STREAMS,null);
    }

    public boolean isOfficeNameSet(){
        if (getOffice().equalsIgnoreCase(DEFAULT_OFFICE)) {
            if (getLastRecordedWeight().equalsIgnoreCase(NO_SAVED_DATA)) {
                return false;
            }
        }
        return true;
    }
}
