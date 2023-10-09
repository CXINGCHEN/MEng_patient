package com.cxc.arduinobluecontrol.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/* loaded from: classes.dex */
public class BluetoothHelper {
    private static final String TAG = "BluetoothHelper";

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void sendData(OutputStream outputStream, String str) {
        if (outputStream != null) {
            try {
                // 把数据写入到outputStream 就完成了发送
                outputStream.write(str.getBytes());
                return;
            } catch (IOException unused) {
                Log.d(TAG, "Could not write to bluetooth outputstream");
                return;
            }
        }
        Log.e(TAG, "No output stream is available.");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean resetConnection(InputStream inputStream, OutputStream outputStream, BluetoothSocket bluetoothSocket) {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (Exception unused) {
                Log.e(TAG, "Could not reset connection");
                return false;
            }
        }
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return true;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void closeSocket(BluetoothSocket bluetoothSocket) {
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
                return;
            } catch (IOException unused) {
                Log.e(TAG, "Could not close bluetooth socket");
                return;
            }
        }
        Log.e(TAG, "Bluetooth socket not retrieved");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @SuppressLint("MissingPermission")
    public static BluetoothSocket createBluetoothSocket(BluetoothDevice bluetoothDevice, UUID uuid) throws IOException {
        if (Build.VERSION.SDK_INT >= 10) {
            try {
                bluetoothDevice.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
                return (BluetoothSocket) bluetoothDevice.getClass().getMethod("createRfcommSocket", Integer.TYPE).invoke(bluetoothDevice, 1);
            } catch (Exception e) {
                Log.d(TAG, "Exception while creating socket", e);
            }
        }
        return bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
    }
}
