package com.divertsy.hid;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Service;
import android.app.PendingIntent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.divertsy.hid.utils.Utils;
import com.divertsy.hid.utils.WeightRecorder;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.ExecutionOptions;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.metadata.CustomPropertyKey;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.google.android.gms.common.ConnectionResult.SERVICE_MISSING;
import static com.google.android.gms.common.ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED;

/**
 * SyncToDriveService handles creating the file which will be saved to Drive.
 * If you build from source, you will need to register your signing keys with
 * Google in order to use this API on a device. More information on the wiki.
 * https://developers.google.com/drive/android/auth
 */
public class SyncToDriveService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "SyncToDriveService";
    public static final String PREFERENCES_NAME = "DrivePrefs";
    public static final String PREF_USE_GOOGLE_DRIVE = "use_google_drive";
    public static final String PREF_DRIVE_ID = "drive_id";
    public static final String PREF_DRIVE_ID_LAST_SAVE_TIME = "last_save_time";
    String sPreviousDriveID;

    protected static final int REQUEST_CODE_RESOLUTION = 1;

    protected GoogleApiClient mGoogleApiClient;
    protected boolean shouldClearAccount = false;

    SharedPreferences mSharedPreferences;
    protected int mStartID;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mStartID = startId;

        // Check if we got an intent to clear the current login
        if (intent.getBooleanExtra("clear_drive_data", false)){
            // Could be a race condition here, thus we need to set a flag to clear the account
            // in case we're not connected yet, but also call ClearAccount if we are connected.
            shouldClearAccount = true;
            if (getGoogleApiClient().isConnected()) {
                ClearAccount();
            }
        } else {
            // Only sync if the use Google Drive setting is true
            if (mSharedPreferences.getBoolean(PREF_USE_GOOGLE_DRIVE, false)) {
                Toast.makeText(this, "Starting Google Drive Sync", Toast.LENGTH_SHORT).show();
                FindOrCreateDriveFile();
            }
        }

        return Service.START_NOT_STICKY;
    }

    @Override
    public void onCreate(){
        super.onCreate();
        mSharedPreferences = getApplicationContext().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        connectClient();
    }

    public String getCurrentOffice(){
        Context context = getApplicationContext();
        if(context != null){
            return getCurrentOffice(context);
        }
        Log.e(TAG, "Could not get Application Context when calling getCurrentOffice");
        return WeightRecorder.DEFAULT_OFFICE;
    }

    public String getCurrentOffice(Context context){
        SharedPreferences officePrefs = context.getSharedPreferences(WeightRecorder.PREFERENCES_NAME, Context.MODE_PRIVATE);
        return officePrefs.getString(WeightRecorder.PREF_OFFICE, WeightRecorder.DEFAULT_OFFICE);
    }

    protected void connectClient() {
        Log.i(TAG, "Calling connectClient");
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        mGoogleApiClient.connect();
    }

    public void ClearAccount(){
        Log.i(TAG, "Clearing Google API Account");
        GoogleApiClient mAPI = getGoogleApiClient();
        if (mAPI != null) {
            if(mAPI.isConnected()){
                mAPI.clearDefaultAccountAndReconnect();
                Log.i(TAG, "Clear account and reconnect called");
            } else {
                Log.w(TAG, "Google API client not connected when attempting disconnect");
                Toast.makeText(this, "Google API not connected. Make sure WiFi is On.", Toast.LENGTH_LONG).show();
            }
        } else {
            Log.w(TAG, "Google API was null when attempting to disconnect account");
        }
        stopSelf(mStartID);
    }


    /**
     * Called when activity gets invisible. Connection to Drive service needs to
     * be disconnected as soon as an activity is invisible.
     */
    @Override
    public void onDestroy() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onDestroy();
    }


    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "GoogleApiClient connected");
        if (shouldClearAccount){
            ClearAccount();
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "GoogleApiClient connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());

        if (result.getErrorCode() == SERVICE_MISSING){
            showMessage("Google Drive API not Found on this Device");
            Log.e(TAG, "Google Drive SERVICE_MISSING");
        }

        if (result.getErrorCode() == SERVICE_VERSION_UPDATE_REQUIRED){
            showMessage("Google Play Services update required. Please update in Google Play store.");
            Log.e(TAG, "Google Play Services update required");
            mGoogleApiClient.getContext().startActivity(new Intent(mGoogleApiClient.getContext(), MainActivity.class)
                    .putExtra("PlayServicesUpdate", true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }

        PendingIntent pI = result.getResolution();
        if (pI != null) {
            Log.v(TAG, "PendingIntent: " + pI.getIntentSender().toString());
            mGoogleApiClient.getContext().startActivity(new Intent(mGoogleApiClient.getContext(), MainActivity.class)
                    .putExtra("resolution", pI).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } else {
            Log.e(TAG, "Pending Intent resolution was Null");
        }

        // If we don't stop this service, we tend to have an invalid API client
        // when we call the first Create new Drive file command
        stopSelf(mStartID);
    }


    public void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.d(TAG,"SHOW MESSAGE: " + message);
    }


    public GoogleApiClient getGoogleApiClient() {
        return mGoogleApiClient;
    }

    void FindOrCreateDriveFile(){
        // Check if we already have a driveID file
        Context context = getApplicationContext();
        SharedPreferences mSharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        sPreviousDriveID = mSharedPreferences.getString(PREF_DRIVE_ID,"");
        if (sPreviousDriveID.length() > 0){
            Log.v(TAG, "Using saved drive ID: " + sPreviousDriveID);
            Drive.DriveApi.fetchDriveId(getGoogleApiClient(),sPreviousDriveID)
                    .setResultCallback(driveFetchIDCallback);
        } else {
            // create new contents resource
            Log.v(TAG, "Attempting to create new drive file");
            Drive.DriveApi.newDriveContents(getGoogleApiClient())
                    .setResultCallback(driveContentsCallback);
        }
    }

    /**
     * WriteDriveFile will pull in the CSV file for the current office and
     * write its data to the drive file, which is saved locally when the call back
     * is received.
     *
     * @param driveContents where the CSV data will go in Google Drive
     */
    void WriteDriveFile(final DriveContents driveContents){
        new Thread() {
            @Override
            public void run() {
                // write content to DriveContents

                try {
                    Log.v(TAG, "Starting Write to Drive File");
                    OutputStream outputStream = driveContents.getOutputStream();
                    Writer writer = new OutputStreamWriter(outputStream);

                    File csv = new File(Utils.getDivertsyFilePath(getCurrentOffice()));
                    FileInputStream instream = new FileInputStream(csv);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(instream));
                    String line = reader.readLine();
                    while(line != null){
                        writer.write(line + System.getProperty("line.separator"));
                        line = reader.readLine();
                    }
                    writer.close();
                    Log.v(TAG, "Drive File Closed");
                } catch (IOException e) {
                    Log.v(TAG, "Error writing to drive file");
                    Log.e(TAG, e.getMessage());
                }

                // Check if we're writing a brand new file or updating an old one.
                if (sPreviousDriveID.length() > 0 ) {
                    driveContents.commit(getGoogleApiClient(), null,
                            new ExecutionOptions.Builder()
                                    .setNotifyOnCompletion(true)
                                    .build()
                    );
                } else {

                    // New file, so we'll set the file name and properties
                    String driveFileName = "DivertsyData-" + getCurrentOffice() + ".csv";
                    CustomPropertyKey officePropertyKey = new CustomPropertyKey("DivertsyOffice", CustomPropertyKey.PRIVATE);

                    MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                            .setTitle(driveFileName)
                            .setMimeType("text/csv")
                            .setDescription("Divertsy Waste Stream Data File")
                            .setCustomProperty(officePropertyKey,getCurrentOffice())
                            .setStarred(false).build();

                    Drive.DriveApi.getRootFolder(getGoogleApiClient())
                            .createFile(getGoogleApiClient(), changeSet, driveContents,
                                    new ExecutionOptions.Builder()
                                            .setNotifyOnCompletion(true)
                                            .build()
                            )
                            .setResultCallback(fileCallback);
                }

            }
        }.start();
    }


    // Used to clear our old drive ID file name, then try to save again to a new file
    private void ResetDriveFile(){
        Log.e(TAG, "Resetting saved Drive file ID. This may make a new file in Drive.");
        SaveDriveID("", getApplicationContext());
        sPreviousDriveID = "";
        FindOrCreateDriveFile();
    }

    // Opens a file that was previously created and then writes data to it
    private void OpenDriveFile(final DriveFile file){
        new Thread() {
            @Override
            public void run() {
                DriveApi.DriveContentsResult driveContentsResult =
                        file.open(getGoogleApiClient(), DriveFile.MODE_WRITE_ONLY, null).await();
                if (!driveContentsResult.getStatus().isSuccess()) {
                    showMessage("Error while trying to get previous Drive File");
                    return;
                }
                DriveContents driveContents = driveContentsResult.getDriveContents();
                WriteDriveFile(driveContents);
            }
        }.start();
    }


    final private ResultCallback<DriveApi.DriveIdResult> driveFetchIDCallback = new
            ResultCallback<DriveApi.DriveIdResult>() {
                @Override
                public void onResult(final DriveApi.DriveIdResult result) {
                    if (!result.getStatus().isSuccess()) {
                        ResetDriveFile();
                        return;
                    }
                    OpenDriveFile(result.getDriveId().asDriveFile());
                }
            };

    // Creates a new Drive file then writes data to it
    final private ResultCallback<DriveApi.DriveContentsResult> driveContentsCallback = new
            ResultCallback<DriveApi.DriveContentsResult>() {
                @Override
                public void onResult(DriveApi.DriveContentsResult result) {
                    if (!result.getStatus().isSuccess()) {
                        showMessage("Error while trying to create new file contents");
                        return;
                    }
                    Log.v(TAG, "driveContentsCallback got valid result");
                    final DriveContents driveContents = result.getDriveContents();
                    WriteDriveFile(driveContents);
                }
            };

    // Called after data is written to the drive file
    final ResultCallback<DriveFolder.DriveFileResult> fileCallback = new
            ResultCallback<DriveFolder.DriveFileResult>() {
                @Override
                public void onResult(DriveFolder.DriveFileResult result) {
                    if (!result.getStatus().isSuccess()) {
                        showMessage("Error while trying to create the file");
                        return;
                    }
                    String sDriveID = result.getDriveFile().getDriveId().encodeToString();
                    showMessage("Local Save to Google Drive");
                }
            };

    // After we get an Event from the SyncEvent Service, look up the meta data and store the office ID
    ResultCallback<DriveResource.MetadataResult> metadataRetrievedCallback = new
            ResultCallback<DriveResource.MetadataResult>() {

                @Override
                public void onResult(DriveResource.MetadataResult result) {
                    if (!result.getStatus().isSuccess()) {
                        showMessage("Problem while trying to fetch metadata");
                        return;
                    }
                    Metadata metadata = result.getMetadata();
                    showMessage("Metadata successfully fetched. Title: " + metadata.getTitle());
                }
            };


    // Save the ID and time of the Drive file so that we can write to the same one next time.
    final public void SaveDriveID(String driveID, Context context){
        Log.v(TAG, "Saving Drive ID: " + driveID);

        SharedPreferences mSharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        mSharedPreferences.edit()
                .putString(PREF_DRIVE_ID, driveID)
                .apply();

        // Save with Office Name
        mSharedPreferences.edit()
                .putString(PREF_DRIVE_ID + ":" + getCurrentOffice(context), driveID)
                .apply();

        SimpleDateFormat s = new SimpleDateFormat("E MMM dd, yyyy HH:mm:ss z");
        String sdate = s.format(new Date());

        // If the name got cleared, then also clear the last saved date/time
        if(driveID.length()<1){
            sdate = "";
        }

        mSharedPreferences.edit()
                .putString(PREF_DRIVE_ID_LAST_SAVE_TIME, sdate)
                .apply();
    }

}
