package com.cxc.arduinobluecontrol.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

@SuppressLint("MissingPermission")
public class BluetoothReceiver extends BroadcastReceiver {
    private final String TAG = "BluetoothReceiver";
    private final boolean DEBUG = true;

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        action.hashCode();
        if (action.equals("android.bluetooth.device.action.ACL_CONNECTED")) {
            Log.d("BluetoothReceiver", "ACTION_ACL_CONNECTED");
        } else if (action.equals("android.bluetooth.device.action.ACL_DISCONNECTED")) {
            String name = ((BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE")).getName();
            Log.d("BluetoothReceiver", "[ACTION_ACL_DISCONNECTED] for device name " + name);
            try {
                if (BluetoothManager.getInstance().getConnectedDeviceName() == null || !BluetoothManager.getInstance().getConnectedDeviceName().equals(name)) {
                    return;
                }
                BluetoothManager.getInstance().onBluetoothDisconnected();
            } catch (IllegalStateException e) {
                Log.e("BluetoothReceiver", "Exception ", e);
            }
        }
    }
}
