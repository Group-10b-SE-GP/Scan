package com.bluetooth.blueka.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bluetooth.blueka.Constants;
import com.bluetooth.blueka.R;
import com.bluetooth.blueka.bluetooth.BleAdvertiser;
import com.bluetooth.blueka.bluetooth.BleScanner;
import com.bluetooth.blueka.bluetooth.ScanResultsConsumer;

import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements ScanResultsConsumer {
    private boolean ble_scanning = false;
    private Handler handler = new Handler();
    private ListAdapter ble_device_list_adapter;
    private BleScanner ble_scanner;
    private static final long SCAN_TIMEOUT = 5000;
    private static final int REQUEST_LOCATION = 0;
    private static String[] PERMISSION_LOCATION = {Manifest.permission.ACCESS_COARSE_LOCATION};
    private boolean permission_granted = false;
    private int device_count =0;
    private Toast toast;
    private static MainActivity instance;

    static class ViewHolder{
        public TextView text;
        public TextView bdaddr;
    }

    //onCreate is called when you open the app.
    //savedInstanceState is what the app store when it get closed before.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        setContentView(R.layout.activity_main);
        setButtonText();

        //Create a listview and connect it to the interface's list, the listview get data from list adapter.
        ble_device_list_adapter = new ListAdapter();
        ListView listView = (ListView) this.findViewById(R.id.deviceList);
        listView.setAdapter(ble_device_list_adapter);


        //Create a scanner.
        ble_scanner = new BleScanner(this.getApplicationContext());

        //Now the button wait and listen for click to action.
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (ble_scanning){
                    setScanState(false);
                    ble_scanner.stopScanning();
                }
                BluetoothDevice device = ble_device_list_adapter.getDevice(position);
                if(toast != null){
                    toast.cancel();
                }
                Intent intent =  new Intent(MainActivity.this, PeripheralControlActivity.class);
                intent.putExtra(PeripheralControlActivity.EXTRA_NAME, device.getName());
                intent.putExtra(PeripheralControlActivity.EXTRA_ID, device.getAddress());
                startActivity(intent);
            }
        });
    }

    //Initialise the button text.
    private void setButtonText(){
        String text = "";
        text = Constants.FIND;
        final String button_text = text;
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                ((TextView) MainActivity.this.findViewById(R.id.scanButton)).setText(button_text);
            }
        });
    }

    //set to which to scan and connect
    public UUID numOfConnectWanted(){
        //if(seekBarNum == 2){
        //    return Constants.SERVICE_UUID2;
        //}else if(seekBarNum == 3){
        //    return Constants.SERVICE_UUID3;
        // }
        return Constants.SERVICE_UUID;
    }



    public static MainActivity getInstance() {
        return instance;
    }

    //This will be call by the scanner when scanner scan something.
    //This notify the list to display the device scanned.
    @Override
    public void candidateBleDevice(BluetoothDevice device, byte[] scan_record, int rssi) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ble_device_list_adapter.addDevice(device);
                ble_device_list_adapter.notifyDataSetChanged();
                device_count++;
            }
        });

    }
    //This will be call by the scanner when scanner start scan to change UI.
    @Override
    public void scanningStarted() {
        setScanState(true);
    }

    //This will be call by the scanner when scanner stop scan to change UI.
    @Override
    public void scanningStopped() {
        if(toast != null){
            toast.cancel();
        }
        setScanState(false);

    }

    //This is called to change button text.
    private void setScanState(boolean value){
        ble_scanning = value;
        ((Button) this.findViewById(R.id.scanButton)).setText(value ? Constants.STOP_SCANNING:Constants.FIND);
    }

    //Defining the list displaying the available scanned devices to connect.
    private class ListAdapter extends BaseAdapter{
        private ArrayList<BluetoothDevice> ble_devices;
        public ListAdapter(){
            super();
            ble_devices = new ArrayList<BluetoothDevice>();
        }
        public void addDevice(BluetoothDevice device){
            if(!ble_devices.contains(device)){
                ble_devices.add(device);
            }
        }
        public boolean contains(BluetoothDevice device){
            return ble_devices.contains(device);
        }
        public BluetoothDevice getDevice(int position){
            return ble_devices.get(position);
        }
        public void clear(){
            ble_devices.clear();
        }
        @Override
        public int getCount(){
            return ble_devices.size();
        }
        @Override
        public Object getItem(int i){
            return ble_devices.get(i);
        }
        @Override
        public long getItemId(int i){
            return i;
        }


        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            if (view == null) {
                view = MainActivity.this.getLayoutInflater().inflate(R.layout.list_row, null);
                viewHolder = new ViewHolder();
                viewHolder.text = (TextView) view.findViewById(R.id.textView);
                viewHolder.bdaddr = (TextView) view.findViewById(R.id.bdaddr);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }
            BluetoothDevice device = ble_devices.get(i);
            String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0) {
                viewHolder.text.setText(deviceName);
            } else {
                viewHolder.text.setText("unknown device");
            }
            viewHolder.bdaddr.setText(device.getAddress());
            return view;
        }
    }

    //onScan will be called when the user press the button.
    public void onScan(View view){
        if (!ble_scanner.isScanning()){
            device_count = 0;
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
                    permission_granted = false;
                    requestLocationPermission();
                } else{
                    Log.i(Constants.TAG, "Location permission has already been granted. Starting scanning");
                    permission_granted = true;
                }
            }else{
                permission_granted = true;
            }
            startScanning();

        }else{
            ble_scanner.stopScanning();
        }
    }

    //startScanning will be call when onScan call it.
    private void startScanning(){
        if(permission_granted){
            runOnUiThread(new Runnable(){
                @Override
                public void run(){
                    ble_device_list_adapter.clear();
                    ble_device_list_adapter.notifyDataSetChanged();
                }
            });
            simpleToast(Constants.SCANNING,1200);
            //disabled button from scanning
            ble_scanner.startScanning(this, SCAN_TIMEOUT);
        }else{
            Log.i(Constants.TAG, "Permission to perform Bluetooth scanning was not yet granted");
        }
    }

    private void requestLocationPermission(){
        Log.i(Constants.TAG, "Location permission has NOT yet been granted. Requesting permission.");
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
            Log.i(Constants.TAG,"Displaying location permission rationale to provide additional context.");
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Permission Required");
            builder.setMessage("Please grant Location access so this application can perform Bluetooth scanning");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    Log.d(Constants.TAG,"Requesting permissions after explanation");
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);

                }
            });
            builder.show();


        } else{
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        if(requestCode == REQUEST_LOCATION){
            Log.i(Constants.TAG, "Received response for location permission request.");
            if(grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Log.i(Constants.TAG, "Location permission has now been granted. Scanning......");
                permission_granted = true;
                if(ble_scanner.isScanning()){
                    startScanning();
                }
            }else{
                Log.i(Constants.TAG, "Location permission was NOT granted.");
            }
        }else{
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
    private void simpleToast(String message, int duration){
        toast = Toast.makeText(this, message, duration);
        toast.setGravity(Gravity.CENTER, 0,0);
        toast.show();
    }
}