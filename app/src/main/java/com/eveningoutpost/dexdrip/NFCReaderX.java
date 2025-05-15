package com.eveningoutpost.dexdrip;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.Tag;
import android.nfc.tech.NfcV;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import com.eveningoutpost.dexdrip.importedlibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.models.ActiveBluetoothDevice;
import com.eveningoutpost.dexdrip.models.GlucoseData;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Libre2SensorData;
import com.eveningoutpost.dexdrip.models.LibreBlock;
import com.eveningoutpost.dexdrip.models.LibreOOPAlgorithm;
import com.eveningoutpost.dexdrip.models.ReadingData;
import com.eveningoutpost.dexdrip.models.SensorSanity;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.utilitymodels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.LibreUtils;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;

import com.eveningoutpost.dexdrip.models.LibreOOPAlgorithm.SensorType;
import com.eveningoutpost.dexdrip.utils.LibreTrendUtil;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.eveningoutpost.dexdrip.xdrip.gs;

// From LibreAlarm et al

// TODO have we always checked checksum on this data? what about LibreAlarm path?


public class NFCReaderX {

    private static final String TAG = "NFCReaderX";
    private static final boolean d = false; // global debug flag
    public static boolean used_nfc_successfully = false;
    private static final int MINUTE = 60000;
    private static boolean foreground_enabled = false;
    private static boolean tag_discovered = false;
    private static long last_tag_discovered = -1;
    private static final Object tag_lock = new Object();
    private static final String ENABLE_BLUETOOTH_TIMESTAMP = "enable_bluetooth_timestamp";

    // Constants for libre1/2 FRAM
    final static int FRAM_RECORD_SIZE = 6;
    final static int TREND_START = 28;
    final static int HISTORY_START = 124;

    // Constants for libre pro
    static final int LPRO_SENSORMINUTES = 74;
    static final int LPRO_TRENDPOINTER = 76;
    static final int LPRO_TRENDOFFSET = 80;

    // Never in production. Used to emulate German sensor behavior.
    public static boolean use_fake_de_data() {
        //Pref.setBoolean("use_fake_de_data", true);
        //
        boolean ret = Pref.getBooleanDefaultFalse("use_fake_de_data");
        Log.d(TAG, "using fake data = " + ret);
        return ret;
    }

    @Deprecated
    public static void stopNFC(Activity context) {
        if (foreground_enabled) {
            try {
                NfcAdapter.getDefaultAdapter(context).disableForegroundDispatch(context);
            } catch (Exception e) {
                Log.d(TAG, "Got exception disabling foregrond dispatch");
            }
            foreground_enabled = false;
        }
    }

    public static synchronized void doTheScan(final Activity context, Tag tag, boolean showui) {
        synchronized (tag_lock) {
            if (tag_discovered) {
                Log.d(TAG, "Tag already discovered!");
                if (JoH.tsl() - last_tag_discovered > 30000)
                    tag_discovered = false; // don't lock too long
            }
        } // lock
    }

    public static void sendLibrereadingToFollowers(final String tagId, byte[] data1, final long CaptureDateTime, byte[] patchUid, byte[] patchInfo) {
        if (!Home.get_master()) {
            return;
        }
        LibreBlock libreBlock = LibreBlock.getForTimestamp(CaptureDateTime);
        if (libreBlock != null) {
            // We already have this one, so we have already sent it, so let's not crate storms.
            return;
        }
        // Create the object to send
        libreBlock = LibreBlock.create(tagId, CaptureDateTime, data1, 0, patchUid, patchInfo);
        if (libreBlock == null) {
            Log.e(TAG, "Error could not create libreBlock for libre-allhouse");
            return;
        }
        final String json = libreBlock.toExtendedJson();

        GcmActivity.pushLibreBlock(json);

    }

    public static boolean HandleGoodReading(final String tagId, byte[] data1, final long CaptureDateTime, final boolean allowUpload, byte[] patchUid, byte[] patchInfo) {
        return HandleGoodReading(tagId, data1, CaptureDateTime, allowUpload, patchUid, patchInfo, false, null, null);
    }


    // returns true if checksum passed.
    public static boolean HandleGoodReading(final String tagId, byte[] data1, final long CaptureDateTime, final boolean allowUpload, byte[] patchUid, byte[] patchInfo,
                                            boolean decripted_data, int[] trend_bg_vals, int[] history_bg_vals) {
        Log.e(TAG, "HandleGoodReading called dat1 len = " + data1.length);
        if (data1.length > Constants.LIBRE_1_2_FRAM_SIZE) {
            // It seems that some times we read a buffer that is bigger than 0x158, but we should only use the first 0x158 bytes.
            data1 = java.util.Arrays.copyOfRange(data1, 0, Constants.LIBRE_1_2_FRAM_SIZE);
        }

        if (LibreOOPAlgorithm.isDecodeableData(patchInfo) && decripted_data == false
                && !Pref.getBooleanDefaultFalse("external_blukon_algorithm")) {
            // Send to OOP2 for decryption.
            LibreOOPAlgorithm.logIfOOP2NotAlive();
            LibreOOPAlgorithm.sendData(data1, CaptureDateTime, patchUid, patchInfo, tagId);
            return true;
        }

        sendLibrereadingToFollowers(tagId, data1, CaptureDateTime, patchUid, patchInfo);

        if (Pref.getBooleanDefaultFalse("external_blukon_algorithm")) {
            // If oop is used, there is no need to  do the checksum It will be done by the oop.
            // (or actually we don't know how to do it, for us 14/de sensors).
            // Save raw block record (we start from block 0)
            LibreBlock.createAndSave(tagId, CaptureDateTime, data1, 0, allowUpload, patchUid, patchInfo);
            LibreOOPAlgorithm.sendData(data1, CaptureDateTime, patchUid, patchInfo, tagId);
        } else {
            final boolean checksum_ok = LibreUtils.verify(data1, patchInfo);
            if (!checksum_ok) {
                Log.e(TAG, "bad cs");
                return false;
            }

            // The 4'th byte is where the sensor status is (for libre1 libre2 and libre pro).
            if (!LibreUtils.isSensorReady(data1[4])) {
                Log.e(TAG, "Sensor is not ready, Ignoring reading!");
                return true;
            }

        }
        return true; // Checksum tests have passed.
    }

    public static void enableBluetoothAskUser(Activity context) {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        LayoutInflater inflater = context.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.activity_enable_bluetooth, null);

        dialogBuilder.setView(dialogView);
        final AlertDialog show = dialogBuilder.show();

        final CheckBox cbx = (CheckBox) dialogView.findViewById(R.id.enable_streaming_dont_ask_again);

        Button enableStreamingYesButton = (Button) dialogView.findViewById(R.id.enable_streaming_yes);
        enableStreamingYesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                if (cbx.isChecked()) {
                    prefs.edit().putString("libre2_enable_bluetooth_streaming", "enable_streaming_always").apply();
                }
                Pref.setLong(ENABLE_BLUETOOTH_TIMESTAMP, JoH.tsl());
                JoH.clearRatelimit("nfc-debounce");
                show.dismiss();
            }
        });

        Button enableStreamingNoButton = (Button) dialogView.findViewById(R.id.enable_streaming_no);
        enableStreamingNoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                if (cbx.isChecked()) {
                    prefs.edit().putString("libre2_enable_bluetooth_streaming", "enable_streaming_never").apply();
                }
                Pref.setLong(ENABLE_BLUETOOTH_TIMESTAMP, 0);
                show.dismiss();
            }
        });

    }

    public static boolean verifyTime(long time, String caller, byte[] extra_data) {
        if ((time < 0) || time >= LibreTrendUtil.MAX_POINTS) {
            // This is an illegal value
            Log.e(TAG, "We have an illegal time at " + caller + " " + time + JoH.bytesToHex(extra_data));
            return false;
        }
        return true;
    }


    // Get the history data for libre1/2
    private static ArrayList<GlucoseData> parseHistoryData(byte[] data, int sensorTime, long sensorStartTime, Long CaptureDateTime) {
        ArrayList<GlucoseData> historyList = new ArrayList<>();
        int indexHistory = data[27] & 0xFF;
        // loads history values (ring buffer, starting at index_trent. byte 124-315)
        for (int index = 0; index < 32; index++) {
            int i = indexHistory - index - 1;
            if (i < 0) i += 32;
            GlucoseData glucoseData = new GlucoseData();

            // If the data is decoded for some reason, we might have a wrong index.
            // The 6 is because we read up to 6 bytes.
            if (i * FRAM_RECORD_SIZE + HISTORY_START + FRAM_RECORD_SIZE >= data.length) {
                Log.e(TAG, "Failing to parse data from " + JoH.dateTimeText(CaptureDateTime));
                return null;
            }

            glucoseData.glucoseLevelRaw =
                    getGlucoseRaw(new byte[]{data[(i * FRAM_RECORD_SIZE + HISTORY_START + 1)], data[(i * FRAM_RECORD_SIZE + HISTORY_START)]});
            glucoseData.flags = LibreOOPAlgorithm.readBits(data, i * FRAM_RECORD_SIZE + HISTORY_START, 0xe, 0xc);
            glucoseData.temp = LibreOOPAlgorithm.readBits(data, i * FRAM_RECORD_SIZE + HISTORY_START, 0x1a, 0xc);
            glucoseData.source = GlucoseData.DataSource.FRAM;

            int time = Math.max(0, Math.abs((sensorTime - 3) / 15) * 15 - index * 15);

            glucoseData.realDate = sensorStartTime + time * MINUTE;
            glucoseData.sensorTime = time;
            if (verifyTime(time, "parseData history", data)) {
                historyList.add(glucoseData);
            }
        }
        return historyList;
    }

    // Get the trend data for libre1/2
    private static ArrayList<GlucoseData> parseTrendData(byte[] data, int sensorTime, long sensorStartTime, Long CaptureDateTime) {
        ArrayList<GlucoseData> trendList = new ArrayList<>();
        int indexTrend = data[26] & 0xFF;

        // loads trend values (ring buffer, starting at index_trent. byte 28-123)
        for (int index = 0; index < 16; index++) {
            int i = indexTrend - index - 1;
            if (i < 0) i += 16;
            GlucoseData glucoseData = new GlucoseData();
            if (i * FRAM_RECORD_SIZE + TREND_START + FRAM_RECORD_SIZE >= data.length) {
                Log.e(TAG, "Failing to parse data from " + JoH.dateTimeText(CaptureDateTime));
                return null;
            }
            glucoseData.glucoseLevelRaw =
                    getGlucoseRaw(new byte[]{data[(i * FRAM_RECORD_SIZE + TREND_START + 1)], data[(i * FRAM_RECORD_SIZE + TREND_START)]});
            glucoseData.flags = LibreOOPAlgorithm.readBits(data, i * FRAM_RECORD_SIZE + TREND_START, 0xe, 0xc);
            glucoseData.temp = LibreOOPAlgorithm.readBits(data, i * FRAM_RECORD_SIZE + TREND_START, 0x1a, 0xc);
            glucoseData.source = GlucoseData.DataSource.FRAM;
            int time = Math.max(0, sensorTime - index);

            glucoseData.realDate = sensorStartTime + time * MINUTE;
            glucoseData.sensorTime = time;
            if (verifyTime(time, "parseData trendList", data)) {
                trendList.add(glucoseData);
            }
        }
        return trendList;
    }


    private static ArrayList<GlucoseData> parseTrendDataLibrePro(byte[] data, int sensorTime, long sensorStartTime, Long CaptureDateTime) {
        int sensorMinutesElapse = 256 * (data[LPRO_SENSORMINUTES + 1] & 0xFF) + (data[LPRO_SENSORMINUTES] & 0xFF);

        byte trendPointer = data[LPRO_TRENDPOINTER];

        if (trendPointer == 0)
            trendPointer = 0x0F;
        else
            trendPointer -= 1;
        int trendPos = LPRO_TRENDOFFSET + trendPointer * 6;

        ArrayList<GlucoseData> trendList = new ArrayList<>();
        GlucoseData glucoseData = new GlucoseData();

        glucoseData.glucoseLevelRaw =
                getGlucoseRaw(new byte[]{data[trendPos + 1], data[trendPos]});
        glucoseData.flags = 0;//???
        glucoseData.temp = 0;//????
        glucoseData.source = GlucoseData.DataSource.FRAM;

        glucoseData.realDate = CaptureDateTime;
        glucoseData.sensorTime = sensorMinutesElapse;
        if (verifyTime(sensorMinutesElapse, "parseData trendList", data)) {
            trendList.add(glucoseData);
        }
        Log.e(TAG, "Creating librepro data  " + glucoseData);
        return trendList;
    }

    // Sensor structure is described at  https://github.com/UPetersen/LibreMonitor/wiki
    public static ReadingData parseData(byte[] data, byte[] patchInfo, Long CaptureDateTime, int[] trend_bg_vals, int[] history_bg_vals) {
        final int sensorTime = 256 * (data[317] & 0xFF) + (data[316] & 0xFF);
        LibreOOPAlgorithm.SensorType sensorType = LibreOOPAlgorithm.getSensorType(patchInfo);

        long sensorStartTime = CaptureDateTime - sensorTime * MINUTE;

        ArrayList<GlucoseData> historyList;
        if (sensorType != LibreOOPAlgorithm.SensorType.LibreProH) {
            historyList = parseHistoryData(data, sensorTime, sensorStartTime, CaptureDateTime);
        } else {
            historyList = new ArrayList<GlucoseData>();
        }

        ArrayList<GlucoseData> trendList;
        if (sensorType != LibreOOPAlgorithm.SensorType.LibreProH) {
            trendList = parseTrendData(data, sensorTime, sensorStartTime, CaptureDateTime);
        } else {
            trendList = parseTrendDataLibrePro(data, sensorTime, sensorStartTime, CaptureDateTime);
        }
        if(trendList == null || historyList == null) {
            Log.e(TAG,"Failed parsing trendList or historyList");
            return null;
        }
        Collections.sort(trendList);
        Collections.sort(historyList);
        // Adding the bg vals must be done after the sort.
        if (trend_bg_vals != null && trend_bg_vals.length == trendList.size() && history_bg_vals != null
                && history_bg_vals.length == historyList.size()) {
            for (int i = 0; i < trend_bg_vals.length; i++) {
                trendList.get(i).glucoseLevel = trend_bg_vals[i];
                Log.e(TAG, "Adding bg val for trend at time " + trendList.get(i).sensorTime + " val =  " + trend_bg_vals[i]);
            }
            for (int i = 0; i < history_bg_vals.length; i++) {
                historyList.get(i).glucoseLevel = history_bg_vals[i];
                Log.e(TAG, "Adding bg val for history at time " + historyList.get(i).sensorTime + " val =  " + history_bg_vals[i]);
            }
        }

        final ReadingData readingData = new ReadingData(trendList, historyList);
        readingData.raw_data = data;
        return readingData;
    }


    private static int getGlucoseRaw(byte[] bytes) {
        return ((256 * (bytes[0] & 0xFF) + (bytes[1] & 0xFF)) & 0x1FFF);
    }

    public static void vibrate(Context context, int pattern) {

        // 0 = scanning
        // 1 = scan ok
        // 2 = warning
        // 3 = lesser error

        final long[][] patterns = {{0, 150}, {0, 150, 70, 150}, {0, 2000}, {0, 1000}, {0, 100}};

        if (Pref.getBooleanDefaultFalse("nfc_scan_vibrate")) {
            final Vibrator vibrate = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if ((vibrate == null) || (!vibrate.hasVibrator())) return;
            vibrate.cancel();
            if (d) Log.d(TAG, "About to vibrate, pattern: " + pattern);
            try {
                vibrate.vibrate(patterns[pattern], -1);
            } catch (Exception e) {
                Log.d(TAG, "Exception in vibrate: " + e);
            }
        }
    }

    public static void handleHomeScreenScanPreference(Context context) {
        handleHomeScreenScanPreference(context, false);
    }

    public static void handleHomeScreenScanPreference(Context context, boolean state) {
        try {
            Log.d(TAG, "HomeScreen Scan State: " + (state ? "enable" : "disable"));
            context.getPackageManager().setComponentEnabledSetting(new ComponentName(context, NFCFilterX.class),
                    state ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
        } catch (Exception e) {
            Log.wtf(TAG, "Exception in handleHomeScreenScanPreference: " + e);
        }
    }

    public static synchronized void scanFromActivity(final Activity context, final Intent intent) {
        context.finish();
    }

    public static void windowFocusChange(final Activity context, boolean hasFocus, View decorView) {
        if (hasFocus) {
            if (Build.VERSION.SDK_INT >= 19) {
                decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            } else {
                decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN);
            }
        } else {
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        context.finish();
                    } catch (Exception e) {
                        //
                    }
                }
            }, 1000);
        }
    }

    static public List<GlucoseData> getLibreTrend(LibreBlock libreBlock) {
        if (libreBlock.byte_start != 0) {
            Log.i(TAG, "libreBlock does not start with 0, don't know how to parse it " + libreBlock.timestamp);
            return null;
        }
        List<GlucoseData> result;
        if (libreBlock.byte_end == Constants.LIBRE_1_2_FRAM_SIZE) {
            ReadingData reading_data = parseData(libreBlock.blockbytes, libreBlock.patchInfo, libreBlock.timestamp, null, null);
            if (reading_data == null) {
                return null;
            }
            result = reading_data.trend;
        } else if (libreBlock.byte_end == 44) {
            // This is the libre2 ble data
            result = LibreOOPAlgorithm.parseBleDataPerMinute(libreBlock.blockbytes, null, libreBlock.timestamp);
        } else {
            Log.i(TAG, "libreBlock exists but size is " + libreBlock.byte_end + " don't know how to parse it " + libreBlock.timestamp);
            return null;
        }
        if (result.size() == 0) {
            Log.i(TAG, "libreBlock exists but no trend data exists, or first value is zero " + libreBlock.timestamp);
            return null;
        }
        return result;
    }
}
