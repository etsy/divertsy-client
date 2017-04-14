package com.divertsy.hid;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.content.pm.PackageManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.divertsy.hid.ble.BLEScanner;
import com.divertsy.hid.ble.Beacon;
import com.divertsy.hid.usb.ScaleMeasurement;
import com.divertsy.hid.usb.UsbScaleManager;
import com.divertsy.hid.utils.AppUpdater;
import com.divertsy.hid.utils.Utils;
import com.divertsy.hid.utils.WeightRecorder;
import com.google.android.gms.common.GoogleApiAvailability;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static com.google.android.gms.common.ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED;

/**
 *  MainActivity this is the main Divertsy class. It handles UI updates and button presses.
 */
public class MainActivity extends AppCompatActivity implements UsbScaleManager.Callbacks, BLEScanner.OnClosestChangedListener {

    private static final String TAG = "DIVERTSY";

    public static final long SEND_DELAY_MILLIS = 1000;  // Helps to prevent double taps
    private static final int BUTTON_HEIGHT_PIXELS = 90;
    private static final int SETTINGS_RESULT = 0;
    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 2;
    private static final int REQUEST_CODE_RESOLUTION = 1;
    private static final String KEY_FLOOR = "Floor";
    private static final String KEY_PLACE = "Place";
    private static final String KEY_URL_TEXT = "url_text";

    private static final int REQUEST_WRITE_STORAGE = 112;

    private Handler mHandler = new MainHandler(this);
    private Handler mDialogDismissHandler = new ActivityHandler(this);

    public static class ActivityHandler extends Handler {
        protected final WeakReference<MainActivity> mRef;
        public ActivityHandler(MainActivity activity) {
            mRef = new WeakReference<>(activity);
        }
    }

    /**
     * Sets the new beacon after 4 seconds so it doesn't fluctuate incorrectly
     */
    public static class MainHandler extends ActivityHandler {
        public MainHandler(MainActivity activity) {
            super(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity mActivity = mRef.get();
            if (mActivity != null) {
                Beacon closest = (Beacon) msg.obj;
                List<String> segments = closest.urlStatus.getUrl().getPathSegments();
                mActivity.updateClosestBeacon(closest.urlStatus.toString(), segments.get(0), segments.get(1));
            }
        }
    }

    private long mLastSendTime = 0;
    private UsbScaleManager mUsbScaleManager;
    private WeightRecorder mWeightRecorder;
    private String mFloor;
    private String mPlace;
    private String SAVED_WEIGHT_TYPE;
    private boolean ZeroWeightAfterAdd = false;

    private TextView mWeight;
    private TextView mWeightUnit;
    private TextView mLocation;

    private BLEScanner mBLEScanner;

    // Required for Remote Scales
    private BroadcastReceiver mRemoteScaleReceiver;
    private ScaleMeasurement mLatestScaleMeasurement;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onNewIntent(Intent intent) {

        // This is used to handle items that need an Activity from the Sync Service
        try {
            // See if we got a Google API error
            int errorCode = intent.getIntExtra("apiErrorCode", 0);
            if (errorCode != 0) {
                Log.e(TAG, "Google API Availability Error: " + errorCode);
                GoogleApiAvailability.getInstance().getErrorDialog(this, errorCode, 0).show();
                return;
            }

            if(intent.getBooleanExtra("PlayServicesUpdate",false)) {
                GoogleApiAvailability.getInstance().getErrorDialog(this, SERVICE_VERSION_UPDATE_REQUIRED, 0).show();
            }

            // If this is the first Google Drive request, we need to prompt for a login to use
            PendingIntent pI = intent.getParcelableExtra("resolution");
            if (pI != null) {
                startIntentSenderForResult(pI.getIntentSender(), 1, null, 0, 0, 0);
            } else {
                Log.w(TAG, "No resolution in received Intent.");
            }
        } catch(Exception e) {
            Log.e(TAG, "Error starting new intent:" + e.getMessage());
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "Starting Divertsy - OnCreate");

        // These flags let the activity turn on the screen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);

        mWeightRecorder = new WeightRecorder(this);

        checkIfBluetoothEnabled();

        setContentView(R.layout.activity_main);
        mWeight = (TextView) findViewById(R.id.weight);
        mWeightUnit = (TextView) findViewById(R.id.weight_unit);
        mLocation = (TextView) findViewById(R.id.location);


        if (savedInstanceState == null) {

            // check for App Update
            File updateFile = AppUpdater.checkAppUpdate();
            if (updateFile != null) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(updateFile), "application/vnd.android.package-archive");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        } else {
            updateClosestBeacon(
                    savedInstanceState.getString(KEY_URL_TEXT),
                    savedInstanceState.getString(KEY_FLOOR),
                    savedInstanceState.getString(KEY_PLACE)
            );
        }

        mUsbScaleManager = new UsbScaleManager(this, getIntent(), this, savedInstanceState);

        initView();

        if (! mWeightRecorder.isOfficeNameSet()){
            Log.v(TAG, "No Office Name Set");
            new AlertDialog.Builder(this)
                    .setTitle(R.string.msg_set_office_name_warning_title)
                    .setMessage(R.string.msg_set_office_name_warning)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            OpenLocationSettings();
                        }
                    })
                    .show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mUsbScaleManager.onStart(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mRemoteScaleReceiver);
        if (mBLEScanner != null) {
            mBLEScanner.onPause();
        }
    }

    @Override
    protected void onResume() {
        // This could have changed via settings menu
        checkIfBluetoothEnabled();

        if (mBLEScanner != null) {
            mBLEScanner.onResume();
        }
        super.onResume();

        // This sets us up to listen for remote scale data
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.divertsy.REMOTE_SCALE_WEIGHT");
        mRemoteScaleReceiver = new RemoteScaleReceiver();
        registerReceiver(mRemoteScaleReceiver, filter);

        // The view might change via settings, this should refresh it
        initView();
    }

    @Override
    protected void onStop() {
        mUsbScaleManager.onStop(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mDialogDismissHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_FLOOR, mFloor);
        outState.putString(KEY_PLACE, mPlace);
        outState.putString(KEY_URL_TEXT, mLocation.getText().toString());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        Intent sharingIntent;
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivityForResult(new Intent(this, SettingsActivity.class), SETTINGS_RESULT);
                return true;
            case R.id.action_share_email:
                sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/csv");
                String subject_line = "Divertsy Data: " + mWeightRecorder.getOffice();
                String email_body = "Divertsy data attachement below. Last update: " + mWeightRecorder.getLastRecordedWeight();

                File csv = new File(Utils.getDivertsyFilePath(mWeightRecorder.getOffice()));
                sharingIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(csv));
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject_line );
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, email_body);
                startActivity(Intent.createChooser(sharingIntent, "Send CSV File"));

                return true;
            default:
                Log.i(TAG,"No Menu Option Found. Check the list in onOptionsItemSelected.");
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SETTINGS_RESULT) {
            // We might need to update the scale weight
            mUsbScaleManager.setAddToScaleWeight(mWeightRecorder.getDefaultWeight());
            initView();
        } else if (requestCode == REQUEST_ENABLE_BLUETOOTH && mBLEScanner != null) {
            if (resultCode == Activity.RESULT_OK) {
                mBLEScanner.init(this);
            }
        } else if (requestCode == REQUEST_CODE_RESOLUTION && resultCode == RESULT_OK) {
            // Connects the chosen account to our Google Drive API connector
            // mGoogleApiClient.connect();
            startService(new Intent(this, SyncToDriveService.class));
        } else {
            Log.e(TAG, "Activity Result Not Handled: " + requestCode );
        }
    }

    public void OpenLocationSettings() {
        startActivityForResult(new Intent(this, SettingsActivity.class), SETTINGS_RESULT);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_COARSE_LOCATION) {
            if (mBLEScanner != null) {
                // If we get denied, we should stop trying.
                if ((grantResults.length > 0) && (grantResults[0] == -1)){
                    Log.e(TAG, "Coarse Permission denied. Turning off Beacon setting.");
                    mWeightRecorder.setUseBeacons(false);
                } else{
                    mBLEScanner.onRequestPermissionsResult(requestCode, grantResults);
                }
            }
        }
        if (requestCode == REQUEST_WRITE_STORAGE){
            saveWeight(SAVED_WEIGHT_TYPE);
        }
    }

    private void checkIfBluetoothEnabled(){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            if (mWeightRecorder.useBluetoothBeacons()){
                mBLEScanner = new BLEScanner(this, REQUEST_ENABLE_BLUETOOTH, this);
            }
        }
    }

    private void initView() {
        setTitleBar();
        WasteStreams wasteStreams = new WasteStreams();
        wasteStreams.loadWasteStreams(getApplicationContext());

        // Only show the enabled streams or default if not set
        Set<String> enabledStreams = mWeightRecorder.getEnabledStreams();
        if ((enabledStreams == null) || (enabledStreams.size() == 0)){
            enabledStreams = wasteStreams.getDefaultStreamValuesSet();
        }
        List<String> sortedStreams = wasteStreams.getSortedStreams(enabledStreams);

        // Buttons rows are hardcoded in the acitivity_main layout for now
        LinearLayout[] buttonRows = {
                (LinearLayout) findViewById(R.id.button_row_1),
                (LinearLayout) findViewById(R.id.button_row_2),
                (LinearLayout) findViewById(R.id.button_row_3)
        };

        // Remove all current buttons. This can happen after a settings change.
        for(LinearLayout row: buttonRows){
            if(row.getChildCount() > 0) row.removeAllViews();
        }

        int current_row = 0;
        for (final String stream : sortedStreams) {
            Log.d(TAG,"Enabled Stream: " + stream);

            AppCompatButton button = new AppCompatButton(this);
            button.setText(wasteStreams.getDisplayNameFromValue(stream));
            LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT,1f);
            lparams.setMargins(10,10,10,10);
            button.setLayoutParams(lparams);
            button.setBackgroundColor(wasteStreams.getButtonColorFromValue(stream));
            button.setTextColor(Color.WHITE);
            button.setOnClickListener(new View.OnClickListener() {
                                          @Override
                                          public void onClick(View view) {
                                              saveWeight(stream);
                                          }
                                      }
            );
            buttonRows[current_row++ % buttonRows.length].addView(button);
        }



        // This sets up the manual weight input pop-up when the digits are tapped
        TextView tvWeight = (TextView) findViewById(R.id.weight);

        LayoutInflater factory = LayoutInflater.from(this);
        final View manualEntryView = factory.inflate(R.layout.manual_weight_entry, null);
        final EditText input = (EditText) manualEntryView.findViewById(R.id.manual_weight_input);

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.msg_manual_entry_title);
        builder.setMessage(R.string.msg_manual_entry_info);
        builder.setView(manualEntryView);

        // Load the manual unit weight list
        final Spinner manualUnitPicker = (Spinner) manualEntryView.findViewById(R.id.manual_weight_units);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.manual_weight_units, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        manualUnitPicker.setAdapter(adapter);

        builder.setCancelable(true);
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                try {
                    Double inputWeight = Double.parseDouble(input.getText().toString());
                    String inputUnits = manualUnitPicker.getSelectedItem().toString();

                    // Update the on Screen Display
                    mWeight.setText(Double.toString(inputWeight));
                    mWeightUnit.setText(inputUnits);
                    // Underline the Unit to show it was a manual entry
                    mWeightUnit.setPaintFlags(mWeightUnit.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

                    // Save the data so we can record it if the user taps a waste stream button
                    ScaleMeasurement.Builder measurementBuilder = new ScaleMeasurement.Builder();
                    measurementBuilder.rawScaleWeight(inputWeight);
                    measurementBuilder.scaleWeight(inputWeight);
                    measurementBuilder.units(inputUnits);
                    mLatestScaleMeasurement = measurementBuilder.build();

                    ZeroWeightAfterAdd = true;

                } catch (Exception e){
                    Log.e(TAG, "Error from Manual Input:" + e.getMessage());
                }

                dialog.dismiss();
            }
        });



        final AlertDialog manualWeightDialog = builder.create();

        tvWeight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Weight View Tap!");
                manualWeightDialog.show();
            };
        });

        Button mTare;
        mTare = (Button) findViewById(R.id.button_zero);
        if (mWeightRecorder.tareAfterAdd()){
            mTare.setBackgroundColor(Color.GRAY);
            mTare.setText(R.string.btn_zero);
            mTare.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "Call Zero Tare");
                    ScaleMeasurement sm = mUsbScaleManager.getLatestMeasurement();
                    if(sm != null) {
                        mUsbScaleManager.setAddToScaleWeight(sm.getRawScaleWeight());
                    } else {
                        Log.e(TAG, "Null ScaleMeasurement on Tare");
                    }
                }
            });
        } else {
            // Make the button transparent so it still takes up the space, but not in use.
            mTare.setBackgroundColor(Color.TRANSPARENT);
            mTare.setText("");
            mTare.setOnClickListener(null);
        }


    }

    private void setTitleBar() {
        Date buildDate = BuildConfig.buildTime;
        Log.i(TAG, "This App was built on " + buildDate.toString());

        try {
            getSupportActionBar().setTitle(getString(R.string.app_name) + ": "
                    + mWeightRecorder.getOffice() + " (" + Utils.getBuildNumber().toString() + ") ∆ " + mWeightRecorder.getLastRecordedWeight());
        } catch(Exception e) {
            Log.e(TAG, "Error setting Title:" + e.getMessage());
        }
    }

    public void requestStoragePermission(){
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                REQUEST_WRITE_STORAGE);
    }


    public void saveWeight(String weightType) {
        ScaleMeasurement measurement = mLatestScaleMeasurement;

        // New Android M+ permission check requirement.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                SAVED_WEIGHT_TYPE = weightType;
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.msg_perm_external_storage_title);
                builder.setMessage(R.string.msg_perm_external_storage);
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestStoragePermission();
                    }
                });
                builder.show();

                return;
            }
        }

        // This could happen if the scale is not connected.
        // We are now allowing "Zero" entries to be recorded, so we'll need to build
        // an empty measurement item if there was not one
        if (measurement == null) {
            ScaleMeasurement.Builder zSMBuilder = new ScaleMeasurement.Builder();
            zSMBuilder.rawScaleWeight(0.0f);
            zSMBuilder.scaleWeight(0.0f);
            // Defaulting to KG which is the 3rd item in the WeightUnit array
            zSMBuilder.units(mUsbScaleManager.WEIGHTUNIT[3]);
            measurement = zSMBuilder.build();
        }

        //Block to help prevent double sends
        long now = measurement.getTime();
        if (now < (mLastSendTime + SEND_DELAY_MILLIS)) {
            Log.v(TAG, "Double Send Block Triggered");
            return;
        } else {
            mLastSendTime = now;
        }

        //Block Negative values
        if (measurement.getScaleWeight() < 0) {
            showError(getString(R.string.error_negative_value));
            return;
        }

        Log.d(TAG, "Saving Weight - Type: " + weightType + " Value: " + Double.toString(measurement.getScaleWeight()));

        try {

            String sOffice = mWeightRecorder.getOffice();
            Utils.saveCSV(sOffice, measurement.toCSV(mWeightRecorder.getOffice(), weightType, mFloor, mPlace));

            mWeightRecorder.saveAsLastRecordedWeight(Double.toString(measurement.getScaleWeight()), weightType);
            setTitleBar();

            // Send an Intent to start Syncing if this is enabled
            SharedPreferences syncPreferences = getApplicationContext().getSharedPreferences(SyncToDriveService.PREFERENCES_NAME, Context.MODE_PRIVATE);
            if(syncPreferences.getBoolean(SyncToDriveService.PREF_USE_GOOGLE_DRIVE, false)){
                startService(new Intent(this, SyncToDriveService.class));
            }

            final AlertDialog dialog = new AlertDialog.Builder(this)
                    .setMessage(R.string.msg_weightsent)
                    .show();

            // Auto dismiss dialog
            mDialogDismissHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (dialog.isShowing()) {
                        dialog.dismiss();
                    }
                }
            }, 2000);

            if (mWeightRecorder.tareAfterAdd()) {
                Log.d(TAG, "Call Zero Tare");
                mUsbScaleManager.setAddToScaleWeight(measurement.getRawScaleWeight());
            }

            if (ZeroWeightAfterAdd){
                Log.i(TAG, "Zeroing Display");
                // Update the on Screen Display
                mWeight.setText(R.string.weight);
                mWeightUnit.setText("");
                mWeightUnit.setPaintFlags(mWeightUnit.getPaintFlags() & (~ Paint.UNDERLINE_TEXT_FLAG));
                mLatestScaleMeasurement = null;
                ZeroWeightAfterAdd = false;
            }

        } catch (Exception e) {
            showError(getString(R.string.error_reporting));
            e.printStackTrace();
        }
    }

    @Override
    public void onMeasurement(final ScaleMeasurement measurement) {
        Double weight = measurement.getScaleWeight();
        mWeight.setText(Double.toString(weight));

        // If zero, hide the units since the USB data won't always show the correct setting
        if (weight == 0){
            mWeightUnit.setText("");
        } else {
            mWeightUnit.setText(measurement.getScaleUnit());
            mWeightUnit.setPaintFlags(mWeightUnit.getPaintFlags() & (~ Paint.UNDERLINE_TEXT_FLAG));
        }

        // Save this in case the user presses a waste stream button
        mLatestScaleMeasurement = mUsbScaleManager.getLatestMeasurement();

    }

    public void showError(@NonNull String errorMessage) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.error)
                .setMessage(errorMessage)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    @Override
    public void onClosestChanged(final Beacon closest) {
        // Remove the last message if there was one, since we just updated to a new beacon
        mHandler.removeMessages(0);

        if (closest != null && closest.urlStatus != null && closest.urlStatus.getUrl() != null) {
            Message msg = Message.obtain(mHandler, 0, closest);
            // If we didn't have a saved floor or place and we just got one, set them immediately
            // otherwise wait 4 seconds to make sure it's stabilized
            if (mFloor == null || mPlace == null) {
                mHandler.sendMessageAtFrontOfQueue(msg);
            } else {
                mHandler.sendMessageDelayed(msg, 4000);
            }
        } else {
            mFloor = null;
            mPlace = null;
            mLocation.setText(null);
        }
    }

    private void updateClosestBeacon(String url, String floor, String place) {
        mFloor = floor;
        mPlace = place;
        mLocation.setText(url);
    }


    // use this as an inner class like here or as a top-level class
    public class RemoteScaleReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                float fRemoteWeight = intent.getFloatExtra("floatScaleWeight", 0.0f);
                String sRemoteUnit = intent.getStringExtra("stringScaleUnit");

                // Assume KG as the default unit
                if ((sRemoteUnit == null ) || (sRemoteUnit.length() < 1)){
                    sRemoteUnit = "KG";
                } else {
                    sRemoteUnit = sRemoteUnit.toUpperCase();
                }
                Log.d(TAG, "RemoteScale Data Received: " + fRemoteWeight + " " + sRemoteUnit);

                // Update the on Screen Display
                mWeight.setText(Float.toString(fRemoteWeight));
                mWeightUnit.setText(sRemoteUnit);

                // Save the data so we can record it if the user taps a waste stream button
                ScaleMeasurement.Builder measurementBuilder = new ScaleMeasurement.Builder();
                measurementBuilder.rawScaleWeight(fRemoteWeight);
                measurementBuilder.scaleWeight(fRemoteWeight);
                measurementBuilder.units(sRemoteUnit);
                mLatestScaleMeasurement = measurementBuilder.build();

            }  catch (Exception e) {
                Log.e(TAG, "REMOTE DATA BROADCAST RECEIVER ERROR: " + e.getMessage());
            }
        }

        // constructor
        public RemoteScaleReceiver(){
            Log.i(TAG, "Creating Broadcast Receiver");
        }

    }

}
