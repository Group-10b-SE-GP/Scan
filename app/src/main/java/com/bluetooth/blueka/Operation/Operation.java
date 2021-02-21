package com.bluetooth.blueka.Operation;

import android.bluetooth.BluetoothGatt;

// All this operation is BLE operation, we just arrange the operations as queue to avoid any
// operation is overwritten. Stabilize the performance.
public abstract class Operation {
    protected BluetoothGatt gatt;
    public Operation(){

    }
    public Operation(BluetoothGatt gatt){
        this.gatt = gatt;
    }
    public abstract void performOperation();


}
