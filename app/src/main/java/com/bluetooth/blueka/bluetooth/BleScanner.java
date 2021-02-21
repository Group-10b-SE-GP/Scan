package com.bluetooth.blueka.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
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
import com.bluetooth.blueka.Operation.DisconnectOperation;
import com.bluetooth.blueka.Operation.DiscoverOperation;
import com.bluetooth.blueka.Operation.GattCloseOperation;
import com.bluetooth.blueka.Operation.OperationManager;
import com.bluetooth.blueka.Operation.WriteRequestOperation;
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
    private OperationManager operationManager;
    private GattClientCallback gattClientCallback;
    private boolean mInitialized = false;
    private BleAdvertiser bleAdvertiser;
    private Boolean checkScan = false;

    //gatt.close() is needed after gatt.disconnect().
    //However, the onConnectionStateChange is not triggered sometimes to close the gatt.
    //We need a Runnable to run it if the onConnectionStateChange is not triggered.
    //If onConnectionStateChange works, it will cancel this Runnable which also close the gatt.
    private Runnable GattCloseRun= new Runnable(){
        @Override
        public void run()
        {   operationManager.operationCompleted();
            operationManager.request(new GattCloseOperation(mGatt));
            operationManager.operationCompleted();
        }
    };

    //Constructor.
    public BleScanner(Context context){
        this.context = context;
        operationManager = new OperationManager();
        gattClientCallback = new GattClientCallback();
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

    //startScanning is call when scanner start scanning.
    public void startScanning(final ScanResultsConsumer scan_results_consumer, long stop_after_ms){
        if(scanning){
            Log.d(Constants.TAG, "Already scanning so ignoring startScanning request");
            return;
        }
        if(mConnected){

            disconnectGattServer();
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
                    if(checkScan != Boolean.TRUE){
                        bleAdvertiser = new BleAdvertiser(context);
                        bleAdvertiser.startAdvertising();
                    }else{
                        checkScan = false;
                    }
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
    //stopScanning is call when scanner stop scanning.
    public void stopScanning(){
        setScanning(false);
        Log.d(Constants.TAG,"Stopping scanning");
        scanner.stopScan(scan_callback);
    }

    //Define what happens after scanner have some scan result, also where the scan result store.
    //Connect the device when a device is found.
    private ScanCallback scan_callback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            checkScan = true;
            if(!scanning){
                return;
            }
                scan_results_consumer.candidateBleDevice(result.getDevice(), result.getScanRecord().getBytes(), result.getRssi());
                scanner.stopScan(scan_callback);
                BluetoothDevice bluetoothDevice = result.getDevice();
                connectDevice(bluetoothDevice);
        }

    };

    private void connectDevice(BluetoothDevice device){
        mGatt = device.connectGatt(context, false, gattClientCallback);
        Log.i(TAG,"connected inside");
    }

    public boolean isScanning(){
        return scanning;
    }

    // setScanning is called when it start scanning or stop scanning
    // To adjust the UI, and variable.
    private void setScanning(boolean scanning){
        this.scanning = scanning;
        if(!scanning){
            scan_results_consumer.scanningStopped();
        }else{
            scan_results_consumer.scanningStarted();
        }
    }




    // GattClientCallback is very important thing to define.
    // Whatever happen during the connection is conducted here.
    // onConnectionStateChange is called when device is connected or disconnected.
    // onServiceDiscovered is called when service of connected device is found.
    // onCharacteristicChanged is called when the advertiser notify.
    // onCharacteristicWrite is called when writeRequest is successfully sent.
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
                operationManager.request(new DiscoverOperation(gatt));
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED){
                handler.removeCallbacks(GattCloseRun);
                operationManager.operationCompleted();
                operationManager.request(new GattCloseOperation(mGatt));
                operationManager.operationCompleted();
                mConnected = false;
                Log.i(TAG,"CLOSED GATT");
            }
        }
        //write
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status){
            super.onServicesDiscovered(gatt, status);
            operationManager.operationCompleted();
            if (status != BluetoothGatt.GATT_SUCCESS) {
                return;
            }
            String message = "hello";
            operationManager.request(new WriteRequestOperation(gatt, message));
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
                    //MainActivity.getInstance().test();
                }
            });
        }
        @Override
        public void onCharacteristicWrite (BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic,
                                           int status){
            super.onCharacteristicWrite(gatt, characteristic, status);
            operationManager.operationCompleted();
            Log.i(TAG,"YES, another queue works");
        }
    }

    public void disconnectGattServer(){
        mConnected = false;
        mInitialized = false;
        if(mGatt != null){
            operationManager.request(new DisconnectOperation(mGatt));
            handler.postDelayed(GattCloseRun,2000);
        }
    }
}
