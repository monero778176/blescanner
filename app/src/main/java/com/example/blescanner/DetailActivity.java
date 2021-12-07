package com.example.blescanner;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.opencsv.CSVWriter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.text.SimpleDateFormat;


public class DetailActivity extends AppCompatActivity {

    private static final String TAG = "DetailActivity";
    TextView connectTv, textView, serviceTv, printTextView, timeView;
    ListView listView;
    Button disbtn;

    private CSVWriter csvWriter;
//    private CSVWriter csvWriter1;

    private BluetoothGatt bluetoothGatt;

//    FileWriter fw;
//    BufferedWriter bw;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        textView = findViewById(R.id.textView);
        connectTv = findViewById(R.id.textView2);
        serviceTv = findViewById(R.id.serviceTv);
        printTextView = findViewById(R.id.printTextView);
        timeView = findViewById(R.id.timeView);

        listView = findViewById(R.id.listView);
        disbtn = findViewById(R.id.disGatt);


        BluetoothDevice device = getIntent().getExtras().getParcelable("device");

        if (device != null) {
            textView.setText("Name: " + device.getName() + "\n address:" + device.getAddress());
        }

        //檔案製作
        File exportDir = new File(Environment.getExternalStorageDirectory(), "");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }
        Date date = new Date(System.currentTimeMillis());
        String filename = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(date);
        File file = new File(exportDir, filename + "acc.csv");  
//        File file1 = new File(exportDir, filename + "gyro.csv");
        try {
            file.createNewFile();
            csvWriter = new CSVWriter(new FileWriter(file));
//            csvWriter1 = new CSVWriter(new FileWriter(file1));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // bluetoothGATT instance
        bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback);
        if (bluetoothGatt != null) {
            bluetoothGatt.connect();

        }

        disbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    csvWriter.close();
//                    csvWriter1.close();
                    Log.d(TAG, "onClick: close write, and disconnect.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                bluetoothGatt.disconnect();
                finish();
            }
        });

    }

    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        connectTv.setText("連結成功");
                    }
                });
                gatt.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        connectTv.setText("連結斷開");
                    }
                });
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.d(TAG, "onServicesDiscovered: 發現服務");
            String uuid = null;

            if (BluetoothGatt.GATT_SUCCESS == status) {

                Log.d(TAG, "GATT服務啟動，獲取服務:");

                for (BluetoothGattService gattService : gatt.getServices()) {
                    uuid = gattService.getUuid().toString();
                    Log.d(TAG, "onServicesDiscovered: 服務是" + gattService + " uuid: " + uuid);

                    String char_uuid = null;
                    for (BluetoothGattCharacteristic characteristic : gattService.getCharacteristics()) {
                        char_uuid = characteristic.getUuid().toString();
                        Log.d(TAG, "onServicesDiscovered: this 特性:" + characteristic + " String uuid:" + char_uuid);


                        if (characteristic.getUuid().toString().equals("6e400007-b5a3-f393-e0a9-e50e24dcca9e")) {

                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    gatt.setCharacteristicNotification(characteristic, true);

                                    for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                        gatt.writeDescriptor(descriptor);
                                    }
                                }
                            }).start();
                        } else if (characteristic.getUuid().toString().equals("6e400009-b5a3-f393-e0a9-e50e24dcca9e")) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    gatt.setCharacteristicNotification(characteristic, true);

                                    for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                        gatt.writeDescriptor(descriptor);
                                    }
                                }
                            }).start();
                        }
                    }
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);



            if (characteristic.getUuid().toString().equals("6e400009-b5a3-f393-e0a9-e50e24dcca9e")) {
                try {
                    Date date1 = new Date(System.currentTimeMillis());
                    String nowTime1 = new SimpleDateFormat("hh:mm:ss").format(date1);
                    Log.d(TAG, "onCharacteristicChanged: 特性9" + nowTime1);
                    Thread.sleep(5);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            byte[] data = characteristic.getValue();
                            String dd = new String(data);

                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    String nowTime = new SimpleDateFormat("hh:mm:ss").format(date1);
                                    nowTime = nowTime + "," + dd;
                                    String[] dx = nowTime.split(",");
                                    csvWriter.writeNext(dx);
                                }
                            }).start();

                            serviceTv.setText(dd);
                            Log.d(TAG, "run: 加速度值:" + dd);
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
//            if (characteristic.getUuid().toString().equals("6e400007-b5a3-f393-e0a9-e50e24dcca9e")) {
//                try {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            byte[] data = characteristic.getValue();
//                            String dd = new String(data);
//                            new Thread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    Date date1 = new Date(System.currentTimeMillis());
//                                    String nowTime = new SimpleDateFormat("hh:mm:ss").format(date1);
//                                    nowTime = nowTime + "," + dd;
//                                    String[] dx = nowTime.split(",");
//                                    csvWriter1.writeNext(dx);
//                                }
//                            }).start();
//                            printTextView.setText(dd);
//                            Log.d(TAG, "run: 陀螺儀值:" + dd);
//                        }
//                    });
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }

        }

    };

    @Override
    protected void onDestroy() {

        super.onDestroy();
    }
}