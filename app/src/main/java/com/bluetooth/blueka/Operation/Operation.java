package com.bluetooth.blueka.Operation;

import android.bluetooth.BluetoothGatt;

public abstract class Operation {
    protected BluetoothGatt gatt;
    public Operation(BluetoothGatt gatt){
        this.gatt = gatt;
    }
    public abstract void performOperation();


}
