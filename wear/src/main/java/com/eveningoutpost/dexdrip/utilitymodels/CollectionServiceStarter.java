package com.eveningoutpost.dexdrip.utilitymodels;

//KS import android.app.AlarmManager;
//KS import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.models.UserError.Log;
//KS import com.eveningoutpost.dexdrip.services.DailyIntentService;
import com.eveningoutpost.dexdrip.services.DexCollectionService;
import com.eveningoutpost.dexdrip.services.DexShareCollectionService;
//KS import com.eveningoutpost.dexdrip.services.DoNothingService;
import com.eveningoutpost.dexdrip.services.G5CollectionService;
//KS import com.eveningoutpost.dexdrip.services.SyncService;
//KS import com.eveningoutpost.dexdrip.services.WifiCollectionService;
//KS import com.eveningoutpost.dexdrip.UtilityModels.pebble.PebbleUtil;
//KS import com.eveningoutpost.dexdrip.UtilityModels.pebble.PebbleWatchSync;
import com.eveningoutpost.dexdrip.services.Ob1G5CollectionService;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
//KS import com.eveningoutpost.dexdrip.wearintegration.WatchUpdaterService;
import com.eveningoutpost.dexdrip.xdrip;

import java.io.IOException;
//KS import java.util.Calendar;

/**
 * Created by Emma Black on 12/22/14.
 */
public class CollectionServiceStarter {
    private Context mContext;
    final public static String pref_run_wear_collector = "run_wear_collector";

    private final static String TAG = CollectionServiceStarter.class.getSimpleName();


    public static boolean isBTShare(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String collection_method = prefs.getString("dex_collection_method", "None");
        if (collection_method.compareTo("DexcomShare") == 0) {
            return true;
        }
        return false;
    }

    public static boolean isBTShare(String collection_method) {
        return collection_method.equals("DexcomShare");
    }

    public static boolean isBTG5(Context context) {
        return false;
    }

    public static boolean isBTG5(String collection_method) {
        return false;
    }

    public static boolean isFollower(String collection_method) {
        return collection_method.equals("Follower");
    }

    public void start(Context context, String collection_method) {//KS use ListenerService processConnectG5 / startBtService methods instead
        this.mContext = context;
        xdrip.checkAppContext(context);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.mContext);

        if (isBTShare(collection_method)) {
            Log.d("DexDrip", "Starting bt share collector");
            stopBtWixelService();
            //KS stopFollowerThread();
            //KS stopWifWixelThread();
            stopG5ShareService();

            startBtShareService();

        } else if (isBTG5(collection_method)) {
            Log.d("DexDrip", "Starting G5 share collector");
            stopBtWixelService();
            //KS stopWifWixelThread();
            stopBtShareService();

            if (prefs.getBoolean("wear_sync", false)) {//KS
                boolean enable_wearG5 = prefs.getBoolean("enable_wearG5", false);
                boolean force_wearG5 = prefs.getBoolean("force_wearG5", false);
                //KS this.mContext.startService(new Intent(context, WatchUpdaterService.class));
                if (!enable_wearG5 || (enable_wearG5 && !force_wearG5)) { //don't start if Wear G5 Collector Service is active
                    startBtG5Service();
                }
            }
            else {
                startBtG5Service();
            }
        } else if (isFollower(collection_method)) {
            stopBtShareService();
            stopBtWixelService();
            stopG5ShareService();
        }

        Log.d(TAG, collection_method);

        // Start logging to logcat
        if (prefs.getBoolean("store_logs", false)) {
            String filePath = Environment.getExternalStorageDirectory() + "/xdriplogcat.txt";
            try {
                String[] cmd = {"/system/bin/sh", "-c", "ps | grep logcat  || logcat -f " + filePath +
                        " -v threadtime AlertPlayer:V com.eveningoutpost.dexdrip.services.WixelReader:V *:E "};
                Runtime.getRuntime().exec(cmd);
            } catch (IOException e2) {
                Log.e(TAG, "running logcat failed, is the device rooted?", e2);
            }
        }

    }

    public void start(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String collection_method = prefs.getString("dex_collection_method", "None");

        start(context, collection_method);
    }

    public CollectionServiceStarter(Context context) {
        this.mContext = context;
    }

    public static void restartCollectionServiceBackground() {
        Inevitable.task("restart-collection-service",500,() -> restartCollectionService(xdrip.getAppContext()));
    }


    public static void restartCollectionService(Context context) {
        CollectionServiceStarter collectionServiceStarter = new CollectionServiceStarter(context);
        collectionServiceStarter.stopBtShareService();
        collectionServiceStarter.stopBtWixelService();
        //KS collectionServiceStarter.stopWifWixelThread();
        //KS collectionServiceStarter.stopFollowerThread();
        collectionServiceStarter.stopG5ShareService();
        collectionServiceStarter.start(context);
    }

    public static void startBtService(Context context) {
        Log.d(TAG, "startBtService: " + DexCollectionType.getDexCollectionType());
        CollectionServiceStarter collectionServiceStarter = new CollectionServiceStarter(context);
        collectionServiceStarter.stopBtShareService();
        collectionServiceStarter.stopBtWixelService();
        collectionServiceStarter.stopG5ShareService();
        switch (DexCollectionType.getDexCollectionType()) {
            case DexcomShare:
                collectionServiceStarter.startBtShareService();
                break;
            default:
                collectionServiceStarter.startBtWixelService();
                break;
        }
    }

    public static void stopBtService(Context context) {
        Log.d(TAG, "stopBtService call stopService");
        PersistentStore.setBoolean(pref_run_wear_collector, false);
        CollectionServiceStarter collectionServiceStarter = new CollectionServiceStarter(context);
        collectionServiceStarter.stopBtWixelService();
        collectionServiceStarter.stopBtShareService();
        collectionServiceStarter.stopG5ShareService();
        Log.d(TAG, "stopBtService should have called onDestroy");
    }

    public void startBtWixelService() {//private
        Log.d(TAG, "starting bt wixel service");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Log.d(TAG, "SDK_INT >=JELLY_BEAN_MR2");
            PersistentStore.setBoolean(pref_run_wear_collector, true);
            this.mContext.startService(new Intent(this.mContext, DexCollectionService.class));
            Log.d(TAG, "After startService");
        }
        Log.d(TAG, "exit");
    }

    public void stopBtWixelService() {//private
        Log.d(TAG, "stopping bt wixel service");
        this.mContext.stopService(new Intent(this.mContext, DexCollectionService.class));
    }

    public void startBtShareService() {//private
        Log.d(TAG, "starting bt share service");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            PersistentStore.setBoolean(pref_run_wear_collector, true);
            this.mContext.startService(new Intent(this.mContext, DexShareCollectionService.class));
        }
    }

    public void startBtG5Service() {//private
        Log.d(TAG, "starting G5 service");
        //if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
        PersistentStore.setBoolean(pref_run_wear_collector, true);

        if (!Pref.getBooleanDefaultFalse(Ob1G5CollectionService.OB1G5_PREFS)) {
            G5CollectionService.keep_running = true;
            this.mContext.startService(new Intent(this.mContext, G5CollectionService.class));
        } else {
            Ob1G5CollectionService.keep_running = true;
            this.mContext.startService(new Intent(this.mContext, Ob1G5CollectionService.class));
        }
        //}
    }

    private void stopBtShareService() {
        Log.d(TAG, "stopping bt share service");
        this.mContext.stopService(new Intent(this.mContext, DexShareCollectionService.class));
    }

    private void stopG5ShareService() {
        Log.d(TAG, "stopping G5 service");
        G5CollectionService.keep_running = false; // ensure zombie stays down
        this.mContext.stopService(new Intent(this.mContext, G5CollectionService.class));
        Ob1G5CollectionService.keep_running = false; // ensure zombie stays down
        this.mContext.stopService(new Intent(this.mContext, Ob1G5CollectionService.class));
    }

}
