package com.divertsy.hid;

import android.app.Application;
import android.provider.Settings;

/**
 *  ScaleApplication for getting self and the DeviceID
 */
public class ScaleApplication extends Application {

    private static ScaleApplication self;

    @Override
    public void onCreate() {
        super.onCreate();
        self = this;
    }

    public static ScaleApplication get() {
        return self;
    }

    public String getDeviceId() {
        return Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
    }
}
