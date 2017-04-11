# Divertsy client application for Android

Divertsy is an open source system developed by the Office Hackers at Etsy to collect weight information about waste streams in local and remote offices. The complete version of Divertsy uses both a client application (found here) and a backend server application. The Divertsy client can work in a "stand alone" mode without the back end service. This is the easiest way to get started with Divertsy and works well if there is only one or two locations where data will be collected. 

## Getting Started

Ensure you have a supported Android tablet (see Tablet Setup) and working USB scale which is connected to the tablet (see Scale Setup). Then install the Divertsy client on the tablet. 

When the Divertsy client runs the first time, it will ask you to set an "Office Name". This is to help you keep track of which tablet is sending you data. The tablet's "Device ID" will also be recorded in case you setup multiple tablets with the same "Office Name". Click the back arrow to return to the main Divertsy client screen.

You may see a message to connect the USB scale. If the scale is connected, make sure the power is now on for the scale (remember, the scale should NOT have batteries or a power connect to the wall). You may get a pop-up message asking if you want to use the USB device you just connected. Click Yes to this message (optionally, click the box to always allow this application to use the device). Now when you add weight to the scale, the numbers shown on the Divertsy screen should match the numbers shown on the scale. When you have finished adding items to the scale, push the button on screen of the type of waste. The amount shown and the type chosen will be written to a data file along with the time.



## Setup Overview

Divertsy requires a number of items to be setup and configured for proper use and tracking of data. This includes:
* An Android Tablet
  * with the Divertsy apk loaded
  * with a DYMO USB Scale 
    * attached over Full Size USB or USB on-the-go OTG
  * with Google Drive installed or a 3rd party sync service (like FolderSync)


## Tablet Setup

Divertsy requires at least Android 4.0 “Ice Cream Sandwich” on the tablet (the client is built with the Android API version 14 as the minimum supported). The tablet must also support USB Host mode. Unfortunately, not all vendors will include this support and it maybe difficult to know if a tablet supports this prior to testing it out. In our experience, any tablet with a full size USB type A or USB type C port will include this feature. Additionally, a number of tablets which support USB OnTheGo (OTG) will work with an adapter, but may not support charging while using this adapter. Below is a list of tablets we have tested with Divertsy.

| Tablet   |  Working  | Full USB A Port | OTG |  Notes |
|----------|:---------:|:--------:|:---:|:------|
| Dell Venue 10 5000 (5050) |  YES | X | | 5 Ghz WiFi. No longer manufactured |
| Dragon Touch X10 | YES | X | | Bluetooth fails to see Beacons |
| RCA 10 Viking Pro| YES | X | | |
| Google Pixel C Tablet |  YES | |  | Requires USB C Hub |
| Amazon Fire 8 HD |  YES | | X | No Google Drive and does not charge with USB OTG |
| Nexus 7 (2012) |  YES | | X | Does not charge while reading data  |

Note OTG supported tablets require an OTG cable in order to connect the scale to the tablet.

## Scale Setup

Divertsy has been tested and works with DYMO's M10, S100, S250, and S400 USB scales. These scales should be connected via USB to the Android tablet running the Divertsy client. The scale should receive power via the tablet's USB connection. Therefore, ensure the scale is NOT plugged in via its own power adapter and does NOT contain batteries. Depending on your tablet, the scale might not power up unless the Divertsy application is running in the foreground on your Android tablet.

## Sync Setup

Divertsy writes its data to the "/sdcard/Documents/" folder on the tablet. This directory location should be accessible to any application with "External Storage" permissions on the Android tablet. This should allow support for any 3rd party syncing application. We recommend using [FolderSync] (https://play.google.com/store/apps/details?id=dk.tacit.android.foldersync.lite) on the tablet since this also supports a number of 3rd party services such as DropBox, Box, OneDrive, S3, and others. Note, the Documents folder may not exist on your tablet until after running the Divertsy client and recording at least one data point.

### Sync with Google Drive

Divertsy does include limited built-in syncing options with Google Drive. This feature will only work on devices with Google's built in Google Mobile Services which are not included on devices such as the Amazon Fire Tablet. If the sync setting is enabled, Divertsy will upload a CSV file to the root of the Google Drive user's account. After the first save, the file can be moved to a new folder and renamed using the Google Drive application. Since Divertsy holds on to the resource identify, the same file will be updated even after it is renamed and/or moved. 

It has been noticed, if you move the Divertsy file in Google Drive to the trash, but do not permanently delete the file, it can still get updated by the client. You will need to unlink the account in Divertsy's sync setting in order to prevent it from trying to update the file.

If the Divertsy data file is opened in Google Sheets, a copy of the csv data will be made and placed in a new file which may have the same name as the upload data file. Yes, this is confusing. We recommend renaming the sheets version of the file which often has a green icon verse the blue icon for the drive files. The sheets version will not get automatically updated, however the drive version of the file will be if this option is selected in the Divertsy application. The Google Drive account and file can be unlinked in the Sync settings of Divertsy.

Alternatively, the share button inside the Divertsy application will allow you to share the CSV file with other Android apps which can handle CSV data. This can be used to select an email client and send a copy of the collected Divertsy data. The exported data will always all contain all collected data points, not just the ones since the last shared event.

## Building from Source Code

Divertsy has been created using [Android Studio] (https://developer.android.com/studio/index.html). Once installed, you should be able to import this project and then build it with the lastest Android SDK and build tools. 

### Compiling with Google Drive support

If you are building from the source code and wish to use the Google Drive intergration, you will need to register your signing key(s) with Google. Follow the instructions under the section "Generate the signing certificate fingerprint and register your application" on [Google Drive API > Android] (https://developers.google.com/drive/android/auth). The default package name for Divertsy is "com.divertsy.hid". If you sync with a 3rd party application or do not use Google Drive, you do not need to perform this setup.

## Contributing

Please see the [Contributing.md] (CONTRIBUTING.md) file.

### Code of Conduct

We are committed to fostering a welcoming community. Any participant and
contributor is required to adhere to our [Code of Conduct](http://etsy.github.io/codeofconduct.html).

## FAQ

#### Will Divertsy work on an iPad
No. Divertsy is Android only for now. We are not aware of an easy way to connect 3rd party USB devices, such as the scale, to an iPad without going through Apple's MFI program.

#### Will Divertsy work with scales other than Dymo
In theory, yes. However, we are not currently aware of other scale brands in this price range that share their data over USB. If you have another brand and are looking to integrate it with Divertsy, view the setupScaleDataListener method in the UsbScaleManager class. The Dymo scales are a Low-Speed USB HID device which sends 6 bytes of data to indicate the weight and settings. Other brands will likely send their data in a different format and will need the code updated to handle this.

#### We always weight our items in a bin. Can that weight be subtracted from the total?
Yes. In the General settings of the Divertsy application is a setting for "Bin Weight". Make sure to set the amount you want subtracted and move the slider to "Use Bin Weight". Note, there is not setting for LBS or KG for the bin weight. You will need to ensure your scale does not change its measurement units while using the bin weight option or the data recorded will be incorrect.

#### Can Divertsy work offline without an Internet connection
Yes, for the Divertsy client. Data on each client is recorded locally and multiple data points can be collected prior to syncing. Syncing is done either manually through the share options in Divertsy or through a 3rd party sync client using its schedule/settings. We recommend using the FolderSync application which supports a number of sync services and controls over syncing options. If you use the built in Google Drive feature, data will be saved locally while offline, but then synced when an connection can be found.

#### What Android permissions are needed by the Divertsy client
Divertsy uses writing to External Storage in order to save its data in a location that can be accessed by 3rd party apps (such as sync services). USB Permission is required to read the scale data while Wake Lock and Key Guard permissions help to keep the tablet awake as data is being read. Optionally, Bluetooth permission can be granted to scan for location beacons. Bluetooth is disabled by default and can be enabled in the Divertsy settings.

#### What Bluetooth beacons are supported?
Divertsy looks for bluetooth beacons which use the Eddystone URL format. The BLEScanner class can be configured to look for specific host names in the URL and will only attempt to parse those beacons. By default, the hostname is "HAX" and the remaining URL is parsed for location data. For example a beacon received with "http://HAX/F4/Kitchen" would record that the data was collected from the "Kitchen" area of the "4th Floor". Additional location beacons can be deployed without needed to change the application, however, if you would like to use a different hostname or URL parsing, you will need to recompile the Divertsy client application.

#### Are remote updates supported?
Yes, however they do not go through the Play Store or other market places. The update is triggered by pushing a new version of the application to the "/sdcard/Divertsy/" folder on the tablet with a file name "Divertsy######.apk" where "######" is a version number greater than the current version of the application. The current Divertsy version number can be seen at the top of the screen when the application runs. By default, this will be the date (Year, Month, Date) which the client code was compiled. The user will be prompted to install the new update when Divertsy restarts and will require that the package is signed with the same key as the currently installed Divertsy application, and that the permission to install from unknown locations is also enabled in the Android tablet settings.

You may want to use the FolderSync application to allow you to sync out application updates to this folder. The choice of user setting in Divertsy should be maintained between upgrades unless a different signing key is used.

#### Can I setup our own default list of office names?
Yes it can be by recompiling the Divertsy client code. Edit the [divertsy_default_strings.xml] (divertsy_client/src/main/res/values/divertsy_default_strings.xml) file and follow the example for the  "pref_offices" string-array. 

#### Can I change which default waste streams are selected?
Yes. On each client, you can select which waste streams buttons to show by selecting them in the "Set Waste Streams" option in the "Location" settings. The default choices and button colors can be changed by recompiling the Divertsy client code. Edit the [raw/waste_streams.json] (divertsy_client/src/main/res/raw/waste_streams.json) file to add or change waste stream buttons.

#### What languages are supported?
Currently, the default waste stream buttons are translated into English, Spanish, French, and German. You can set the language for the buttons in Divertsy's settings, then "Location", then "Language (for buttons)". These translations are stored in the [raw/waste_streams.json] (divertsy_client/src/main/res/raw/waste_streams.json) file. Data is still recorded with English names for the waste streams since this is needed for processing.

The application will show error messages in the default lanuage set for the Android tablet (under Android's "settings" then "language" option). Currently, these are in English, French, and German. However, some settings and messages are currently only in English. To add additional languages and translations, see the [strings.xml] (divertsy_client/src/main/res/values/strings.xml) files in the client source code. 

#### Can data be saved to a different location than the "/sdcard/"
Yes, if you recompile the application. This can be changed in the source code in the Utils class. Look for the LOG_BASE_DIR and LOG_FILENAME variables.

#### Which IDE is used for developing the Divertsy client
Android Studio has been used for developing the Divertsy client. Use the "Import Project" settings to load the client code and build your own version.

#### Can data be sent from other applications?
Yes. Divertsy includes a BroadcastReceiver which will look for weight data being sent from other applications on the device. It will still require a user to choose a waste stream in the Divertsy application. Below is an example of how to send the broadcast message to Divertsy.

```java
Intent sendIntent = new Intent();
sendIntent.setAction("com.divertsy.REMOTE_SCALE_WEIGHT");
sendIntent.putExtra("floatScaleWeight", fScaleWeightValue);
sendIntent.putExtra("stringScaleUnit", "KG");
sendBroadcast(sendIntent);
```

#### Will Divertsy work with the DYMO S25 scale?
Sort of. There is code to support the Dymo S25 scale in the Divertsy client (data is recorded in grams or ounces), however, we have seen issues with successfully using some S25 scales. While the scale will turn on with USB power and even show a proper USB endpoint, it doesn't always send readable data to the tablet. We have had success with newly purchased S25 scales, so this may just be an issue with some older scales. 
