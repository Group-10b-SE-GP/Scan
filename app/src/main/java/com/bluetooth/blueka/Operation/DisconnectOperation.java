package com.bluetooth.blueka.Operation;

import android.bluetooth.BluetoothGatt;

public class DisconnectOperation extends Operation{
    public DisconnectOperation(BluetoothGatt gatt){
        super(gatt);

    }
    @Override
    public void performOperation() {
        gatt.disconnect();
    }
}
