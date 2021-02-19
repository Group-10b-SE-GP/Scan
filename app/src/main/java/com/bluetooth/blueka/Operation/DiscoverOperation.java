package com.bluetooth.blueka.Operation;

import android.bluetooth.BluetoothGatt;

public class DiscoverOperation extends Operation {
    public DiscoverOperation(BluetoothGatt gatt){
        super(gatt);
    }

    @Override
    public void performOperation() {
        gatt.discoverServices();
    }
}
