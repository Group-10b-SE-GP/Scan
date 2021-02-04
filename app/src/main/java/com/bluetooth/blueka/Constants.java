package com.bluetooth.blueka;

import java.util.UUID;

public class Constants {
    public static String SERVICE_STRING = "7D2EA28A-F7BD-485A-BD9D-92AD6ECFE93E";
    public static UUID SERVICE_UUID = UUID.fromString(SERVICE_STRING);

    public static final String TAG = "BlueKa";
    public static final String FIND = "Find BlueKa Devices";
    public static final String STOP_SCANNING = "Stop Scanning";
    public static final String SCANNING = "Scanning";

    //write
    public static String CHARACTERISTIC_ECHO_STRING = "7D2EBAAD-F7BD-485A-BD9D-92AD6ECFE93E";
    public static UUID CHARACTERISTIC_ECHO_UUID = UUID.fromString(CHARACTERISTIC_ECHO_STRING);
}
