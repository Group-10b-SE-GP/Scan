package com.bluetooth.blueka.Operation;

import android.bluetooth.BluetoothGatt;

public class WriteCharacteristicOperation extends Operation {
    private String message;
    public WriteCharacteristicOperation(BluetoothGatt gatt, String message){
        super(gatt);
    }

    @Override
    public void performOperation() {

    }
}
