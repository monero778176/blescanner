package com.example.blescanner;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.ArraySet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    int DISCOVERABLE_DURATION = 120;
    private static final int REQUEST_ENABLE_BT = 1; // Unique request code
    private static final int REQUEST_DISCOVERABLE_BT = 2; // Unique request code
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner scanner;
    private ArrayList<BluetoothDevice> mBluetoothDevices = new ArrayList<>();
    private boolean mScanning = false;
    private static final int SCAN_TIME = 10000;
    private Handler mHandler;
    private ArrayList<String> deviceName;
    private ArrayList<BluetoothDevice> deviceList;
    private ArrayAdapter<String> listAdapter;
    private ArrayAdapter<BluetoothDevice> devicelistAdapter;

    Button scanbtn, stopbtn;
    ListView listView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scanbtn = findViewById(R.id.button);
//        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            Toast.makeText(this, "R.string.bluetooth_not_supported", Toast.LENGTH_SHORT).show();
            finish();
        }
// Use this check to determine whether BLE is supported on the device. Then
// you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "R.string.ble_not_supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        //要求位置
        ActivityResultLauncher<String[]> locationPermissionRequest =
                registerForActivityResult(new ActivityResultContracts
                                .RequestMultiplePermissions(), result -> {
                            Boolean coarseLocationGranted = null;
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                Boolean fineLocationGranted = result.getOrDefault(
                                        Manifest.permission.ACCESS_FINE_LOCATION, false);
                                coarseLocationGranted = result.getOrDefault(
                                        Manifest.permission.ACCESS_COARSE_LOCATION, false);
                                if (fineLocationGranted != null && fineLocationGranted) {
                                    // Precise location access granted.
                                } else if (coarseLocationGranted != null && coarseLocationGranted) {
                                    // Only approximate location access granted.
                                } else {
                                    // No location access granted.
                                }
                            }
                        }

                );

        locationPermissionRequest.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });

        bluetoothManager = (BluetoothManager) this.getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        scanner = mBluetoothAdapter.getBluetoothLeScanner();

        if (mBluetoothAdapter == null) {
            Toast.makeText(getBaseContext(), "Not supprted bluetooth", Toast.LENGTH_SHORT).show();
            finish();
        }

        listView = findViewById(R.id.listView);
        deviceName = new ArrayList<>();
        deviceList = new ArrayList<>();
        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceName);
        devicelistAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceList);
        listView.setAdapter(devicelistAdapter);

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }


        scanbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startingScanning();
            }
        });


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(getBaseContext(), "You click the number of item " + deviceList.get(position), Toast.LENGTH_SHORT).show();
            }
        });


    }

    public void startingScanning() {
        System.out.println("start scanning");
        scanbtn.setVisibility(View.INVISIBLE);
        stopbtn.setVisibility(View.VISIBLE);

//        AsyncTask.execute(new Runnable() {
//            @Override
//            public void run() {
//                deviceName.clear();
//                scanner.startScan(leScanCallback);
//                Log.d(TAG, "run: Starting Scanning");
//            }
//        });
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
//                    scanner.stopScan(leScanCallback);
//                Toast.makeText(MainActivity .this,"Scan five second",Toast.LENGTH_SHORT).show();
                deviceName.clear();
                scanbtn.setText("SCAN");
                scanner.stopScan(leScanCallback);
                Log.d(TAG, "run: Starting Scanning");
                Toast.makeText(getBaseContext(), "Stop scan", Toast.LENGTH_SHORT).show();

            }
        }, 5000);
        scanbtn.setText("searching");
        scanner.startScan(leScanCallback);
        Toast.makeText(getBaseContext(), "Starting scan", Toast.LENGTH_SHORT).show();
    }

    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (!deviceList.contains(device)) {
                deviceList.add(device);
            }
            deviceName.add(device.getName() + "\n" + device.getAddress());
            devicelistAdapter.notifyDataSetChanged();
            Log.d(TAG, device.getName() + "\n" + device.getAddress());
        }
    };

    public void stopScanning() {
        System.out.println("stopping scanning");
        scanbtn.setVisibility(View.VISIBLE);
        stopbtn.setVisibility(View.INVISIBLE);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                scanner.stopScan(leScanCallback);
                Log.d(TAG, "run: stop scanning");
            }
        });
    }


}