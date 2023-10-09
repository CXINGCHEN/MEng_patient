package com.cxc.arduinobluecontrol.dialog;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatDialogFragment;


import com.specknet.pdiotapp.R;

import java.util.Set;

/**
 * 他是一个弹窗
 * extends AppCompatDialogFragment
 */
@SuppressLint("MissingPermission")
public class DeviceListDialog extends AppCompatDialogFragment {
    public static final String TAG = "DeviceListDialog";
    private BluetoothAdapter mBtAdapter;
    private DeviceSelectedListener mDeviceSelectedListener;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;
    private SparseArray<BluetoothDevice> mPairedDevices = new SparseArray<>();
    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() { // from class: com.broxcode.arduinobluetoothfree.dialog.DeviceListDialog.1
        @Override // android.widget.AdapterView.OnItemClickListener
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long j) {
            Log.d(DeviceListDialog.TAG, "Device selected for position " + position);
            if (DeviceListDialog.this.mDeviceSelectedListener != null) {
                if (DeviceListDialog.this.mPairedDevices != null && DeviceListDialog.this.mPairedDevices.size() > 0) {
                    BluetoothDevice bluetoothDevice = (BluetoothDevice) DeviceListDialog.this.mPairedDevices.get(position);
                    if (bluetoothDevice != null) {
                        Log.d(DeviceListDialog.TAG, "Selected device " + bluetoothDevice.getAddress() + "| Address " + bluetoothDevice.getName());
                        DeviceListDialog.this.mDeviceSelectedListener.onDeviceSelected(bluetoothDevice.getAddress(), bluetoothDevice.getName());
                        DeviceListDialog.this.dismiss();
                        return;
                    }
                    Log.e(DeviceListDialog.TAG, "Selected device not found in cache");
                    return;
                }
                Log.e(DeviceListDialog.TAG, "No paired devices in cache");
                return;
            }
            Log.e(DeviceListDialog.TAG, "No device selected listener, impossible use case");
        }
    };

    /* loaded from: classes.dex */
    public interface DeviceSelectedListener {
        void onDeviceSelected(String str, String str2);

        void onDismiss();
    }

    public DeviceListDialog() {
    }

    public DeviceListDialog(DeviceSelectedListener deviceSelectedListener) {
        this.mDeviceSelectedListener = deviceSelectedListener;
    }

    @Override // androidx.fragment.app.DialogFragment, androidx.fragment.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        // 创建一个弹窗 第一步都会执行这个onCreate
    }

    @Override // androidx.fragment.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        int i = 0;
        View inflate = layoutInflater.inflate(R.layout.dialog_device_list, viewGroup, false);
        getDialog().setTitle("Select your device");

        ArrayAdapter arrayAdapter = new ArrayAdapter(getContext(), R.layout.device_name);
        this.mNewDevicesArrayAdapter = new ArrayAdapter<>(getContext(), R.layout.device_name);

        // listview <=>  adapter (中间纽带)  <=> data
        ListView listView = (ListView) inflate.findViewById(R.id.paired_devices);
        listView.setAdapter((ListAdapter) arrayAdapter); // 设置适配器
        listView.setOnItemClickListener(this.mDeviceClickListener);
        BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mBtAdapter = defaultAdapter;

        // 集合
        Set<BluetoothDevice> bondedDevices = defaultAdapter.getBondedDevices();
        if (bondedDevices.size() > 0) {
            for (BluetoothDevice bluetoothDevice : bondedDevices) {
                arrayAdapter.add(bluetoothDevice.getName() + "\n" + bluetoothDevice.getAddress());
                this.mPairedDevices.put(i, bluetoothDevice);
                i++;
            }
        } else {
            arrayAdapter.add(getResources().getText(R.string.none_paired).toString());
        }
        return inflate;
    }

    @Override // androidx.fragment.app.DialogFragment, android.content.DialogInterface.OnDismissListener
    public void onDismiss(DialogInterface dialogInterface) {
        super.onDismiss(dialogInterface);
        DeviceSelectedListener deviceSelectedListener = this.mDeviceSelectedListener;
        if (deviceSelectedListener != null) {
            deviceSelectedListener.onDismiss();
        }
    }

    @Override // androidx.fragment.app.DialogFragment, androidx.fragment.app.Fragment
    public void onDestroyView() {
        super.onDestroyView();
    }
}
