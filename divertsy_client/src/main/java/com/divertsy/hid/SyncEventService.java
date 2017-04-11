package com.divertsy.hid;

import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.events.CompletionEvent;
import com.google.android.gms.drive.events.DriveEventService;

/**
 * SyncEventService is used to respond to Events from the Google Drive API.
 * At this time, Divertsy only uses it to capture the ResourceId of the
 * file which gets saved into Google Drive. Before this event, we have a FileID
 * but the ResourceId is needed so we can access and update the same file each time.
 *
 */
public class SyncEventService extends DriveEventService {
    private static final String TAG = "SyncEventService";

    @Override
    public void onCompletion(CompletionEvent event) {  super.onCompletion(event);
        Log.i(TAG, "New SyncEventService completion triggered");

        try{
            DriveId driveId = event.getDriveId();
            String driveResourceID = driveId.getResourceId();
            SyncToDriveService sd = new SyncToDriveService();

            switch (event.getStatus()) {
                case CompletionEvent.STATUS_CONFLICT:
                    Log.e(TAG, "STATUS_CONFLICT");
                    event.dismiss();
                    break;
                case CompletionEvent.STATUS_FAILURE:
                    Log.e(TAG, "STATUS_FAILURE");
                    String message = "Divertsy Sync Failed. You may need to reconnect Google Drive in Sync Settings.";
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    event.dismiss();
                    break;
                case CompletionEvent.STATUS_SUCCESS:
                    sd.SaveDriveID(driveResourceID, getApplicationContext());
                    event.dismiss();
                    break;
            }
        } catch (Exception e){
            Log.e(TAG, "Failed:" + e.getMessage());
        }

    }


}
