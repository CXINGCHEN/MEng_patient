package com.cxc.arduinobluecontrol.bluetooth;

import android.content.Context;
import android.util.Log;

/* loaded from: classes.dex */
public abstract class BluetoothManager implements BluetoothDisconnectedListener {
    private static BluetoothManager INSTANCE = null;
    private static final String TAG = "BluetoothManager";

    public abstract void addDisconnectionListener(BluetoothDisconnectedListener bluetoothDisconnectedListener);

    public abstract void addIncomingDataListener(IncomingDataListener incomingDataListener);

    public abstract boolean connectTo(String str);

    public abstract String getConnectedDeviceName();

    public abstract boolean isBluetoothEnabled();

    public abstract boolean isConnected();

    public abstract void removeDisconnectionListener(BluetoothDisconnectedListener bluetoothDisconnectedListener);

    public abstract void removeIncomingDataListener(IncomingDataListener incomingDataListener);

    public abstract boolean resetConnection();

    public abstract void sendData(String str);

    public static synchronized void initialize(Context context) {
        synchronized (BluetoothManager.class) {
            Log.d(TAG, "Initializing BluetoothManager");
            if (INSTANCE != null) {
                Log.e(TAG, "BluetoothManager already instantiated");
            } else {
                INSTANCE = new BluetoothManagerImpl(context);
            }
        }
    }

    public static BluetoothManager getInstance() {
        BluetoothManager bluetoothManager = INSTANCE;
        if (bluetoothManager != null) {
            return bluetoothManager;
        }
        throw new IllegalStateException("BluetoothManager has not been initialized");
    }
}
