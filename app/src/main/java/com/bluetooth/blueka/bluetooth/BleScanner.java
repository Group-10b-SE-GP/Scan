package com.bluetooth.blueka.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;

import com.bluetooth.blueka.Constants;
import com.bluetooth.blueka.ui.MainActivity;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import static com.bluetooth.blueka.Constants.SERVICE_UUID;
import static com.bluetooth.blueka.Constants.TAG;

public class BleScanner {
    private BluetoothLeScanner scanner = null;
    private BluetoothAdapter bluetooth_adapter = null;
    private BluetoothGatt mGatt;
    private Handler handler = new Handler();
    private ScanResultsConsumer scan_results_consumer;
    private Context context;
    private boolean scanning = false;
    private String device_name_start = "";
    private boolean mConnected = false;
    //write
    private boolean mInitialized = false;

    public BleScanner(Context context){
        this.context = context;


        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);

        bluetooth_adapter = bluetoothManager.getAdapter();

        // check bluetooth is available and on.
        if(bluetooth_adapter == null || !bluetooth_adapter.isEnabled()){
            Log.d(Constants.TAG, "Bluetooth is NOT switched on");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBtIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(enableBtIntent);
        }
        Log.d(Constants.TAG, "Bluetooth is switched on");
    }

    public void startScanning(final ScanResultsConsumer scan_results_consumer, long stop_after_ms){
        if(scanning){
            Log.d(Constants.TAG, "Already scanning so ignoring startScanning request");
            return;
        }
        if(scanner == null){
            scanner = bluetooth_adapter.getBluetoothLeScanner();
            Log.d(Constants.TAG, "Created Bluetooth object");
        }
        handler.postDelayed(new Runnable(){
            @Override
            public void run()
            {
                if(scanning){
                    Log.d(Constants.TAG, "Stopping scanning");
                    scanner.stopScan(scan_callback);
                    setScanning(false);
                }
            }

        }, stop_after_ms);

        this.scan_results_consumer = scan_results_consumer;
        Log.d(Constants.TAG,"Scanning");
        List<ScanFilter> filters;
        //Filtering the scan results.
        filters = new ArrayList<ScanFilter>();
        ScanFilter scanFilter =  new ScanFilter.Builder().setServiceUuid(new ParcelUuid(SERVICE_UUID)).build();
        filters.add(scanFilter);
        //Scan settings.
        ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();

        setScanning(true);
        scanner.startScan(filters, settings, scan_callback);
    }

    public void stopScanning(){
        setScanning(false);
        Log.d(Constants.TAG,"Stopping scanning");
        scanner.stopScan(scan_callback);
    }

    private ScanCallback scan_callback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if(!scanning){
                return;
            }
            scan_results_consumer.candidateBleDevice(result.getDevice(),result.getScanRecord().getBytes(),result.getRssi());
            scanner.stopScan(scan_callback);
            BluetoothDevice bluetoothDevice = result.getDevice();
            connectDevice(bluetoothDevice);
        }

    };

    public boolean isScanning(){
        return scanning;
    }

    private void setScanning(boolean scanning){
        this.scanning = scanning;
        if(!scanning){
            scan_results_consumer.scanningStopped();
        }else{
            scan_results_consumer.scanningStarted();
        }
    }

    private void connectDevice(BluetoothDevice device){
        GattClientCallback gattClientCallback = new GattClientCallback();
        mGatt = device.connectGatt(context, false, gattClientCallback);
        Log.i(TAG,"connected inside");
    }


    private class GattClientCallback extends BluetoothGattCallback{
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState){
            super.onConnectionStateChange(gatt, status, newState);
            if (status == BluetoothGatt.GATT_FAILURE){
                disconnectGattServer();
                return;
            } else if (status != BluetoothGatt.GATT_SUCCESS){
                disconnectGattServer();
                return;
            }
            if (newState == BluetoothProfile.STATE_CONNECTED){
                mConnected = true;
                //write
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED){
                disconnectGattServer();
            }
        }
        //write
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status){
            super.onServicesDiscovered(gatt, status);
            if (status != BluetoothGatt.GATT_SUCCESS) {
                return;
            }
            BluetoothGattService service = gatt.getService(Constants.SERVICE_UUID);
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(Constants.CHARACTERISTIC_ECHO_UUID);
            String message = "hello";
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
            mInitialized = gatt.setCharacteristicNotification(characteristic, true);
            //
            if(success){
                Log.i(TAG,"success");
            }else{
                Log.i(TAG, "not success");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.i(TAG, "entered into notified");
            byte[] messageBytes = characteristic.getValue();
            String messageString = null;
            try {
                messageString = new String(messageBytes, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Unable to convert message bytes to string");
            }
            //receiving the message, will be reversed because the advertiser reverse it just to see the difference
            Log.d("Receive message", messageString);
            final String result = messageString;
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context,result,Toast.LENGTH_LONG).show();
                }
            });

        }
    }

    public void disconnectGattServer(){
        mConnected = false;
        if(mGatt != null){
            mGatt.disconnect();
            mGatt.close();
        }
    }

}
