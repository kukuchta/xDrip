package com.eveningoutpost.dexdrip.utilitymodels;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.xdrip;

import static com.eveningoutpost.dexdrip.utils.DexCollectionType.SHFollow;
import static com.eveningoutpost.dexdrip.utils.DexCollectionType.CLFollow;
import static com.eveningoutpost.dexdrip.utils.DexCollectionType.getCollectorServiceClass;

/**
 * Created by Emma Black on 12/22/14.
 */

// TODO everything started here must be capable of being foreground if it is started with compat - really everything should be started with compat!

public class CollectionServiceStarter {

    private Context mContext;
    private static final String TAG = CollectionServiceStarter.class.getSimpleName();

    private static final Object lock = new Object();

    private static volatile boolean stopPending;
    private static volatile boolean startPending;


    private static void queueRestart() {
        Log.d(TAG, "queueRestart called");
        if (!operationInProgress()) {
            Log.d(TAG, "Aquiring lock");
            synchronized (lock) {
                if (!operationInProgress()) {
                    if (JoH.ratelimit("collection-queue-restart", 10)) {
                        Log.d(TAG, "before: " + status());
                        stopPending = true;
                        startPending = true;
                        Log.d(TAG, " after: " + status());
                        automata();
                    } else {
                        Log.d(TAG, "Too fast - queueing cooldown task");
                        Inevitable.task("collect-queue-cooldown", 3000, CollectionServiceStarter::queueRestart);
                    }
                } else {
                    Log.d(TAG, "Went busy during lock acquire: " + status());
                }
            }
        } else {
            Log.d(TAG, "Apparent operation already in progress so calling automata instead");
            automata();
        }
    }

    private static boolean operationInProgress() {
        return startPending || stopPending;
    }

    private static String status() {
        return String.format("Stop Pending: %b  Start Pending: %b", stopPending, startPending);
    }

    private static void automata() {
        Inevitable.task("collect-automata", 200, CollectionServiceStarter::processPending);
    }

    private static void processPending() {
        final PowerManager.WakeLock wl = JoH.getWakeLock("collection-processPending", 60000);
        try {
            synchronized (lock) {
                if (operationInProgress()) {
                    // TODO staticify
                    final CollectionServiceStarter starter = new CollectionServiceStarter(xdrip.getAppContext());
                    if (stopPending) {
                        Log.d(TAG, "processPending: Issuing a stop all");
                        starter.stopAll();
                        stopPending = false;
                    }
                    if (startPending) {
                        Log.d(TAG, "processPending: Issuing a start");
                        starter.start();
                        startPending = false;
                    }
                } else {
                    Log.d(TAG, "Called but nothing pending");
                }
            }
        } finally {
            JoH.releaseWakeLock(wl);
        }
    }

    private void stopAll() {
        Log.d(TAG, "stop all");
        JoH.stopService(getCollectorServiceClass(SHFollow));
        JoH.stopService(getCollectorServiceClass(CLFollow));
    }

    private void start(Context context, String collection_method) {
        Log.d(TAG, "start called: " + collection_method);
        this.mContext = context;
        xdrip.checkAppContext(context);

        {
            // TODO newer item startups should be consolidated in to a DexCollectionType has set to avoid duplicating logic
            if (DexCollectionType.getDexCollectionType() == SHFollow
                    || DexCollectionType.getDexCollectionType() == CLFollow) { // TODO make this a set lookup
                Log.d(TAG, "Starting service based on collector lookup");
                startServiceCompat(new Intent(context, DexCollectionType.getCollectorServiceClass()));
            }
        }

        //startSyncService(); // TODO do we need to actually do this here?
        //startDailyIntentService();
        Log.d(TAG, collection_method);
    }

    private void start() {
        start(xdrip.getAppContext(), Pref.getString("dex_collection_method", "BluetoothWixel"));
    }

    // private constructer, use static methods to start
    private CollectionServiceStarter(Context context) {
        if (context == null) context = xdrip.getAppContext();
        this.mContext = context;
    }


    public static void restartCollectionServiceBackground() {
        Log.d(TAG, "restartCollectionServiceBackground Restart no args");
        Inevitable.task("restart-collection-service", 500, CollectionServiceStarter::queueRestart);
    }


    // TODO refactor all calls to this to use the background method above and make this private
    public static void restartCollectionService(Context context) {
        Log.d(TAG, "Restart with context");
        restartCollectionServiceBackground();
    }


    @SuppressWarnings("ConstantConditions")
    private void startServiceCompat(final Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            //    && BuildConfig.targetSDK >= Build.VERSION_CODES.N
                && ForegroundServiceStarter.shouldRunCollectorInForeground()) {
            try {
                Log.d(TAG, String.format("Starting oreo foreground service: %s", intent.getComponent().getClassName()));
            } catch (NullPointerException e) {
                Log.d(TAG, "Null pointer exception in startServiceCompat");
            }
            mContext.startForegroundService(intent);
        } else {
            mContext.startService(intent);
        }
    }

}
