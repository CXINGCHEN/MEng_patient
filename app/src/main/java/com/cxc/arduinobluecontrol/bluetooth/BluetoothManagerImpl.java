package com.cxc.arduinobluecontrol.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/* loaded from: classes.dex */
public class BluetoothManagerImpl extends BluetoothManager implements Handler.Callback, IncomingDataListener {
    private static final UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothSocket btSocket;
    private InputStream inStream;

    private OutputStream outStream;

    private boolean isConnectedToDevice;
    private Context mContext;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private Thread mIncomingDataWorkerThread;

    private Ringtone ringtone;
    private final String TAG = "BluetoothManagerImpl";
    private final boolean DEBUG = true;
    private BluetoothDevice mBluetoothDevice = null;
    private Uri notificationUri = null;
    private Set<BluetoothDisconnectedListener> disconnectedListeners = Collections.synchronizedSet(new HashSet());
    private Set<IncomingDataListener> incomingDataListeners = Collections.synchronizedSet(new HashSet());
    public final boolean showToast = false;
    private final int MSG_SEND_DATA = 0;
    private final int MSG_BLUETOOTH_DISCONNECTED = 1;
    private BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

    /* JADX INFO: Access modifiers changed from: package-private */
    public BluetoothManagerImpl(Context context) {
        this.mContext = context;
        HandlerThread handlerThread = new HandlerThread("BluetoothManagerImpl");
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        this.mHandler = new Handler(this.mHandlerThread.getLooper(), this);
    }

    @SuppressLint("MissingPermission")
    @Override // com.broxcode.arduinobluetoothfree.bluetooth.BluetoothManager
    public boolean connectTo(String macAddress) {
        Log.d("BluetoothManagerImpl", "Connecting to " + macAddress);
        Uri defaultUri = RingtoneManager.getDefaultUri(4);
        this.notificationUri = defaultUri;
        this.ringtone = RingtoneManager.getRingtone(this.mContext, defaultUri);
        BluetoothDevice remoteDevice = this.btAdapter.getRemoteDevice(macAddress);
        this.mBluetoothDevice = remoteDevice;
        try {
            // 客户端 创建连接  设备已经建立了连接  可以进行数据传输
            /// socket就是传输通道
            this.btSocket = BluetoothHelper.createBluetoothSocket(remoteDevice, mUUID);
            Log.d("BluetoothManagerImpl", "bluetooth socket created");
            BluetoothSocket bluetoothSocket = this.btSocket;
            if (bluetoothSocket != null) {
                bluetoothSocket.connect();
                this.outStream = this.btSocket.getOutputStream();
                this.inStream = this.btSocket.getInputStream();
                this.btAdapter.cancelDiscovery();
                this.isConnectedToDevice = true;
                return true;
            }
            Log.e("BluetoothManagerImpl", "Could not get bluetooth socket");
            return false;
        } catch (IOException e) {
            Log.e("BluetoothManagerImpl", "Unable to connect to device " + e);
            BluetoothHelper.closeSocket(this.btSocket);
            this.outStream = null;
            this.inStream = null;
            return false;
        }
    }

    @Override // android.os.Handler.Callback
    public boolean handleMessage(Message message) {
        int what = message.what;
        if (what == 0) { // 手机发送数据给蓝牙模块
            String str = (String) message.obj; // 1
            Log.d("BluetoothManagerImpl", "[MSG_SEND_DATA] Message data=" + str);
            if (this.isConnectedToDevice) {

//                try {
//                    this.outStream.write(str.getBytes());
//                    // 发送完毕
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }

                BluetoothHelper.sendData(this.outStream, str);
            } else {
                Log.e("BluetoothManagerImpl", "Trying to send data while no device is connected");
            }
            return true;
        } else if (what != 1) {
            return false;
        } else {
            Log.d("BluetoothManagerImpl", "[MSG_BLUETOOTH_DISCONNECTED]");
            notifyBluetoothDisconnected();
            this.isConnectedToDevice = false;
            this.mBluetoothDevice = null;
            BluetoothHelper.closeSocket(this.btSocket);
            resetIncomingDataWorker();
            return true;
        }
    }

    @Override // com.broxcode.arduinobluetoothfree.bluetooth.IncomingDataListener
    public void onDataReceived(String str) {
        Log.d("BluetoothManagerImpl", "onDataReceived " + str);
        notifyIncomingData(str);
    }

    private void notifyIncomingData(String str) {
        for (IncomingDataListener incomingDataListener : this.incomingDataListeners) {
            incomingDataListener.onDataReceived(str);
        }
    }

    @Override // com.broxcode.arduinobluetoothfree.bluetooth.BluetoothDisconnectedListener
    public void onBluetoothDisconnected() {
        Log.d("BluetoothManagerImpl", "onBluetoothDisconnected");
        this.mHandler.sendEmptyMessage(1);
    }

    private void notifyBluetoothDisconnected() {
        Log.d("BluetoothManagerImpl", "notifyBluetoothDisconnected");
        for (BluetoothDisconnectedListener bluetoothDisconnectedListener : this.disconnectedListeners) {
            bluetoothDisconnectedListener.onBluetoothDisconnected();
        }
    }

    @Override // com.broxcode.arduinobluetoothfree.bluetooth.BluetoothManager
    public void sendData(String data) {
        // 假设 data 是 1
        Log.d("BluetoothManagerImpl", "send Data");
        this.mHandler.obtainMessage(0, data).sendToTarget();

//        this.mHandler.sendMessage(mHandler.obtainMessage(0, data));
    }

    @Override // com.broxcode.arduinobluetoothfree.bluetooth.BluetoothManager
    public boolean resetConnection() {
        if (this.isConnectedToDevice) {
            notifyBluetoothDisconnected();
        }
        this.isConnectedToDevice = false;
        this.mBluetoothDevice = null;
        resetIncomingDataWorker();
        return BluetoothHelper.resetConnection(this.inStream, this.outStream, this.btSocket);
    }

    @Override // com.broxcode.arduinobluetoothfree.bluetooth.BluetoothManager
    public boolean isBluetoothEnabled() {
        BluetoothAdapter bluetoothAdapter = this.btAdapter;
        if (bluetoothAdapter == null) {
            return false;
        }
        return bluetoothAdapter.isEnabled();
    }

    @Override // com.broxcode.arduinobluetoothfree.bluetooth.BluetoothManager
    public boolean isConnected() {
        return this.isConnectedToDevice;
    }

    @SuppressLint("MissingPermission")
    @Override // com.broxcode.arduinobluetoothfree.bluetooth.BluetoothManager
    public String getConnectedDeviceName() {
        BluetoothDevice bluetoothDevice = this.mBluetoothDevice;
        if (bluetoothDevice != null) {
            return bluetoothDevice.getName();
        }
        return null;
    }

    @Override // com.broxcode.arduinobluetoothfree.bluetooth.BluetoothManager
    public void addDisconnectionListener(BluetoothDisconnectedListener bluetoothDisconnectedListener) {
        Log.d("BluetoothManagerImpl", "addDisconnectionListener");
        if (bluetoothDisconnectedListener == null) {
            return;
        }
        this.disconnectedListeners.add(bluetoothDisconnectedListener);
    }

    @Override // com.broxcode.arduinobluetoothfree.bluetooth.BluetoothManager
    public void removeDisconnectionListener(BluetoothDisconnectedListener bluetoothDisconnectedListener) {
        Log.d("BluetoothManagerImpl", "removeDisconnectionListener");
        if (bluetoothDisconnectedListener == null) {
            return;
        }
        this.disconnectedListeners.remove(bluetoothDisconnectedListener);
    }

    @Override // com.broxcode.arduinobluetoothfree.bluetooth.BluetoothManager
    public void addIncomingDataListener(IncomingDataListener incomingDataListener) {
        Log.d("BluetoothManagerImpl", "addIncomingDataListener");
        if (incomingDataListener == null) {
            return;
        }
        if (this.inStream == null) {
            Log.d("BluetoothManagerImpl", "InputStream is null, ignoring adding listener");
        }
        Thread thread = this.mIncomingDataWorkerThread;
        if (thread == null) {
            Log.d("BluetoothManagerImpl", "Instantiating Incoming data thread and starting it.");
            Thread thread2 = new Thread(new IncomingDataRunnable(this.inStream, this));
            this.mIncomingDataWorkerThread = thread2;
            thread2.start();
        } else if (thread.isInterrupted()) {
            Log.d("BluetoothManagerImpl", "Incoming data thread is in interrupted state, now restarting.");
            this.mIncomingDataWorkerThread.start();
        }
        this.incomingDataListeners.add(incomingDataListener);
    }

    @Override // com.broxcode.arduinobluetoothfree.bluetooth.BluetoothManager
    public void removeIncomingDataListener(IncomingDataListener incomingDataListener) {
        Log.d("BluetoothManagerImpl", "removeIncomingDataListener");
        if (incomingDataListener == null) {
            return;
        }
        this.incomingDataListeners.remove(incomingDataListener);
        if (this.incomingDataListeners.size() == 0) {
            resetIncomingDataWorker();
        }
    }

    private void resetIncomingDataWorker() {
        Log.d("BluetoothManagerImpl", "resetIncomingDataWorker");
        Thread thread = this.mIncomingDataWorkerThread;
        if (thread != null) {
            thread.interrupt();
            this.mIncomingDataWorkerThread = null;
        }
    }
}
