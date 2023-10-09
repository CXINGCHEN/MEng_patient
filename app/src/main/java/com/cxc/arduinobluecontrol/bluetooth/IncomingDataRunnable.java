package com.cxc.arduinobluecontrol.bluetooth;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/* loaded from: classes.dex */
public class IncomingDataRunnable implements Runnable {
    private final InputStream mInputStream;
    private final IncomingDataListener mListener;
    private final String TAG = "IncomingDataRunnable";
    private final byte delimiter = 10;
    private int readBufferPosition = 0;
    private final byte[] readBuffer = new byte[1024];

    /* JADX INFO: Access modifiers changed from: package-private */
    public IncomingDataRunnable(InputStream inputStream, IncomingDataListener incomingDataListener) {
        this.mInputStream = inputStream;
        this.mListener = incomingDataListener;
    }

    @Override // java.lang.Runnable
    public void run() {
        InputStream inputStream;
        // 死循环  在读数据
        while (!Thread.currentThread().isInterrupted()) {
            inputStream = this.mInputStream;
            if (inputStream == null) {
                Log.e("IncomingDataRunnable", "Interrupting IncomingDataRunnable,inputstream=null");
                Thread.currentThread().interrupt();
                return;
            }
            int available = 0;
            try {
                available = inputStream.available(); // 接收到的数据长度
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (available > 0) {
                byte[] bArr = new byte[available];
                try {
                    // 读出来字节数组
                    this.mInputStream.read(bArr);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                for (int i = 0; i < available; i++) {
                    byte b = bArr[i];
                    int i2 = this.readBufferPosition;
                    if (i2 == 1023) {
                        b = 10;
                    }
                    if (b == 10) {
                        byte[] bArr2 = new byte[i2];
                        System.arraycopy(this.readBuffer, 0, bArr2, 0, i2);
                        String str = null;
                        try {
                            str = new String(bArr2, "US-ASCII");
                        } catch (UnsupportedEncodingException e) {
                            throw new RuntimeException(e);
                        }
                        this.readBufferPosition = 0;
                        this.mListener.onDataReceived(str);
                    } else {
                        byte[] bArr3 = this.readBuffer;
                        this.readBufferPosition = i2 + 1;
                        bArr3[i2] = b;
                    }
                }
            }
        }
    }
}
