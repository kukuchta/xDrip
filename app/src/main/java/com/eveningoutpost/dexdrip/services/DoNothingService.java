package com.eveningoutpost.dexdrip.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.text.SpannableString;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.ForegroundServiceStarter;
import com.eveningoutpost.dexdrip.utilitymodels.InstalledApps;
import com.eveningoutpost.dexdrip.utilitymodels.NanoStatus;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.StatusItem;
import com.eveningoutpost.dexdrip.ui.helpers.Span;
import com.eveningoutpost.dexdrip.utils.framework.WakeLockTrampoline;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.ArrayList;
import java.util.List;

import static com.eveningoutpost.dexdrip.models.JoH.msSince;
import static com.eveningoutpost.dexdrip.models.JoH.msTill;
import static com.eveningoutpost.dexdrip.models.JoH.niceTimeScalar;
import static com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.BAD;
import static com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.CRITICAL;
import static com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.GOOD;
import static com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.NOTICE;
import static com.eveningoutpost.dexdrip.xdrip.gs;

import lombok.val;

public class DoNothingService extends Service {
    private final static String TAG = DoNothingService.class.getSimpleName();
    private DoNothingService dexCollectionService;
    private SharedPreferences prefs;
    private ForegroundServiceStarter foregroundServiceStarter;
    public SharedPreferences.OnSharedPreferenceChangeListener prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if (key.compareTo("run_service_in_foreground") == 0) {
                UserError.Log.d("FOREGROUND", "run_service_in_foreground changed!");
                if (prefs.getBoolean("run_service_in_foreground", false)) {
                    foregroundServiceStarter = new ForegroundServiceStarter(getApplicationContext(), dexCollectionService);
                    foregroundServiceStarter.start();
                    UserError.Log.d(TAG, "Moving to foreground");
                } else {
                    dexCollectionService.stopForeground(true);
                    UserError.Log.d(TAG, "Removing from foreground");
                }
            }
        }
    };

    private static long nextWakeUpTime = -1;
    private static long wake_time_difference = 0;
    private static long max_wake_time_difference = 0;
    private static int wakeUpErrors = 0;
    private static String lastState = "Not running";


    public DoNothingService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        foregroundServiceStarter = new ForegroundServiceStarter(getApplicationContext(), this);
        foregroundServiceStarter.start();
        dexCollectionService = this;
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        listenForChangeInSettings();
        UserError.Log.i(TAG, "onCreate: STARTING SERVICE");
    }

    private static final long TOLERABLE_JITTER = 300000;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        PowerManager.WakeLock wl = JoH.getWakeLock("donothing-follower", 60000);
        lastState = "Trying to start " + JoH.hourMinuteString();
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // TODO this block is never used due to min sdk
            stopSelf();
            JoH.releaseWakeLock(wl);
            return START_NOT_STICKY;
        }

        //JoH.persistentBuggySamsungCheck();

        if (nextWakeUpTime > 0) {
            wake_time_difference = JoH.tsl() - nextWakeUpTime;
            if (wake_time_difference > TOLERABLE_JITTER) {
                UserError.Log.e(TAG, "Slow Wake up! time difference in ms: " + wake_time_difference);
                wakeUpErrors = wakeUpErrors + 3;
                max_wake_time_difference = Math.max(max_wake_time_difference, wake_time_difference);

               // if (!JoH.buggy_samsung && JoH.isSamsung()) {
                //    UserError.Log.wtf(TAG, "Enabled wake workaround due to jitter of: " + JoH.niceTimeScalar(wake_time_difference));
               //     JoH.setBuggySamsungEnabled();
               // }

            } else {
                if (wakeUpErrors > 0) wakeUpErrors--;
            }
        }


            stopSelf();
            JoH.releaseWakeLock(wl);
            return START_NOT_STICKY;

  

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        UserError.Log.d(TAG, "onDestroy entered");
        foregroundServiceStarter.stop();
        UserError.Log.i(TAG, "SERVICE STOPPED");
        lastState = "Stopped " + JoH.hourMinuteString();
    }

    public void listenForChangeInSettings() {
        prefs.registerOnSharedPreferenceChangeListener(prefListener);
    }


    // data for MegaStatus
    private static BgReading last_bg;

    public static List<StatusItem> megaStatus() {
        final List<StatusItem> l = new ArrayList<>();

        l.add(new StatusItem("Service State", lastState));

        updateLastBg();
        if (last_bg != null) {
            l.add(new StatusItem("Glucose Data", JoH.niceTimeSince(last_bg.timestamp) + " ago"));
        }

        if (wakeUpErrors > 0) {
            l.add(new StatusItem("Slow Wake up", JoH.niceTimeScalar(wake_time_difference)));
            l.add(new StatusItem("Wake Up Errors", wakeUpErrors));
        }
        if (max_wake_time_difference > 0) {
            l.add(new StatusItem("Slowest Wake up", JoH.niceTimeScalar(max_wake_time_difference)));
        }

        if (JoH.buggy_samsung) {
            l.add(new StatusItem("Buggy handset", "Using workaround", max_wake_time_difference < TOLERABLE_JITTER ? GOOD : BAD));
        }

        if (nextWakeUpTime != -1) {
            l.add(new StatusItem("Next Wake up", JoH.niceTimeTill(nextWakeUpTime)));

        }

        return l;
    }

    private static void updateLastBg() {
        if ((last_bg == null) || JoH.ratelimit("follower-bg-status", 5)) {
            last_bg = BgReading.last();
        }
    }
}