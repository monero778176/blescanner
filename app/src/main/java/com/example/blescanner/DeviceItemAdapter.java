package com.example.blescanner;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Map;

public class DeviceItemAdapter extends BaseAdapter {

    private ArrayList<BluetoothDevice> mArrayList;
    private Context context;

    public DeviceItemAdapter(Context context, ArrayList<BluetoothDevice> arrayList) {
        this.context = context;
        this.mArrayList = arrayList;
    }

    public void addDeviceItem(BluetoothDevice device) {
        if (!mArrayList.contains(device)) {
            mArrayList.add(device);
            notifyDataSetChanged();
        }
    }

    public void clear(){
        mArrayList.clear();
    }

    @Override
    public int getCount() {
        return mArrayList.size();
    }

    @Override
    public Object getItem(int position) {
        return mArrayList.get(position);
    }

    public Object getDeviceName(int position){
        return mArrayList.get(position).getName();
    }
    public Object getDeviceAddr(int position){
        return mArrayList.get(position).getAddress();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).
                    inflate(R.layout.list_device_item, parent, false);
        }


        // get the TextView for item name and item description
        TextView textViewItemName = (TextView)
                convertView.findViewById(R.id.deviceName);
        TextView textViewItemDescription = (TextView)
                convertView.findViewById(R.id.deviceAddr);

        //sets the text for item name and item description from the current item object
        if (mArrayList.get(position).getName()==null){
            textViewItemName.setText("Non Name");
        }else {
            textViewItemName.setText(mArrayList.get(position).getName());
        }

        textViewItemDescription.setText(mArrayList.get(position).getAddress());

        // returns the view for the current row
        return convertView;
    }
}