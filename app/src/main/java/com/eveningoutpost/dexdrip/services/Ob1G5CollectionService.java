package com.eveningoutpost.dexdrip.services;

import static com.eveningoutpost.dexdrip.Home.get_engineering_mode;
import static com.eveningoutpost.dexdrip.g5model.CalibrationState.Ok;
import static com.eveningoutpost.dexdrip.g5model.G6CalibrationParameters.getCurrentSensorCode;
import static com.eveningoutpost.dexdrip.models.JoH.msSince;
import static com.eveningoutpost.dexdrip.models.JoH.niceTimeScalar;
import static com.eveningoutpost.dexdrip.models.JoH.tsl;
import static com.eveningoutpost.dexdrip.services.Ob1G5CollectionService.STATE.CLOSE;
import static com.eveningoutpost.dexdrip.services.Ob1G5CollectionService.STATE.CLOSED;
import static com.eveningoutpost.dexdrip.services.Ob1G5CollectionService.STATE.CONNECT_NOW;
import static com.eveningoutpost.dexdrip.services.Ob1G5CollectionService.STATE.GET_DATA;
import static com.eveningoutpost.dexdrip.services.Ob1G5CollectionService.STATE.INIT;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.G5_CALIBRATION_REQUEST;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.G5_SENSOR_FAILED;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.G5_SENSOR_RESTARTED;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.G5_SENSOR_STARTED;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.MINUTE_IN_MS;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.SECOND_IN_MS;
import static com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.BAD;
import static com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.CRITICAL;
import static com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.NORMAL;
import static com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.NOTICE;
import static com.eveningoutpost.dexdrip.utils.bt.Subscription.addErrorHandler;
import static com.eveningoutpost.dexdrip.watch.thinjam.BlueJayEntry.isNative;
import static com.eveningoutpost.dexdrip.xdrip.gs;

import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;

import android.text.SpannableString;
import android.text.SpannableStringBuilder;

import com.eveningoutpost.dexdrip.AddCalibration;
import com.eveningoutpost.dexdrip.DoubleCalibrationActivity;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.g5model.CalibrationState;
import com.eveningoutpost.dexdrip.g5model.DexSyncKeeper;
import com.eveningoutpost.dexdrip.g5model.FirmwareCapability;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.models.Treatments;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.plugin.IPluginDA;
import com.eveningoutpost.dexdrip.utilitymodels.BroadcastGlucose;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.StatusItem;
import com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight;
import com.eveningoutpost.dexdrip.utils.bt.Subscription;
import com.eveningoutpost.dexdrip.watch.thinjam.BlueJayEntry;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.common.collect.Sets;
import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleCustomOperation;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.internal.RxBleLog;
import com.polidea.rxandroidble2.internal.connection.RxBleGattCallback;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import lombok.Getter;
import lombok.Setter;
import lombok.val;


/**
 * OB1 G5/G6 collector
 * Created by jamorham on 16/09/2017.
 * <p>
 * App version is master, best to avoid editing wear version directly
 */


public class Ob1G5CollectionService extends G5BaseService {

    public static final String TAG = Ob1G5CollectionService.class.getSimpleName();
    public static final String OB1G5_PREFS = "use_ob1_g5_collector_service";
    private static final String OB1G5_MACSTORE = "G5-mac-for-txid-";
    private static final String OB1G5_STATESTORE = "ob1-state-store-";
    private static final String OB1G5_STATESTORE_TIME = "ob1-state-store-time";
    private static final int DEFAULT_AUTOMATA_DELAY = 100;
    private static final String BUGGY_SAMSUNG_ENABLED = "buggy-samsung-enabled";
    private static final String STOP_SCAN_TASK_ID = "ob1-g5-scan-timeout_scan";
    private static final String KEKS = "keks";
    private static final String KEKS_ONE = "keks1_";
    private static volatile STATE state = INIT;
    private static volatile STATE last_automata_state = CLOSED;

    private static RxBleClient rxBleClient;
    private static volatile PendingIntent pendingIntent;

    private static volatile String transmitterID;
    private static volatile String transmitterMAC;
    private static volatile String historicalTransmitterMAC;
    private static String transmitterIDmatchingMAC;

    private static volatile String lastScanError = null;
    public static volatile String lastSensorStatus = null;
    public static volatile CalibrationState lastSensorState = null;
    public static volatile long lastUsableGlucosePacketTime = 0;
    private static volatile String static_connection_state = null;
    public static volatile long static_last_connected = 0;
    @Setter
    @Getter
    private static long last_transmitter_timestamp = 0;
    private static long lastStateUpdated = 0;
    private static long wakeup_time = 0;
    private static long wakeup_jitter = 0;
    private static long max_wakeup_jitter = 0;


    public static boolean keep_running = true;

    public static boolean android_wear = false;
    public static boolean wear_broadcast = false;

    private static volatile Subscription scanSubscription;
    private static volatile Subscription connectionSubscription;
    private static volatile Subscription stateSubscription;
    private Subscription discoverSubscription;
    private RxBleDevice bleDevice;
    private RxBleConnection connection;
    public volatile IPluginDA plugin;

    private PowerManager.WakeLock connection_linger;
    private volatile PowerManager.WakeLock scanWakeLock;
    private volatile PowerManager.WakeLock floatingWakeLock;
    private PowerManager.WakeLock fullWakeLock;

    private volatile boolean background_launch_waiting = false;
    private static volatile long last_scan_started = -1;
    private static volatile int error_count = 0;
    private static volatile int retry_count = 0;
    private int error_backoff_ms = 1000;
    private static final int max_error_backoff_ms = 10000;
    private static final long TOLERABLE_JITTER = 10000;
    private static final boolean d = false;

    private static volatile boolean always_scan = false;
    private static volatile boolean scan_next_run = true;
    private static boolean always_connect = false;

    private static final Set<String> alwaysScanModels = Sets.newHashSet("SM-N910V", "G Watch");
    private static final List<String> alwaysScanModelFamilies = Arrays.asList("SM-N910");
    private static final Set<String> alwaysConnectModels = Sets.newHashSet("G Watch");
    private static final Set<String> alwaysBuggyWakeupModels = Sets.newHashSet("Jelly-Pro", "SmartWatch 3");
    private static final HashMap<String, Long> failureTally = new HashMap<>();

    // Internal process state tracking
    public enum STATE {
        INIT("Initializing"),
        SCAN("Scanning"),
        CONNECT("Waiting connect"),
        CONNECT_NOW("Power connect"),
        DISCOVER("Examining"),
        CHECK_AUTH("Checking Auth"),
        PREBOND("Bond Prepare"),
        BOND("Bonding"),
        UNBOND("UnBonding"),
        RESET("Reseting"),
        GET_DATA("Getting Data"),
        CLOSE("Sleeping"),
        CLOSED("Deep Sleeping");


        private String str;

        STATE(String custom) {
            this.str = custom;
        }

        public String getString() {
            return str;
        }
    }

    public void authResult(boolean good) {
    }

    public synchronized void background_automata(final int timeout) {
        if (background_launch_waiting) {
            UserError.Log.d(TAG, "Blocked by existing background automata pending");
            return;
        }
        final PowerManager.WakeLock wl = JoH.getWakeLock("jam-g5-background", timeout + 5000);
        background_launch_waiting = true;
        new Thread(() -> {
            JoH.threadSleep(timeout);
            background_launch_waiting = false;
            JoH.releaseWakeLock(wl);
        }).start();
    }

    private static boolean specialPairingWorkaround() {
        return Pref.getBooleanDefaultFalse("ob1_special_pairing_workaround");
    }

    public STATE getState() {
        return state;
    }

    public void changeState(STATE new_state) {
        UserError.Log.d(TAG, "Stopping service due to having being disabled in preferences");
        stopSelf();
    }

    private static void init_tx_id() {
        val TXID_PREF = "dex_txid";
        val txid = Pref.getString(TXID_PREF, "NULL");
        val txid_filtered = txid.trim();
        transmitterID = txid_filtered;
        if (!txid.equals(txid_filtered)) {
            Pref.setString(TXID_PREF, txid_filtered);
            UserError.Log.wtf(TAG, "Had to fix invalid txid: :" + txid + ": -> :" + txid_filtered + ":");
        }
    }

    public synchronized void reset_bond(boolean allow) {
        if (allow || (JoH.pratelimit("ob1-bond-cycle", 7200))) {
            UserError.Log.e(TAG, "Attempting to refresh bond state");
            msg("Resetting Bond");
            unBond();
            do_create_bond();
        }
    }

    private synchronized void do_create_bond() {
        final boolean isDeviceLocallyBonded = isDeviceLocallyBonded();
        UserError.Log.d(TAG, "Attempting to create bond, device is : " + (isDeviceLocallyBonded ? "BONDED" : "NOT Bonded"));

        if (isDeviceLocallyBonded && getInitiateBondingFlag()) {
            UserError.Log.e(TAG, "Device is marked as bonded but we are being asked to bond so attempting to unbond first");
            unbondIfAllowed();
            changeState(CLOSE);
        } else {
            try {
                if (transmitterID.length() > 4) {

                } else {
                    startInitiateBondReal();
                }
                //background_automata(10000);
            } catch (Exception e) {
                UserError.Log.wtf(TAG, "Got exception in do_create_bond() " + e);
            }
        }
    }

    private void startInitiateBondReal() {
        try {
            weInitiatedBondConfirmation = 1;
            instantCreateBondIfAllowed();
        } catch (Exception e) {
            UserError.Log.wtf(TAG, "Got exception in startInitiateBondReal() " + e);
        }
    }

    public static String getMac() {
        return transmitterMAC;
    }


    public static synchronized boolean isDeviceLocallyBonded() {
        if (transmitterMAC == null) return false;
        final Set<RxBleDevice> pairedDevices = rxBleClient.getBondedDevices();
        if ((pairedDevices != null) && (pairedDevices.size() > 0)) {
            for (RxBleDevice device : pairedDevices) {
                if ((device.getMacAddress() != null) && (device.getMacAddress().equals(transmitterMAC))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean immediateBonding() {
        return Pref.getBooleanDefaultFalse("engineering_ob1_bonding_test") || isNative();
    }

    public static boolean ignoreBonding() {
        return Pref.getBooleanDefaultFalse("engineering_ob1_ignore_bonding");
    }

    public synchronized void unBond() {

        UserError.Log.d(TAG, "unBond() start");
        if (transmitterMAC == null) return;

        final BluetoothAdapter mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();

        final Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (final BluetoothDevice device : pairedDevices) {
                if (device.getAddress() != null) {
                    if (device.getAddress().equals(transmitterMAC)) {
                        try {

                            UserError.Log.e(TAG, "removingBond: " + transmitterMAC);
                            final Method m = device.getClass().getMethod("removeBond", (Class[]) null);
                            m.invoke(device, (Object[]) null);
                            // TODO interpret boolean response
                            break;

                        } catch (Exception e) {
                            UserError.Log.e(TAG, e.getMessage(), e);
                        }
                    }

                }
            }
        }
        UserError.Log.d(TAG, "unBond() finished");
    }


    public static String getTransmitterID() {
        if (transmitterID == null) {
            init_tx_id();
        }
        return transmitterID;
    }

    public synchronized void savePersist() {
        if (plugin != null) {
            PersistentStore.cleanupOld(KEKS_ONE);
            PersistentStore.setBytes(KEKS_ONE + transmitterMAC, plugin.getPersistence(1));
        }
    }

    public static void clearPersistStore() {
        PersistentStore.cleanupOld(KEKS_ONE);
        PersistentStore.cleanupOld(OB1G5_MACSTORE);
    }


    public void incrementErrors() {
        error_count++;
        if (error_count > 1) {
            UserError.Log.e(TAG, "Error count reached: " + error_count);
        }
    }

    public int incrementRetry() {
        retry_count++;
        return retry_count;
    }

    public void clearErrors() {
        error_count = 0;
    }

    private void checkAlwaysScanModels() {
        final String this_model = Build.MODEL;
        UserError.Log.d(TAG, "Checking model: " + this_model);

        if ((JoH.isSamsung() && PersistentStore.getLong(BUGGY_SAMSUNG_ENABLED) > 4)) {
            UserError.Log.d(TAG, "Enabling wake workaround due to persistent metric");
            JoH.buggy_samsung = true;
        }

        always_connect = alwaysConnectModels.contains(this_model);

        if (alwaysBuggyWakeupModels.contains(this_model)) {
            UserError.Log.e(TAG, "Always buggy wakeup exact match for " + this_model);
            JoH.buggy_samsung = true;
        }

        if (alwaysScanModels.contains(this_model)) {
            UserError.Log.e(TAG, "Always scan model exact match for: " + this_model);
            always_scan = true;
            return;
        }

        for (String check : alwaysScanModelFamilies) {
            if (this_model.startsWith(check)) {
                UserError.Log.e(TAG, "Always scan model fuzzy match for: " + this_model);
                always_scan = true;
                return;
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            UserError.Log.wtf(TAG, "Not high enough Android version to run: " + Build.VERSION.SDK_INT);
        } else {

            try {
                registerReceiver(mBondStateReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
            } catch (Exception e) {
                UserError.Log.e(TAG, "Could not register bond state receiver: " + e);
            }

            final IntentFilter pairingRequestFilter = new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST);
            pairingRequestFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY - 1);
            try {
                if (Build.VERSION.SDK_INT < 26) {
                    registerReceiver(mPairingRequestRecevier, pairingRequestFilter);
                } else {
                    UserError.Log.d(TAG, "Not registering pairing receiver on Android 8+");
                }
            } catch (Exception e) {
                UserError.Log.e(TAG, "Could not register pairing request receiver:" + e);
            }

            checkAlwaysScanModels();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                android_wear = JoH.areWeRunningOnAndroidWear();
                if (android_wear) {
                    UserError.Log.d(TAG, "We are running on Android Wear");
                    wear_broadcast = Pref.getBooleanDefaultFalse("ob1_wear_broadcast");
                }
            }
        }
        if (d) RxBleClient.setLogLevel(RxBleLog.DEBUG);
        addErrorHandler(TAG);
        listenForChangeInSettings(true);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        xdrip.checkAppContext(getApplicationContext());
        final PowerManager.WakeLock wl = JoH.getWakeLock("g5-start-service", 310000);
        try {
            UserError.Log.d(TAG, "WAKE UP WAKE UP WAKE UP WAKE UP @ " + JoH.dateTimeText(tsl()));
            msg("Wake up");
            if (wakeup_time > 0) {
                wakeup_jitter = msSince(wakeup_time);
                if (wakeup_jitter < 0) {
                    UserError.Log.d(TAG, "Woke up Early..");
                } else {
                    if (wakeup_jitter > 1000) {
                        UserError.Log.d(TAG, "Wake up, time jitter: " + niceTimeScalar(wakeup_jitter));
                        if ((wakeup_jitter > TOLERABLE_JITTER) && (!JoH.buggy_samsung) && JoH.isSamsung()) {
                            UserError.Log.wtf(TAG, "Enabled wake workaround due to jitter of: " + niceTimeScalar(wakeup_jitter));
                            JoH.buggy_samsung = true;
                            PersistentStore.incrementLong(BUGGY_SAMSUNG_ENABLED);
                            max_wakeup_jitter = 0;
                        } else {
                            max_wakeup_jitter = Math.max(max_wakeup_jitter, wakeup_jitter);
                        }

                    }
                }
            }

            UserError.Log.d(TAG, "Stopping service due to shouldServiceRun() result");
            msg("Stopping");
            stopSelf();
            return START_NOT_STICKY;
        } finally {
            JoH.releaseWakeLock(wl);
        }
    }

    @Override
    public void onDestroy() {
        msg("Shutting down");
        if (pendingIntent != null) {
            JoH.cancelAlarm(this, pendingIntent);
            pendingIntent = null;
            wakeup_time = 0;
        }
        stopScan();
        stopDiscover();
        stopConnect();
        scanSubscription = null;
        connectionSubscription = null;
        stateSubscription = null;
        discoverSubscription = null;

        listenForChangeInSettings(false);
        unregisterPairingReceiver();

        try {
            unregisterReceiver(mBondStateReceiver);
        } catch (Exception e) {
            UserError.Log.e(TAG, "Got exception unregistering pairing receiver: " + e);
        }

        state = INIT; // Should be STATE.END ?
        last_automata_state = CLOSED;
        msg("Service Stopped");
        super.onDestroy();
    }

    public void unregisterPairingReceiver() {
        try {
            unregisterReceiver(mPairingRequestRecevier);
        } catch (Exception e) {
            UserError.Log.d(TAG, "Got exception unregistering pairing receiver: " + e);
        }
    }

    private synchronized void stopScan() {
        if (scanSubscription != null) {
            scanSubscription.unsubscribe();
        }
        UserError.Log.d(TAG, "DEBUG: killing stop scan task");
        Inevitable.kill(STOP_SCAN_TASK_ID);
        if (scanWakeLock != null) {
            JoH.releaseWakeLock(scanWakeLock);
        }
        last_scan_started = 0;
    }

    private synchronized void stopConnect() {
        if (connectionSubscription != null) {
            connectionSubscription.unsubscribe();
        }
        if (stateSubscription != null) {
            stateSubscription.unsubscribe();
        }
    }

    private synchronized void stopDiscover() {
        if (discoverSubscription != null) {
            discoverSubscription.unsubscribe();
        }
    }


    public static void clearScanError() {
        lastScanError = null;
    }


    public void saveTransmitterMac() {
        UserError.Log.d(TAG, "Saving transmitter mac: " + transmitterID + " = " + transmitterMAC);
        PersistentStore.cleanupOld(OB1G5_MACSTORE);
        PersistentStore.setString(OB1G5_MACSTORE + transmitterID, transmitterMAC);
    }

    private void unbondIfAllowed() {
        if (Pref.getBoolean("ob1_g5_allow_resetbond", true)) {
            unBond();
        } else {
            UserError.Log.e(TAG, "Would have tried to unpair but preference setting prevents it. (unbond)");
        }
    }

    public void tryGattRefresh() {
        if (JoH.ratelimit("ob1-gatt-refresh", 60)) {
            if (Pref.getBoolean("use_gatt_refresh", true)) {
                try {
                    if (connection != null)
                        UserError.Log.d(TAG, "Trying gatt refresh queue");
                    connection.queue((new GattRefreshOperation(0))).timeout(2, TimeUnit.SECONDS).subscribe(
                            readValue -> {
                                UserError.Log.d(TAG, "Refresh OK: " + readValue);
                            }, throwable -> {
                                UserError.Log.d(TAG, "Refresh exception: " + throwable);
                            });
                } catch (NullPointerException e) {
                    UserError.Log.d(TAG, "Probably harmless gatt refresh exception: " + e);
                } catch (Exception e) {
                    UserError.Log.d(TAG, "Got exception trying gatt refresh: " + e);
                }
            } else {
                UserError.Log.d(TAG, "Gatt refresh rate limited");
            }
        }
    }

    public void connectionStateChange(String connection_state) {
        static_connection_state = connection_state;
    }

    public static void updateLast(long timestamp) {
        if ((static_last_timestamp == 0) && (transmitterID != null)) {
            final String ref = "last-ob1-data-" + transmitterID;
            if (PersistentStore.getLong(ref) == 0) {
                PersistentStore.setLong(ref, timestamp);
                if (!android_wear) JoH.playResourceAudio(R.raw.labbed_musical_chime);
            }
        }
        static_last_timestamp = timestamp;
    }

    private static class GattRefreshOperation implements RxBleCustomOperation<Void> {
        private long delay_ms = 500;


        GattRefreshOperation(long delay_ms) {
            this.delay_ms = delay_ms;
        }

        @NonNull
        @Override
        public Observable<Void> asObservable(BluetoothGatt bluetoothGatt,
                                             RxBleGattCallback rxBleGattCallback,
                                             Scheduler scheduler) throws Throwable {

            return Observable.fromCallable(() -> refreshDeviceCache(bluetoothGatt))
                    .delay(delay_ms, TimeUnit.MILLISECONDS, Schedulers.computation())
                    .subscribeOn(scheduler);
        }

        private Void refreshDeviceCache(final BluetoothGatt gatt) {
            UserError.Log.d(TAG, "Gatt Refresh " + (JoH.refreshDeviceCache(TAG, gatt) ? "succeeded" : "failed"));
            return null;
        }
    }

    private int currentBondState = 0;
    public volatile int waitingBondConfirmation = 0; // 0 = not waiting, 1 = waiting, 2 = received
    public volatile int weInitiatedBondConfirmation = 0; // 0 = not waiting, 1 = waiting, 2 = received
    final BroadcastReceiver mBondStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!keep_running) {
                try {
                    UserError.Log.e(TAG, "Rogue bond state receiver still active - unregistering");
                    unregisterReceiver(mBondStateReceiver);
                } catch (Exception e) {
                    //
                }
                return;
            }
            final String action = intent.getAction();
            UserError.Log.d(TAG, "BondState: onReceive ACTION: " + action);
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final BluetoothDevice parcel_device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                currentBondState = parcel_device.getBondState();
                final int bond_state_extra = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                final int previous_bond_state_extra = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);

                UserError.Log.e(TAG, "onReceive UPDATE Name " + parcel_device.getName() + " Value " + parcel_device.getAddress()
                        + " Bond state " + parcel_device.getBondState() + bondState(parcel_device.getBondState()) + " "
                        + "bs: " + bondState(bond_state_extra) + " was " + bondState(previous_bond_state_extra));
                try {
                    if (parcel_device.getAddress().equals(transmitterMAC)) {
                        msg(bondState(bond_state_extra).replace(" ", ""));
                        if (parcel_device.getBondState() == BluetoothDevice.BOND_BONDED) {

                            if (waitingBondConfirmation == 1) {
                                waitingBondConfirmation = 2; // received
                                UserError.Log.e(TAG, "Bond confirmation received!");
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                    UserError.Log.d(TAG, "Sleeping before create bond");
                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException e) {
                                        //
                                    }
                                    instantCreateBondIfAllowed();
                                }
                            }

                            if (weInitiatedBondConfirmation == 1) {
                                weInitiatedBondConfirmation = 2;
                                changeState(GET_DATA);
                            }
                        } else if (parcel_device.getBondState() == BluetoothDevice.BOND_BONDING) {
                            if (Build.VERSION.SDK_INT >= 26) {
                                JoH.playResourceAudio(R.raw.bt_meter_connect);
                                UserError.Log.uel(TAG, "Prompting user to notice pairing request with sound - On Android 8+ you have to manually pair when requested");
                            }
                        }
                    }
                } catch (Exception e) {
                    UserError.Log.e(TAG, "Got exception trying to process bonded confirmation: ", e);
                }
            }
        }
    };

    public void instantCreateBondIfAllowed() {
        if (getInitiateBondingFlag()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    UserError.Log.d(TAG, "instantCreateBond() called");
                    bleDevice.getBluetoothDevice().createBond();
                }
            } catch (Exception e) {
                UserError.Log.e(TAG, "Got exception in instantCreateBond() " + e);
            }
        } else {
            UserError.Log.e(TAG, "instantCreateBond blocked by lack of initiate_bonding flag");
        }
    }


    private boolean getInitiateBondingFlag() {
        return true; // There is no reason not to initiate bonding
    }


    private final BroadcastReceiver mPairingRequestRecevier = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!keep_running) {
                try {
                    UserError.Log.e(TAG, "Rogue pairing request receiver still active - unregistering");
                    unregisterReceiver(mPairingRequestRecevier);
                } catch (Exception e) {
                    //
                }
                return;
            }
            if ((bleDevice != null) && (bleDevice.getBluetoothDevice().getAddress() != null)) {
                UserError.Log.e(TAG, "Processing mPairingRequestReceiver !!!");
                JoH.releaseWakeLock(fullWakeLock);
                fullWakeLock = JoH.fullWakeLock("pairing-screen-wake", 30 * Constants.SECOND_IN_MS);
                if (!android_wear) Home.startHomeWithExtra(context, Home.HOME_FULL_WAKEUP, "1");
                if (!JoH.doPairingRequest(context, this, intent, bleDevice.getBluetoothDevice().getAddress())) {
                    if (!android_wear) {
                        unregisterPairingReceiver();
                        UserError.Log.e(TAG, "Pairing failed so removing pairing automation"); // todo use flag
                    }
                }
            } else {
                UserError.Log.e(TAG, "Received pairing request but device was null !!!");
            }
        }
    };


    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private static final String NEEDING_CALIBRATION = "G5_NEEDING_CALIBRATION";
    private static final String IS_STARTED = "G5_IS_STARTED";
    private static final String IS_FAILED = "G5_IS_FAILED";

    private static volatile long lastProcessCalibrationState;

    public static void processCalibrationStateLite(final CalibrationState state, final long incomingTimestamp) {
        if (incomingTimestamp > lastProcessCalibrationState) {
            processCalibrationStateLite(state);
        } else {
            UserError.Log.d(TAG, "Ignoring calibration state as it is: " + JoH.dateTimeText(incomingTimestamp) + " vs local: " + JoH.dateTimeText(lastProcessCalibrationState));
        }
    }

    public static boolean processCalibrationStateLite(final CalibrationState state) {
        if (state == CalibrationState.Unknown) {
            UserError.Log.d(TAG, "Not processing push of unknown state as this is the unset state");
            return false;
        }

        if (msSince(lastProcessCalibrationState) < MINUTE_IN_MS) {
            UserError.Log.d(TAG, "Ignoring duplicate processCalibration State");
            return false;
        }
        lastProcessCalibrationState = tsl();

        lastSensorStatus = state.getExtendedText();
        lastSensorState = state;
        return true;
    }

    public static void processCalibrationState(final CalibrationState state) {

        if (!processCalibrationStateLite(state)) {
            UserError.Log.d(TAG, "Not processing more calibration state as lite returned false");
            return;
        }

        storeCalibrationState(state);

        final boolean needs_calibration = state.needsCalibration();
        final boolean was_needing_calibration = PersistentStore.getBoolean(NEEDING_CALIBRATION);

        final boolean is_started = state.sensorStarted();
        final boolean was_started = PersistentStore.getBoolean(IS_STARTED);

        final boolean is_failed = state.sensorFailed();
        final boolean was_failed = PersistentStore.getBoolean(IS_FAILED);


        if (needs_calibration && !was_needing_calibration) {
            final Class c;
            switch (state) {
                case NeedsFirstCalibration:
                    c = DoubleCalibrationActivity.class;
                    break;
                default:
                    c = AddCalibration.class;
                    break;
            }

            Inevitable.task("ask initial calibration", SECOND_IN_MS * 30, () -> {
                final PendingIntent pi = PendingIntent.getActivity(xdrip.getAppContext(), G5_CALIBRATION_REQUEST, JoH.getStartActivityIntent(c), PendingIntent.FLAG_UPDATE_CURRENT);
                // pending intent not used on wear
                JoH.showNotification(state.getText(), "Calibration Required", android_wear ? null : pi, G5_CALIBRATION_REQUEST, state == CalibrationState.NeedsFirstCalibration, true, false);

            });
        } else if (!needs_calibration && was_needing_calibration) {
            JoH.cancelNotification(G5_CALIBRATION_REQUEST);
        }


        if (!is_started && was_started) {
            if (Sensor.isActive()) {
                if (Pref.getBooleanDefaultFalse("ob1_g5_restart_sensor")) {
                    if (state.ended()) {
                        UserError.Log.uel(TAG, "Requesting time-travel restart");
                        // start deleted
                    } else {
                        UserError.Log.uel(TAG, "Attempting to auto-start sensor");
                        // start deleted
                    }
                    final PendingIntent pi = PendingIntent.getActivity(xdrip.getAppContext(), G5_SENSOR_RESTARTED, JoH.getStartActivityIntent(Home.class), PendingIntent.FLAG_UPDATE_CURRENT);
                    JoH.showNotification("Auto Start", "Sensor Requesting Restart", pi, G5_SENSOR_RESTARTED, true, true, false);
                } else {
                    UserError.Log.uel(TAG, "Marking sensor session as stopped");
                    Sensor.stopSensor();
                }
            }
            final PendingIntent pi = PendingIntent.getActivity(xdrip.getAppContext(), G5_SENSOR_STARTED, JoH.getStartActivityIntent(Home.class), PendingIntent.FLAG_UPDATE_CURRENT);
            JoH.showNotification(state.getText(), "Sensor Stopped", pi, G5_SENSOR_STARTED, true, true, false);
            UserError.Log.ueh(TAG, "Native Sensor is now Stopped: " + state.getExtendedText());
            Treatments.sensorStop(null, "Stopped by transmitter: " + state.getExtendedText());
        } else if (is_started && !was_started) {
            JoH.cancelNotification(G5_SENSOR_STARTED);
            UserError.Log.ueh(TAG, "Native Sensor is now Started: " + state.getExtendedText());
            Treatments.sensorStartIfNeeded();
        }

        if (is_failed && !was_failed) {
            final PendingIntent pi = PendingIntent.getActivity(xdrip.getAppContext(), G5_SENSOR_FAILED, JoH.getStartActivityIntent(Home.class), PendingIntent.FLAG_UPDATE_CURRENT);
            JoH.showNotification(state.getText(), "Sensor FAILED", pi, G5_SENSOR_FAILED, true, true, false);
            UserError.Log.ueh(TAG, "Native Sensor is now marked FAILED: " + state.getExtendedText());
        }
        // we can't easily auto-cancel a failed notice as auto-restart may mean the user is not aware of it?


        updateG5State(needs_calibration, was_needing_calibration, NEEDING_CALIBRATION);
        updateG5State(is_started, was_started, IS_STARTED);
        updateG5State(is_failed, was_failed, IS_FAILED);
    }


    private static void updateG5State(boolean now, boolean previous, String reference) {
        if (now != previous) {
            PersistentStore.setBoolean(reference, now);
        }
    }

    private static void storeCalibrationState(final CalibrationState state) {
        PersistentStore.setByte(OB1G5_STATESTORE, state.getValue());
        PersistentStore.setLong(OB1G5_STATESTORE_TIME, tsl());
    }

    public static void msg(String msg) {
        lastState = msg + " " + JoH.hourMinuteString();
        UserError.Log.d(TAG, "Status: " + lastState);
        lastStateUpdated = tsl();
        if (android_wear && wear_broadcast) {
            BroadcastGlucose.sendLocalBroadcast(null);
        }
    }

    // data for NanoStatus
    public static SpannableString nanoStatus() {
        if (android_wear) {
            final SpannableStringBuilder builder = new SpannableStringBuilder();
            builder.append(lastSensorStatus != null ? lastSensorStatus + "\n" : "");
            builder.append(state.getString());
            return new SpannableString(builder);
        } else {
            return null;
        }
    }

    private static final String PREF_PURDAH = "ob1g5-purdah-time";

    // data for MegaStatus
    public static List<StatusItem> megaStatus() {

        init_tx_id(); // needed if we have not passed through local INIT state

        final List<StatusItem> l = new ArrayList<>();

        if (!DexSyncKeeper.isReady(transmitterID)) {
            l.add(new StatusItem("Hunting Transmitter", "Stay on this page", CRITICAL));
        }

        if (isVolumeSilent() && !isDeviceLocallyBonded()) {
            l.add(new StatusItem("Turn Sound On!", "You will not hear pairing request with volume set low or do not disturb enabled!", CRITICAL));
        }

        l.add(new StatusItem("Phone Service State", lastState + (BlueJayEntry.isPhoneCollectorDisabled() ? "\nDisabled by BlueJay option" : ""), msSince(lastStateUpdated) < 300000 ? (lastState.startsWith("Got data") ? Highlight.GOOD : NORMAL) : (isWatchRunning() ? Highlight.GOOD : CRITICAL)));
        if (last_scan_started > 0) {
            final long scanning_time = msSince(last_scan_started);
            l.add(new StatusItem("Time scanning", niceTimeScalar(scanning_time), scanning_time > MINUTE_IN_MS * 5 ? (scanning_time > MINUTE_IN_MS * 10 ? BAD : NOTICE) : NORMAL));
        }
        if (lastScanError != null) {
            l.add(new StatusItem("Scan Error", lastScanError, BAD));
        }
        if ((lastSensorStatus != null)) {
            l.add(new StatusItem("Sensor Status", lastSensorStatus, lastSensorState != Ok ? NOTICE : NORMAL));
        }

        if (hardResetTransmitterNow) {
            l.add(new StatusItem("Hard Reset", "Attempting - please wait", Highlight.CRITICAL));
        }

        if (transmitterID != null) {
            l.add(new StatusItem("Transmitter ID", transmitterID + ((transmitterMAC != null && get_engineering_mode()) ? "\n" + transmitterMAC : "")));
        }

        if (static_connection_state != null) {
            l.add(new StatusItem("Bluetooth Link", static_connection_state));
        }

        if (static_last_connected > 0) {
            l.add(new StatusItem("Last Connected", niceTimeScalar(msSince(static_last_connected)) + " ago"));
        }

        if ((!lastState.startsWith("Service Stopped")) && (!lastState.startsWith("Not running")))
            l.add(new StatusItem("Brain State", state.getString() + (error_count > 1 ? " Errors: " + error_count : ""), error_count > 1 ? NOTICE : error_count > 4 ? BAD : NORMAL));

        if (lastUsableGlucosePacketTime != 0) {
            if (msSince(lastUsableGlucosePacketTime) < MINUTE_IN_MS * 15) {
                l.add(new StatusItem("Native Algorithm", "Data Received " + JoH.hourMinuteString(lastUsableGlucosePacketTime), Highlight.GOOD));
            }
        }

        if (max_wakeup_jitter > 5000) {
            l.add(new StatusItem("Slowest Wakeup ", niceTimeScalar(max_wakeup_jitter), max_wakeup_jitter > Constants.SECOND_IN_MS * 10 ? CRITICAL : NOTICE));
        }

        if (JoH.buggy_samsung) {
            l.add(new StatusItem("Buggy handset", "Using workaround", max_wakeup_jitter < TOLERABLE_JITTER ? Highlight.GOOD : BAD));
        }

        final String tx_id = getTransmitterID();

        if (Pref.getBooleanDefaultFalse("wear_sync") &&
                Pref.getBooleanDefaultFalse("enable_wearG5")) {
            l.add(new StatusItem("Watch Service State", lastStateWatch));
            if (static_last_timestamp_watch > 0) {
                l.add(new StatusItem("Watch got Glucose", JoH.niceTimeSince(static_last_timestamp_watch) + " ago"));
            }
        }
        final String sensorCode = getCurrentSensorCode();
        if (sensorCode != null) {
            if (usingG6() && FirmwareCapability.isTransmitterG6(getTransmitterID())) {
                l.add(new StatusItem("Calibration Code", sensorCode));
            }
        }

        if (JoH.quietratelimit("update-g5-battery-warning", 10)) {
            updateBatteryWarningLevel();
        }

        l.add(new StatusItem("Battery Info Unavailable", "Click to trigger update", NORMAL, "long-press",
                new Runnable() {
                    @Override
                    public void run() {
                        getBatteryStatusNow = true;
                    }
                }));

        return l;
    }

    public static void resetSomeInternalState() {
        UserError.Log.d(TAG, "Resetting internal state by request");
        transmitterMAC = null; // probably gets reloaded from cache
        state = INIT;
        scan_next_run = true;
    }

    public synchronized void logFailure() {
        val localMac = transmitterMAC;
        if (localMac == null) {
            UserError.Log.e(TAG, "Could not log failure as mac is null");
            return;
        }
        val tsl = tsl();
        failureTally.put(localMac, tsl);
        UserError.Log.d(TAG, "Adding " + localMac + " to failure tally " + JoH.dateTimeText(tsl));
        resetSomeInternalState();
    }

    public void listenForChangeInSettings(boolean listen) {
        try {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            if (listen) {
                prefs.registerOnSharedPreferenceChangeListener(prefListener);
            } else {
                prefs.unregisterOnSharedPreferenceChangeListener(prefListener);
            }
        } catch (Exception e) {
            UserError.Log.e(TAG, "Error with preference listener: " + e + " " + listen);
        }
    }

    public final SharedPreferences.OnSharedPreferenceChangeListener prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            checkPreferenceKey(key, prefs);
        }
    };


    // remember needs proguard exclusion due to access by reflection
    public static boolean isCollecting() {
        return (state == CONNECT_NOW && msSince(static_last_timestamp) < MINUTE_IN_MS * 30) || msSince(static_last_timestamp) < MINUTE_IN_MS * 6;
    }

    // TODO may want to move this to utility method in the future
    private static boolean isVolumeSilent() {
        final AudioManager am = (AudioManager) xdrip.getAppContext().getSystemService(Context.AUDIO_SERVICE);
        return (am.getRingerMode() != AudioManager.RINGER_MODE_NORMAL);
    }
}
