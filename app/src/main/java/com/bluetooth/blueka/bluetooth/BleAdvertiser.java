package com.bluetooth.blueka.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.bluetooth.blueka.Constants;
import com.bluetooth.blueka.Operation.OperationManager;
import com.bluetooth.blueka.Operation.WriteCharacteristicOperation;
import com.bluetooth.blueka.R;

import java.io.UnsupportedEncodingException;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.UUID;

import static android.content.Context.BLUETOOTH_SERVICE;
import static com.bluetooth.blueka.Constants.TAG;

public class BleAdvertiser {
    private OperationManager operationManager = new OperationManager();
    private BluetoothLeAdvertiser advertiser;
    private Handler hander = new Handler();
    private BluetoothManager mBluetoothManager;
    private BluetoothGattServer mGattServer;
    private Context context;
    private boolean advertising =  false;
    private static ArrayList<BluetoothDevice> mDevices = new ArrayList();





    public BleAdvertiser(Context context){
        this.context = context;
        advertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();
        mBluetoothManager = (BluetoothManager) context.getSystemService(BLUETOOTH_SERVICE);
        GattServerCallback gattServerCallback = new GattServerCallback();
        mGattServer = mBluetoothManager.openGattServer(context, gattServerCallback);
        setupServer();
    }

    private void setupServer(){
        BluetoothGattService service = new BluetoothGattService(Constants.SERVICE_UUID,BluetoothGattService.SERVICE_TYPE_PRIMARY);
        //write
        BluetoothGattCharacteristic writeCharacteristic = new BluetoothGattCharacteristic(
                Constants.CHARACTERISTIC_ECHO_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE);
        service.addCharacteristic(writeCharacteristic);
        mGattServer.addService(service);
    }

    public void startAdvertising(){
        if(advertising){
            Log.d(Constants.TAG, "Already advertising");
            return;
        }
        if(advertiser == null){
            Log.d(Constants.TAG, "Bluetooth advertiser failed.");
            return;
        }
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode( AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY )
                .setTxPowerLevel( AdvertiseSettings.ADVERTISE_TX_POWER_HIGH )
                .setConnectable(true)
                .build();

        ParcelUuid pUuid = new ParcelUuid(Constants.SERVICE_UUID);
        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName( false )
                .addServiceUuid( pUuid )
                .build();


        advertiser.startAdvertising( settings, data, advertisingCallback );
        setAdvertising(true);


    }
    private AdvertiseCallback advertisingCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.d(Constants.TAG,"Advertise success");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.e( "BLE", "Advertising onStartFailure: " + errorCode );
            super.onStartFailure(errorCode);
        }
    };

    public void stopAdvertising(){
        setAdvertising(false);
        advertiser.stopAdvertising(advertisingCallback);
    }

    public boolean isAdvertising(){return advertising;}

    void setAdvertising(boolean advertising){
        this.advertising = advertising;
    }

    private class GattServerCallback extends BluetoothGattServerCallback{
        //write
        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device,
                                                 int requestId,
                                                 BluetoothGattCharacteristic characteristic,
                                                 boolean preparedWrite,
                                                 boolean responseNeeded,
                                                 int offset,
                                                 byte[] value) {
            super.onCharacteristicWriteRequest(device,
                    requestId,
                    characteristic,
                    preparedWrite,
                    responseNeeded,
                    offset,
                    value);
            //advertiser will see the number of connected device and notify every phone.
            Log.i(TAG,"inside on characterisitic write request in advertiser");
            if (characteristic.getUuid().equals(Constants.CHARACTERISTIC_ECHO_UUID)) {
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
                int num_connected = mDevices.size();
                String message = Integer.toString(num_connected);
                byte[] reply = new byte[0];

                try {
                    reply = message.getBytes("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    Log.e(TAG, "Failed to convert message string to byte array");
                }
                characteristic.setValue(reply);

                for(BluetoothDevice dev : mDevices) {
                    operationManager.request(new WriteCharacteristicOperation(mGattServer, characteristic, dev));
                }
                //
//                int num_connected = mDevices.size();
//                String message = Integer.toString(num_connected);
//                byte[] reply = new byte[0];
//                try {
//                    reply = message.getBytes("UTF-8");
//                } catch (UnsupportedEncodingException e) {
//                    Log.e(TAG, "Failed to convert message string to byte array");
//                }
//
//                characteristic.setValue(reply);
//                for(BluetoothDevice dev : mDevices) {
//                    mGattServer.notifyCharacteristicChanged(dev, characteristic, false);
//                }
            }
        }
        @Override
        public void onConnectionStateChange (BluetoothDevice device,
                                             int status,
                                             int newState){
            super.onConnectionStateChange(device, status, newState);
            Log.i(TAG,"HEREWEGO");
            if (newState == BluetoothProfile.STATE_CONNECTED){
                if(!mDevices.contains(device)) {
                    mDevices.add(device);
                    Log.i(TAG,"Yes, device added");
                }
            }else if(newState == BluetoothProfile.STATE_DISCONNECTED){
                mDevices.remove(device);
                Log.i(TAG,"Yes, device removed");
            }
        }
        @Override
        public void onNotificationSent (BluetoothDevice device,
                                                 int status){
            super.onNotificationSent(device,status);
            operationManager.operationCompleted();

        }
    }


    private void stopServer(){
        if(mGattServer != null){
            mGattServer.close();
        }
    }

}
