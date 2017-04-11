package com.divertsy.hid;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;


import com.divertsy.hid.utils.WeightRecorder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *  SettingsActivity handles parsing the waste_streams.json file for button information
 *  and returning that data to the main and settings activity.
 */
public class WasteStreams {

    private static final String TAG = "DIVERTSY";
    private List<String> StreamNames;
    private List<String> StreamValues;
    private List<String> DefaultStreamValues;
    private List<String> ButtonColors;
    private String LanguageSetting;

    public static final String JSON_DISPLAY_NAME = "display_name";

    public void loadWasteStreams(Context context){
        StringBuilder json = new StringBuilder();
        StreamNames = new ArrayList<String>();
        StreamValues = new ArrayList<String>();
        ButtonColors = new ArrayList<String>();
        DefaultStreamValues = new ArrayList<String>();
        String displayNameField = JSON_DISPLAY_NAME;

        SharedPreferences prefs = context.getSharedPreferences(WeightRecorder.PREFERENCES_NAME, Context.MODE_PRIVATE);
        LanguageSetting = prefs.getString(WeightRecorder.PREF_LANGUAGE, "");
        if (LanguageSetting.length() > 0){
            displayNameField = JSON_DISPLAY_NAME + "_" + LanguageSetting;
        }

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    context.getResources().openRawResource(R.raw.waste_streams)));
            String line;
            while((line = reader.readLine()) != null) {
                json.append(line);
            }
            JSONArray waste_streams = (JSONArray) new JSONTokener(json.toString()).nextValue();


            for (int i = 0; i < waste_streams.length(); i++) {
                JSONObject waste_stream = waste_streams.getJSONObject(i);

                // Check if this tag has the proper language, otherwise get the default
                String streamName;
                try {
                    streamName = waste_stream.getString(displayNameField);
                } catch (JSONException e) {
                    streamName = waste_stream.getString(JSON_DISPLAY_NAME);
                }

                Log.d(TAG, "Loading Stream: " + streamName);


                StreamNames.add(streamName);

                StreamValues.add(waste_stream.getString("logged_data_name"));
                ButtonColors.add(waste_stream.getString("button_color"));
                if(waste_stream.getBoolean("is_default")){
                    DefaultStreamValues.add(waste_stream.getString("logged_data_name"));
                }
            }

        } catch (Exception e){
            Log.e(TAG, e.getLocalizedMessage());
        }

        // If there are no saved waste streams, set the value to the default streams
        Set<String> savedStreams = prefs.getStringSet(WeightRecorder.PREF_WASTE_STREAMS, null);
        if ((savedStreams == null) || (savedStreams.size() == 0)){
            prefs.edit().putStringSet(WeightRecorder.PREF_WASTE_STREAMS,getDefaultStreamValuesSet()).apply();
        }

    }

    public CharSequence[] getAllStreamNames(){
        return StreamNames.toArray(new CharSequence[StreamNames.size()]);
    }

    public CharSequence[] getAllStreamValues(){
        return StreamValues.toArray(new CharSequence[StreamValues.size()]);
    }

    public CharSequence[] getDefaultStreamValues(){
        return DefaultStreamValues.toArray(new CharSequence[DefaultStreamValues.size()]);
    }

    public Set<String> getDefaultStreamValuesSet(){
        return new HashSet<String>(DefaultStreamValues);
    }

    public String getDisplayNameFromValue(String value){
        int index = StreamValues.indexOf(value);
        if (index < 0){
            Log.e(TAG, "Stream value not found: " + value);
            return "";
        }
        return StreamNames.get(index);
    }

    public Integer getButtonColorFromValue(String value){
        Integer color = Color.parseColor("#FF555555");
        int index = StreamValues.indexOf(value);
        if (index < 0){
            Log.e(TAG, "Stream value not found: " + value);
            return color;
        }
        String input_color = ButtonColors.get(index);
        try{
            color = Color.parseColor(input_color);
        } catch(Exception e) {
            Log.e(TAG, "button_color decoded failed: " + input_color);
        }
        return color;
    }

    // Sorts the buttons so the order matches the JSON file
    // This will also silently drop saved streams if they are no longer in the JSON file
    public List<String> getSortedStreams(Set<String> unsorted){
        List<String> sorted = new ArrayList<String>();
        for (String streamValue: StreamValues){
            if (unsorted.contains(streamValue)){
                sorted.add(streamValue);
            }
        }
        return sorted;
    }
}
