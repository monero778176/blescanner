package com.example.blescanner;

import static java.security.AccessController.getContext;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
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
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcelable;
import android.telephony.TelephonyManager;
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

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

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

    private ArrayList<Integer> rssiList;
    private ArrayList<String> uuidList;

    private BluetoothGatt bluetoothGatt;

    private CSVWriter csvWriter;
    private DeviceItemAdapter deviceItemAdapter;

    Button scanbtn, broadbtn, testbtn;
    ListView listView;
    UUID uniqueID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scanbtn = findViewById(R.id.button);
        broadbtn = findViewById(R.id.broadbtn);
        testbtn = findViewById(R.id.testbtn);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);
        }

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
        ActivityResultLauncher<String[]> locationPermissionRequest = registerForActivityResult(new ActivityResultContracts
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

        //要求寫檔


        bluetoothManager = (BluetoothManager) this.getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        scanner = mBluetoothAdapter.getBluetoothLeScanner();
        uniqueID = UUID.randomUUID();
        Log.d(TAG, "onCreate: uuid: " + uniqueID);
        Log.d(TAG, "onCreate: ");
        //若不支援藍芽，則跳出程式
        if (mBluetoothAdapter == null) {
            Toast.makeText(getBaseContext(), "Not supprted bluetooth", Toast.LENGTH_SHORT).show();
            finish();
        }

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




        listView = findViewById(R.id.listView);
        rssiList = new ArrayList<>();
        uuidList = new ArrayList<>();
        deviceList = new ArrayList<>();
        deviceItemAdapter = new DeviceItemAdapter(this, deviceList);
        listView.setAdapter(deviceItemAdapter);

        //藍芽搜尋結果列表，做連結等動作
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

//                Toast.makeText(getApplicationContext(), "Your click item "+deviceItemAdapter.getDeviceAddr(position), Toast.LENGTH_SHORT).show();
                //先遠端
                BluetoothDevice device = (BluetoothDevice) deviceItemAdapter.getItem(position);
                mBluetoothAdapter.cancelDiscovery();
                Intent deviceIntent = new Intent(MainActivity.this, DetailActivity.class);
                deviceIntent.putExtra("device", device);
                startActivity(deviceIntent);


            }
        });

        //廣播搜尋附近藍芽裝置
        broadbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
//                startActivityForResult(intent,2);
                deviceItemAdapter.clear();
                rssiList.clear();
                uuidList.clear();
                IntentFilter filter2 = new IntentFilter();
                filter2.addAction(BluetoothDevice.ACTION_FOUND);
                filter2.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
                filter2.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                registerReceiver(receiver, filter2);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
//                    scanner.stopScan(leScanCallback);
//                Toast.makeText(MainActivity .this,"Scan five second",Toast.LENGTH_SHORT).show();
                        mBluetoothAdapter.cancelDiscovery();
                        Log.d(TAG, "run: stop Scanning");
                        Toast.makeText(getBaseContext(), "Stop broadcasting", Toast.LENGTH_SHORT).show();
                        broadbtn.setClickable(true);

                    }
                }, 5000);
                mBluetoothAdapter.startDiscovery();
                broadbtn.setClickable(false);
                Toast.makeText(getBaseContext(), "Starting broadcast for 5 seconds", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);

                String uuid = intent.getStringExtra(BluetoothDevice.EXTRA_UUID);
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                deviceItemAdapter.addDeviceItem(device);
                deviceItemAdapter.notifyDataSetChanged();
                Log.d(TAG, "onReceive: device name: " + device.getName() + ", address: " + device.getAddress());
            }
        }

    };

    public void startingScanning() {
        System.out.println("start scanning");

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
        Toast.makeText(getBaseContext(), "Starting scan for 5 seconds", Toast.LENGTH_SHORT).show();
    }

    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
//            if (!deviceList.contains(device)) {
//                deviceList.add(device);
//            }
            deviceItemAdapter.addDeviceItem(device);
//            deviceItemAdapter.notifyDataSetChanged();

        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        Log.d(TAG, "onDestroy: unregisterReceiver receiver");
    }
}