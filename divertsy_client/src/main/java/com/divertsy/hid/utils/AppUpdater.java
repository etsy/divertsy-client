package com.divertsy.hid.utils;

import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

/**
 *  AppUpdater is called when application starts and checks for a new APK in a specific directory.
 *  This allows remote updates if using a 3rd party syncing tool.
 */
public class AppUpdater {

    private static final String TAG = AppUpdater.class.getName();

    private static final String SD_CARD_PATH = Environment.getExternalStorageDirectory().getPath() + "/Divertsy/";

    @Nullable
    public static File checkAppUpdate() {
        Log.d(TAG, "Entering Update Check");

        // start at the set path
        String filePath = SD_CARD_PATH;
        File folder = new File(filePath);
        File[] files = folder.listFiles();

        if (files != null && files.length > 0) {
            try {
                Arrays.sort(files, new Comparator() {
                    public int compare(Object o1, Object o2) {

                        Integer loc = o1.toString().indexOf(".");
                        String file1tag = o1.toString().substring(loc - 6, loc);
                        loc = o2.toString().indexOf(".");
                        String file2tag = o1.toString().substring(loc - 6, loc);

                        if (Integer.parseInt(file1tag) > Integer.parseInt(file2tag)) {
                            return -1;
                        } else if (Integer.parseInt(file1tag) < Integer.parseInt(file2tag)) {
                            return +1;
                        } else {
                            return 0;
                        }
                    }

                });

                if (files[0].exists()) {
                    filePath = files[0].toString();
                    Log.d(TAG, "Found update file:" + filePath);
                    Integer loc = filePath.indexOf(".");
                    if (Integer.parseInt(filePath.substring(loc - 6, loc)) > Utils.getBuildNumber()) {
                        Log.i(TAG, "Starting Update from File: " + filePath);
                        return new File(filePath);
                    }

                } else {
                    Log.d(TAG, "No update files found.");
                }
            } catch (Exception e) {
                Log.e(TAG, "UPDATE CHECK FAIL. Check for INVALID file in Update Folder!");
                Log.e(TAG, e.toString());
            }
        }

        return null;
    }
}
