# Divertsy client application for Android

Divertsy is an open source system developed by the Office Hackers at Etsy to collect weight information about waste streams in local and remote offices. The complete version of Divertsy uses both a client application (found here) and a backend server application. The Divertsy client can work in a "stand alone" mode without the back end service. This is the easiest way to get started with Divertsy. 

![Divertsy Client on Android](https://cloud.githubusercontent.com/assets/714166/24930736/8bdda552-1ed8-11e7-9eba-660515d9d260.png)

## Getting Started

Ensure you have a supported Android tablet (see [Tablet Setup](https://github.com/etsy/divertsy-client/wiki/2)-Tablet-Setup)) and working USB scale which is connected to the tablet (see [Scale Setup](https://github.com/etsy/divertsy-client/wiki/3)-Scale-Setup)). Then compile the Divertsy client and sideload it on to the tablet. 

When the Divertsy client runs the first time, it will ask you to set an "Office Name". This is to help you keep track of which tablet is sending you data. The tablet's "Device ID" will also be recorded in case you setup multiple tablets with the same "Office Name". Click the back arrow to return to the main Divertsy client screen.

You may see a message to connect the USB scale. If the scale is connected, make sure the power is now on for the scale (remember, the scale should NOT have batteries or a power connect to the wall). You may get a pop-up message asking if you want to use the USB device you just connected. Click Yes to this message (optionally, click the box to always allow this application to use the device). Now when you add weight to the scale, the numbers shown on the Divertsy screen should match the numbers shown on the scale. When you have finished adding items to the scale, push the button on screen of the type of waste. The amount shown and the type chosen will be written to a data file along with the time.

## Additional Information

Please see the [Wiki](https://github.com/etsy/divertsy-client/wiki) for additional information about.


