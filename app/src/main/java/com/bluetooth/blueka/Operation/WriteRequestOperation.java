package com.bluetooth.blueka.Operation;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import com.bluetooth.blueka.Constants;

import java.io.UnsupportedEncodingException;

import static com.bluetooth.blueka.Constants.TAG;

public class WriteRequestOperation extends Operation {
    private String message;
    public WriteRequestOperation(BluetoothGatt gatt, String message){
        super(gatt);
        this.message = message;

    }

    @Override
    public void performOperation() {
        BluetoothGattService service = gatt.getService(Constants.SERVICE_UUID);
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(Constants.CHARACTERISTIC_ECHO_UUID);
        byte[] messageBytes = new byte[0];
        try {
            messageBytes = message.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Failed to convert message string to byte array");
        }

        characteristic.setValue(messageBytes);
        boolean success = gatt.writeCharacteristic(characteristic);
        // to add the write characteristic
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        gatt.setCharacteristicNotification(characteristic, true);
        //
        if(success){
            Log.i(TAG,"success");
        }else{
            Log.i(TAG, "not success");
        }

    }
}
