package com.specknet.pdiotapp.bluetooth;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
import com.specknet.pdiotapp.HomeActivity;
import com.specknet.pdiotapp.R;
import com.specknet.pdiotapp.utils.Constants;
import com.specknet.pdiotapp.utils.SpeckIntentFilters;
import com.specknet.pdiotapp.utils.Thingy1PacketHandler;
import com.specknet.pdiotapp.utils.Thingy2PacketHandler;
import com.specknet.pdiotapp.utils.Utils;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.function.Consumer;

import rx.Observable;
import rx.Subscription;


public class BluetoothSpeckService extends Service {
    private static final String TAG = "BLT";

    public static RxBleClient rxBleClient;
    private BluetoothAdapter mBluetoothAdapter;

    private Subscription scanSubscription;
    private boolean mIsServiceRunning = false;

    // all thingy1 variables
    private RxBleConnection.RxBleConnectionState mLastThingy1ConnectionState;
    private static String THINGY1_UUID;
    private static String THINGY1_BLE_ADDRESS;

    private Subscription thingy1LiveSubscription;

    private boolean mIsThingy1Enabled;

    private Thingy1PacketHandler thingy1Handler;

    private RxBleDevice mThingy1Device;

    private boolean mIsThingy1Found;

    private String mThingy1Name;

    private boolean mIsThingy1Paused;

    private Observable<RxBleConnection> thingy1Connection;

    // everything for the Thingy2
    private RxBleConnection.RxBleConnectionState mLastThingy2ConnectionState;

    private static String THINGY2_UUID;
    private static String THINGY2_BLE_ADDRESS;

    private Subscription thingy2LiveSubscription;

    private boolean mIsThingy2Enabled;

    private Thingy2PacketHandler thingy2Handler;

    private RxBleDevice mThingy2Device;

    private boolean mIsThingy2Found;

    private String mThingy2Name;

    private boolean mIsThingy2Paused;

    private Observable<RxBleConnection> thingy2Connection;

    public BluetoothSpeckService() {
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate: here");
        super.onCreate();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
            startMyOwnForeground();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startMyOwnForeground() {
        Log.d(TAG, "startMyOwnForeground: here");
        final int SERVICE_NOTIFICATION_ID = 8598001;
        String NOTIFICATION_CHANNEL_ID = "com.specknet.pdiotapp";
        String channelName = "Thingy Bluetooth Service";
        NotificationChannel chan = null;
        chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true).setSmallIcon(R.drawable.vec_wireless_active).setContentTitle("Thingy Bluetooth Service").setPriority(NotificationManager.IMPORTANCE_MIN).setCategory(Notification.CATEGORY_SERVICE).build();
        startForeground(SERVICE_NOTIFICATION_ID, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        Log.d(TAG, "onStartCommand: here");
        new Thread() {
            @Override
            public void run() {
                Log.i(TAG, "Starting SpeckService...");
                startInForeground();
                initSpeckService();
                startServiceAndBluetoothScanning();
            }
        }.start();
        return START_STICKY;
    }

    private void startInForeground() {
        Log.d(TAG, "startInForeground: here");
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
            Intent notificationIntent = new Intent(this, HomeActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

            Notification notification = new Notification.Builder(this).setContentTitle(getText(R.string.notification_speck_title)).setContentText(getText(R.string.notification_speck_text)).setSmallIcon(R.drawable.vec_wireless_active).setContentIntent(pendingIntent).build();

            // Just use a "random" service ID
            final int SERVICE_NOTIFICATION_ID = 8598001;
            startForeground(SERVICE_NOTIFICATION_ID, notification);
        }
    }

    @Override
    public void onDestroy() {
        stopSpeckService();
        Log.i(TAG, "SpeckService has been stopped");
//        int pid = android.os.Process.myPid();
//        android.os.Process.killProcess(pid);

        super.onDestroy();

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // We don't allow threads to bind to this service. Once the service is started, it sends updates
        // via broadcasts and there is no need for calls from outside
        return null;
    }

    /**
     * Initiate Bluetooth adapter.
     */
    public void initSpeckService() {
        Log.d(TAG, "initSpeckService: here");
        loadConfigInstanceVariables();

        // Initializes a Bluetooth adapter. For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        BluetoothManager mBluetoothManager = (BluetoothManager) getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        // Get singleton instances of packet handler classes
        thingy1Handler = new Thingy1PacketHandler(this);
        thingy2Handler = new Thingy2PacketHandler(this);

        mIsThingy1Paused = false;
        mIsThingy2Paused = false;

        // Provide a method for forcefully scanning for bluetooth devices.
        // Useful when Bluetooth or GPS are dropped.
        // TODO: test with other connected devices
        registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                Log.i(TAG, "Received request to scan for Bluetooth devices...");
                startServiceAndBluetoothScanning();
            }
        }, SpeckIntentFilters.INSTANCE.getBluetoothServiceScanForDevicesIntentFilter());

    }

    private void loadConfigInstanceVariables() {
        Log.d(TAG, "loadConfigInstanceVariables: here");
        // Get references to Utils
        mIsThingy1Enabled = true;
        mIsThingy2Enabled = true;

        // Get Bluetooth address
        THINGY1_UUID = Utils.getThingy1UUID(this);
        THINGY2_UUID = Utils.getThingy2UUID(this);

        Log.i(TAG, "Thingy1 uuid found = " + THINGY1_UUID);
        Log.i(TAG, "Thingy2 uuid found = " + THINGY2_UUID);
    }

    /**
     * Check Bluetooth availability and initiate devices scanning.
     */
    public void startServiceAndBluetoothScanning() {
        Log.d(TAG, "startServiceAndBluetoothScanning: here");
        mIsServiceRunning = true;

        // Check if Bluetooth is supported on the device
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "This device does not support Bluetooth", Toast.LENGTH_LONG).show();
            return;
        }

        mIsThingy1Found = false;
        mIsThingy2Found = false;

        rxBleClient = RxBleClient.create(this);

        Log.i(TAG, "Scanning..");

        scanForDevices();
    }

    private void scanForDevices() {
        Log.d(TAG, "scanForDevices: here");
        scanSubscription = rxBleClient.scanBleDevices().subscribe(rxBleScanResult -> {

            if ((mIsThingy1Found || !mIsThingy1Enabled) && (mIsThingy2Found || !mIsThingy2Enabled)) {
                scanSubscription.unsubscribe();
            }

            if (mIsThingy1Enabled && !mIsThingy1Found) {
                if (rxBleScanResult.getBleDevice().getMacAddress().equalsIgnoreCase(THINGY1_UUID)) {
                    THINGY1_BLE_ADDRESS = THINGY1_UUID;
                    mIsThingy1Found = true;
                    Log.i(TAG, "Connecting after scanning");
                    BluetoothSpeckService.this.connectToThingy1();
                }
            }

            if (mIsThingy2Enabled && !mIsThingy2Found) {
                if (rxBleScanResult.getBleDevice().getMacAddress().equalsIgnoreCase(THINGY2_UUID)) {
                    THINGY2_BLE_ADDRESS = THINGY2_UUID;
                    mIsThingy2Found = true;
                    Log.i(TAG, "Connecting after scanning");
                    BluetoothSpeckService.this.connectToThingy2();
                }

            }

        }, throwable -> {
            // Handle an error here.
            Log.e(TAG, "Error while scanning: " + throwable.toString());
            Log.e(TAG, "Scanning again in 10 seconds");

            // Try again after timeout
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    scanForDevices();
                }
            }, 10000);
        });
    }

    private void connectToThingy1() {
        Log.d(TAG, "connectToThingy1: here");
        mThingy1Device = rxBleClient.getBleDevice(THINGY1_BLE_ADDRESS);
        mThingy1Name = mThingy1Device.getName();

        Log.d(TAG, "connectToThingy1: mThingy1Device = " + mThingy1Device.toString());
        Log.d(TAG, "connectToThingy1: mThingy1Name = " + mThingy1Name);

        // re-usable connect callback
        Consumer<RxBleConnection.RxBleConnectionState> establishConnectionAndSubscribe = (connectionState) -> {
            thingy1Connection = establishThingyConnection(mThingy1Device);

            Log.d(TAG, "connectToThingy1: set thingy1 handler");

            setupThingy1Subscription(thingy1Connection);

            // update the connection state coming from the connection observer
            mLastThingy1ConnectionState = connectionState;
        };

        mThingy1Device.observeConnectionStateChanges().subscribe(connectionState -> {
            if (connectionState == RxBleConnection.RxBleConnectionState.DISCONNECTED && mIsServiceRunning) {
                Log.d(TAG, "Thingy1 disconnected");
                Intent thingyDisconnectedIntent = new Intent(Constants.ACTION_THINGY1_DISCONNECTED);
                sendBroadcast(thingyDisconnectedIntent);

                if (mLastThingy1ConnectionState == RxBleConnection.RxBleConnectionState.CONNECTED) {
                    // If we were just disconnected, try to immediately connect again.
                    Log.i(TAG, "Thingy1 connection lost, trying to reconnect");
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            // last connection state was OK - only re-establish the connection
                            establishConnectionAndSubscribe.accept(connectionState);
                        }
                    }, 10000);
                } else if (mLastThingy1ConnectionState == RxBleConnection.RxBleConnectionState.CONNECTING) {
                    // This means we tried to reconnect, but there was a timeout. In this case we
                    // wait for x seconds before reconnecting
                    Log.i(TAG, String.format("Thingy1 connection timeout, waiting %d seconds before reconnect", Constants.RECONNECTION_TIMEOUT_MILLIS / 1000));
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            Log.i(TAG, "Thingy1 reconnecting...");
                            establishConnectionAndSubscribe.accept(connectionState);
                        }
                    }, Constants.RECONNECTION_TIMEOUT_MILLIS);
                }
            }
        }, throwable -> {
            Log.e(TAG, "Error occured while listening to Thingy1 connection state changes: " + throwable.getMessage());
        });

        // first time establishing the connection, so the last state was "disconnected"
        establishConnectionAndSubscribe.accept(RxBleConnection.RxBleConnectionState.DISCONNECTED);
    }

    private void connectToThingy2() {
        Log.d(TAG, "connectToThingy2: here");
        mThingy2Device = rxBleClient.getBleDevice(THINGY2_BLE_ADDRESS);
        mThingy2Name = mThingy2Device.getName();

        Log.d(TAG, "connectToThingy2: mThingy2Device = " + mThingy2Device.toString());
        Log.d(TAG, "connectToThingy2: mThingy2Name = " + mThingy2Name);

        // re-usable connect callback
        Consumer<RxBleConnection.RxBleConnectionState> establishConnectionAndSubscribe = (connectionState) -> {
            thingy2Connection = establishThingyConnection(mThingy2Device);

            Log.d(TAG, "connectToThingy2: set thingy2 handler");

            setupThingy2Subscription(thingy2Connection);

            // update the connection state coming from the connection observer
            mLastThingy2ConnectionState = connectionState;
        };

        mThingy2Device.observeConnectionStateChanges().subscribe(connectionState -> {
            if (connectionState == RxBleConnection.RxBleConnectionState.DISCONNECTED && mIsServiceRunning) {
                Log.d(TAG, "Thingy2 disconnected");
                Intent thingyDisconnectedIntent = new Intent(Constants.ACTION_THINGY2_DISCONNECTED);
                sendBroadcast(thingyDisconnectedIntent);

                if (mLastThingy2ConnectionState == RxBleConnection.RxBleConnectionState.CONNECTED) {
                    // If we were just disconnected, try to immediately connect again.
                    Log.i(TAG, "Thingy2 connection lost, trying to reconnect");
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            // last connection state was OK - only re-establish the connection
                            establishConnectionAndSubscribe.accept(connectionState);
                        }
                    }, 10000);
                } else if (mLastThingy2ConnectionState == RxBleConnection.RxBleConnectionState.CONNECTING) {
                    // This means we tried to reconnect, but there was a timeout. In this case we
                    // wait for x seconds before reconnecting
                    Log.i(TAG, String.format("Thingy2 connection timeout, waiting %d seconds before reconnect", Constants.RECONNECTION_TIMEOUT_MILLIS / 1000));
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            Log.i(TAG, "Thingy2 reconnecting...");
                            establishConnectionAndSubscribe.accept(connectionState);
                        }
                    }, Constants.RECONNECTION_TIMEOUT_MILLIS);
                }
            }
        }, throwable -> {
            Log.e(TAG, "Error occured while listening to Thingy2 connection state changes: " + throwable.getMessage());
        });

        // first time establishing the connection, so the last state was "disconnected"
        establishConnectionAndSubscribe.accept(RxBleConnection.RxBleConnectionState.DISCONNECTED);
    }

    private void setupThingy1Subscription(Observable<RxBleConnection> conn) {
        String characteristic = Constants.THINGY_MOTION_CHARACTERISTIC;

        Log.i(TAG, String.format("Setting up subscription, characteristic: %s", characteristic));

        Consumer<byte[]> cvHandler = (byte[] cv) -> thingy1Handler.processThingy1Packet(cv);

        // https://polidea.github.io/RxAndroidBle/#change-notifications
        Runnable subscribe = () -> thingy1LiveSubscription = conn.flatMap(rxBleConnection -> rxBleConnection.setupNotification(UUID.fromString(characteristic))).doOnNext(notificationObservable -> {
            // Notification has been set up
            Log.i(TAG, "Subscribed to Thingy");
            Intent thingyFoundIntent = new Intent(Constants.ACTION_THINGY1_CONNECTED);
            thingyFoundIntent.putExtra(Constants.Config.THINGY1_UUID, THINGY1_UUID);
            sendBroadcast(thingyFoundIntent);

        }).flatMap(notificationObservable -> notificationObservable).subscribe(bytes -> {
            if (mIsThingy1Paused) {
                Log.i(TAG, "Thingy packet ignored as paused mode on");
            } else {
                // Call requires API level 24 (current min is 22): java.util.function.Consumer#accept
                cvHandler.accept(bytes);
            }
        }, throwable -> {
            // An error with autoConnect means that we are disconnected
            String stackTrace = Utils.getStackTraceAsString(throwable);
            Log.e(TAG, "Thingy disconnected: " + stackTrace);

            Intent thingyDisconnectedIntent = new Intent(Constants.ACTION_THINGY1_DISCONNECTED);
            sendBroadcast(thingyDisconnectedIntent);
        });

        Log.i(TAG, String.format("Unsubscribing from subscription: %s", thingy1LiveSubscription));
        long connectDelay;
        try {
            thingy1LiveSubscription.unsubscribe();
            Log.i(TAG, "Unsubscribed from thingy! Pausing...");
            // if this is an unsubscribe, allow for enough time to change characteristic
            connectDelay = Constants.RESPECK_CHARACTERISTIC_CHANGE_TIMEOUT_MS;
        } catch (Exception e) {
            Log.w(TAG, String.format("Unsubscribe error: %s", e.getMessage()));
            // if the unsubscribe failed, there likely was no subscription,
            // so the connection can happen right away (short delay may be useful)
            connectDelay = 1000;
        }

        (new Handler()).postDelayed(subscribe, connectDelay);
//        subscribe.run();

    }

    private void setupThingy2Subscription(Observable<RxBleConnection> conn) {
        String characteristic = Constants.THINGY_MOTION_CHARACTERISTIC;

        Log.i(TAG, String.format("Setting up subscription, characteristic: %s", characteristic));

        Consumer<byte[]> cvHandler = (byte[] cv) -> thingy2Handler.processThingy2Packet(cv);

        // https://polidea.github.io/RxAndroidBle/#change-notifications
        Runnable subscribe = () -> thingy2LiveSubscription = conn.flatMap(rxBleConnection -> rxBleConnection.setupNotification(UUID.fromString(characteristic))).doOnNext(notificationObservable -> {
            // Notification has been set up
            Log.i(TAG, "Subscribed to Thingy");
            Intent thingyFoundIntent = new Intent(Constants.ACTION_THINGY2_CONNECTED);
            thingyFoundIntent.putExtra(Constants.Config.THINGY2_UUID, THINGY2_UUID);
            sendBroadcast(thingyFoundIntent);

        }).flatMap(notificationObservable -> notificationObservable).subscribe(bytes -> {
            if (mIsThingy2Paused) {
                Log.i(TAG, "Thingy packet ignored as paused mode on");
            } else {
                // Call requires API level 24 (current min is 22): java.util.function.Consumer#accept
                cvHandler.accept(bytes);
            }
        }, throwable -> {
            // An error with autoConnect means that we are disconnected
            String stackTrace = Utils.getStackTraceAsString(throwable);
            Log.e(TAG, "Thingy disconnected: " + stackTrace);

            Intent thingyDisconnectedIntent = new Intent(Constants.ACTION_THINGY2_DISCONNECTED);
            sendBroadcast(thingyDisconnectedIntent);
        });

        Log.i(TAG, String.format("Unsubscribing from subscription: %s", thingy2LiveSubscription));
        long connectDelay;
        try {
            thingy2LiveSubscription.unsubscribe();
            Log.i(TAG, "Unsubscribed from thingy! Pausing...");
            // if this is an unsubscribe, allow for enough time to change characteristic
            connectDelay = Constants.RESPECK_CHARACTERISTIC_CHANGE_TIMEOUT_MS;
        } catch (Exception e) {
            Log.w(TAG, String.format("Unsubscribe error: %s", e.getMessage()));
            // if the unsubscribe failed, there likely was no subscription,
            // so the connection can happen right away (short delay may be useful)
            connectDelay = 1000;
        }
        (new Handler()).postDelayed(subscribe, connectDelay);
//        subscribe.run();

    }

    private Observable<RxBleConnection> establishThingyConnection(RxBleDevice device) {
        Log.d(TAG, "establishThingyConnection: here");
        Log.i(TAG, "Connecting to Thingy...");

        return device.establishConnection(false);
    }

    public void stopSpeckService() {
        Log.i(TAG, "Stopping SpeckService");
        mIsServiceRunning = false;

        if (scanSubscription != null) {
            Log.i(TAG, "stopSpeckService: unsubscribed scansub");
            scanSubscription.unsubscribe();
        }

        if (thingy1LiveSubscription != null) {
            Log.i(TAG, "stopSpeckService: unsubscribed thingy1sub");
            thingy1LiveSubscription.unsubscribe();
        }

        if (thingy2LiveSubscription != null) {
            Log.i(TAG, "stopSpeckService: unsubscribed thingy2sub");
            thingy2LiveSubscription.unsubscribe();
        }

    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

}