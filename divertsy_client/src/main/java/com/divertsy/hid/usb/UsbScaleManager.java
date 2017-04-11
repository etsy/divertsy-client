package com.divertsy.hid.usb;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.divertsy.hid.R;
import com.divertsy.hid.utils.Utils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class UsbScaleManager {

    /*
      This Array defines the values which the DYMO scales send for the current
      unit of measurement of the connected scale. A user can change this value
      at anytime on the scale, so we must check its value when we record weight
      information. The S100, S250, and S400 scales should only report "KG" and "LBS"
     */
    public String WEIGHTUNIT[] = {"UNKNOWN", "MG", "G", "KG", "CD", "TAELS", "GR", "DWT", "TONNES", "TONS", "OZT", "OZ", "LBS"};

    /*
      This defines the RAW HID data which the DYMO scales send. These values
      will most likely need to change when used with other types of scales.
     */
    private static int RHID_NEGATIVE_FLAG    = 1;
    private static int RHID_UNIT_OF_MEASURE  = 2;
    private static int RHID_WEIGHT_LOW_BYTE  = 4;
    private static int RHID_WEIGHT_HIGH_BYTE = 5;

    long USB_READ_RATE = 200; //Time between scale reads in milliseconds



    private static final String TAG = UsbScaleManager.class.getName();
    private double mAddToScaleWeight;
    private final Callbacks mCallbacks;
    private ScaleMeasurement mLatestMeasurement;

    public ScaleMeasurement getLatestMeasurement() {
        return mLatestMeasurement;
    }

    public void setAddToScaleWeight(Double newWeight) {
        mAddToScaleWeight = newWeight;
    }

    public double getAddToScaleWeight() {
        return mAddToScaleWeight;
    }

    public interface Callbacks {
        void onMeasurement(ScaleMeasurement measurement);
    }

    private static final String ACTION_USB_PERMISSION = "com.google.android.HID.action.USB_PERMISSION";

    private UsbDevice device;
    private UsbManager mUsbManager;

    private UsbInterface intf;
    private UsbEndpoint endPointRead;
    private UsbEndpoint endPointWrite;
    private UsbDeviceConnection connection;
    private int packetSize;
    private PendingIntent mPermissionIntent;
    private Timer myTimer = new Timer();
    private final Handler uiHandler = new Handler();

    private AlertDialog adScaleWarning;

    /*
     * This gets called if a USB Device is plugged in or removed while our app is running
     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Entered BroadcastReceiver onReceive");
            String action = intent.getAction();
            Log.d(TAG, "Action was: " + action);

            if (ACTION_USB_PERMISSION.equals(action)) {
                //TODO: maybe check for failure?
                synchronized (this) {
                    device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    setUSBDevice(device);
                }
            }
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                synchronized (this) {
                    device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (mUsbManager.hasPermission(device)) {
                        setUSBDevice(device);
                    } else {
                        mUsbManager.requestPermission(device, mPermissionIntent);
                    }
                }
                if (device == null) {
                    Log.d(TAG, "device connected");
                }
            }
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                if (device != null) {
                    device = null;
                }
                Log.d(TAG, "device disconnected");
            }
        }

    };

    public UsbScaleManager(Context context, Intent intent, Callbacks callbacks, Bundle savedInstanceState) {
        mCallbacks = callbacks;

        mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);

        mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(intent.getAction())) {
            setUSBDevice(device);
        } else {
            searchForDevice(context);
        }
    }

    public void onStart(Context context) {
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        context.registerReceiver(mUsbReceiver, filter);

        setupScaleDataListener();
    }

    public void onStop(Context context) {
        try {
            context.unregisterReceiver(mUsbReceiver);
        } catch (Exception e) {
            Log.e(TAG, "OnStop:" + e.getMessage());
        }
    }

    public HashMap<String, UsbDevice> getDeviceList() {
        return mUsbManager.getDeviceList();
    }

    /*
     * This gets called if a USB Device is plugged in or removed while our app is running
     */
    private void setUSBDevice(UsbDevice device) {
        Log.d(TAG, "Selected device VID:" + Integer.toHexString(device.getVendorId()) + " PID:" + Integer.toHexString(device.getProductId()));

        // Close this since we should now have a USB device
        if (adScaleWarning != null && adScaleWarning.isShowing()) {
            adScaleWarning.dismiss();
        }

        connection = mUsbManager.openDevice(device);
        Log.d(TAG, "USB Interface count: " + device.getInterfaceCount());
        intf = device.getInterface(0);
        if (null == connection) {
            Log.e(TAG, "USB Error - unable to establish connection");
        } else {
            connection.claimInterface(intf, true);
        }
        try {
            Log.d(TAG, "Interface endpoints: " + intf.getEndpointCount());
            if (UsbConstants.USB_DIR_IN == intf.getEndpoint(0).getDirection()) {
                endPointRead = intf.getEndpoint(0);
                packetSize = endPointRead.getMaxPacketSize();
                Log.d(TAG, "USB PacketSIZE: " + packetSize );
            }
        } catch (Exception e) {
            Log.wtf(TAG, "Device have no endPointRead. WHAT DID YOU PLUG IN?", e);
        }
    }

    private void searchForDevice(Context context) {
        HashMap<String, UsbDevice> devices = mUsbManager.getDeviceList();
        UsbDevice selected = null;
        int num_of_devices = devices.size();

        if (num_of_devices == 1) {
            //If there's only one device, go ahead and connect. YOLO to keyboards!
            for (UsbDevice device : devices.values()) {
                selected = device;
            }

            if (mUsbManager.hasPermission(selected)) {
                setUSBDevice(selected);
            } else {
                mUsbManager.requestPermission(selected, mPermissionIntent);
            }
        } else {
            if (num_of_devices > 1) {
                Log.wtf(TAG, "Extra devices are plugged in. Found: " + num_of_devices);
            }
            showListOfDevices(context);
        }

    }

    /*
     * This should no longer get called unless the user has done something strange
     * to have more than one USB device connect. This could happen if they use a
     * USB Hub.
     */
    void showListOfDevices(Context context) {

        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);

        if (getDeviceList().isEmpty()) {
            alertBuilder.setTitle(R.string.usb_connect_title)
                    .setPositiveButton(R.string.ok, null);
        } else {
            alertBuilder.setTitle(R.string.usb_select_title);
            List<CharSequence> list = new LinkedList<>();
            for (UsbDevice usbDevice : getDeviceList().values()) {
                list.add("devID:" + usbDevice.getDeviceId() + " VID:" + Integer.toHexString(usbDevice.getVendorId()) + " PID:" + Integer.toHexString(usbDevice.getProductId()) + " " + usbDevice.getDeviceName());
            }
            final CharSequence devicesName[] = new CharSequence[getDeviceList().size()];
            list.toArray(devicesName);
            alertBuilder.setItems(devicesName, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    UsbDevice device = (UsbDevice) getDeviceList().values().toArray()[which];
                    mUsbManager.requestPermission(device, mPermissionIntent);
                }
            });
        }
        alertBuilder.setCancelable(true);
        adScaleWarning = alertBuilder.show();
    }

    /**
     * This handles the raw USB data packet from the scale and decodes it to a readable format.
     * Only tested with 2 scales, so may need to update this if the hardware changes.
     */
    private void setupScaleDataListener() {
        myTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (connection != null && endPointRead != null) {
                        final byte[] buffer = new byte[packetSize];
                        final int status = connection.bulkTransfer(endPointRead, buffer, packetSize, 300);
                        uiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                ScaleMeasurement.Builder measurementBuilder = new ScaleMeasurement.Builder();
                                if (status >= 0) {

                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append("DEBUG USB IN:");
                                    for (int i = 0; i < packetSize; i++) {
                                        stringBuilder.append(" ").append(String.valueOf(Utils.toInt(buffer[i])));
                                    }

                                    if (packetSize >= 5) {
                                        double weight = (256 * Utils.toInt(buffer[RHID_WEIGHT_HIGH_BYTE]))
                                                + Utils.toInt(buffer[RHID_WEIGHT_LOW_BYTE]);

                                        if (Utils.toInt(buffer[RHID_UNIT_OF_MEASURE]) < WEIGHTUNIT.length){
                                            measurementBuilder.units(WEIGHTUNIT[Utils.toInt(buffer[RHID_UNIT_OF_MEASURE])]);
                                            if (!WEIGHTUNIT[Utils.toInt(buffer[RHID_UNIT_OF_MEASURE])].equals("G")) {
                                                //This is correct for at least LBS, OZ, and KG... maybe others?
                                                weight = weight * 0.1;
                                            }
                                        } else {
                                            Log.e(TAG, "USB DATA ERROR - RHID_UNIT_OF_MEASURE not a known value in WEIGHTUNIT array");
                                        }

                                        //Fix edge cases of double weight
                                        weight = Utils.round(weight, 1);

                                        //Check for Negative Numbers
                                        if (Utils.toInt(buffer[RHID_NEGATIVE_FLAG]) == 5) {
                                            // Int 5 seems to indicate a negative value on S250 scales
                                            // however, column 5 is still positive
                                            weight = 0 - weight;
                                        }

                                        //Remove any default values from the weight
                                        measurementBuilder.rawScaleWeight(weight);
                                        weight = weight - mAddToScaleWeight;
                                        weight = Utils.round(weight, 1);

                                        stringBuilder.append(" Weight: ").append(String.valueOf(weight));

                                        measurementBuilder.scaleWeight(weight);

                                    } else {
                                        stringBuilder.append("ERROR: USB packetSize too small");
                                    }

                                    mLatestMeasurement = measurementBuilder.build();

                                    Log.v(TAG, stringBuilder.toString());

                                    mCallbacks.onMeasurement(mLatestMeasurement);
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Exception: " + e.getLocalizedMessage());
                }
            }
        }, 0L, USB_READ_RATE);
    }
}
