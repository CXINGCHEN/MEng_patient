package com.specknet.pdiotapp;

import android.app.Application;
import android.util.Log;

import com.cxc.arduinobluecontrol.bluetooth.BluetoothManager;

public class App extends Application {

    public static final String TAG = "App";


    public static App instance;


    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate() called");
        super.onCreate();
        instance = this;
        BluetoothManager.initialize(getApplicationContext());
    }

    public Application getApp() {
        return instance;
    }


}
