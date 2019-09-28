package com.enel400.xiang.hydroping;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by xiang on 2019-02-21.
 */

public class DeviceListAdapter extends ArrayAdapter<BluetoothDevice>{
    private LayoutInflater myLayoutInflater;
    private ArrayList<BluetoothDevice> myDevices;
    private int myViewResourceID;
    public DeviceListAdapter(Context context, int tvResourceID, ArrayList<BluetoothDevice> devices)
    {
        super(context, tvResourceID, devices);
        this.myDevices = devices;
        myLayoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        myViewResourceID = tvResourceID;
    }
    public View getView (int position, View convertView, ViewGroup parent)
    {
        convertView = myLayoutInflater.inflate(myViewResourceID,null);
        BluetoothDevice device = myDevices.get(position);
        if(device != null)
        {
            TextView deviceName = (TextView)convertView.findViewById(R.id.tvDeviceName);
            TextView deviceAddress = (TextView)convertView.findViewById(R.id.tvDeviceAddress);
            if(deviceName!=null)
            {
                deviceName.setText(device.getName());
            }
            if(deviceAddress!=null)
            {
                deviceAddress.setText(device.getAddress());
            }
        }
        return convertView;
    }
}
