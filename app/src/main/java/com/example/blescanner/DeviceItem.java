package com.example.blescanner;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class DeviceItem extends BaseAdapter {

    private LayoutInflater mLayInf;
    ArrayList<BluetoothDevice> mArrayList;

    public DeviceItem(Context context,ArrayList<BluetoothDevice> arrayList){
        mLayInf = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mArrayList = arrayList;
    }

    @Override
    public int getCount() {
        return mArrayList.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = mLayInf.inflate(R.layout.list_device_item, parent, false);


        TextView deviceName = (TextView) v.findViewById(R.id.deviceName);
        TextView deviceAddr = (TextView) v.findViewById(R.id.deviceAddr);

        deviceName.setText(mArrayList.get(position).getName());
        deviceAddr.setText(mArrayList.get(position).getName());

        return v;
    }
}
