package com.eveningoutpost.dexdrip.utilitymodels;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import com.activeandroid.annotation.Column;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.NewDataObserver;
import com.eveningoutpost.dexdrip.WidgetUpdateService;
import com.eveningoutpost.dexdrip.xDripWidget;

import java.util.List;

/**
 * Created by Emma Black on 11/7/14.
 */

public class BgSendQueue {

    @Column(name = "bgReading", index = true)
    public BgReading bgReading;

    @Column(name = "success", index = true)
    public boolean success;


    // TODO extract to non depreciated class
    public static void handleNewBgReading(final BgReading bgReading, String operation_type, Context context, boolean quick) {
        if (bgReading == null) {
            UserError.Log.wtf("BgSendQueue", "handleNewBgReading called with null bgReading!");
            return;
        }
        final PowerManager.WakeLock wakeLock = JoH.getWakeLock("sendQueue", 120000);
        try {
            // all this other UI stuff probably shouldn't be here but in lieu of a better method we keep with it..
            if (!quick) {
                if (Home.activityVisible) {
                    context.sendBroadcast(new Intent(Intents.ACTION_NEW_BG_ESTIMATE_NO_DATA));
                }

                if (AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, xDripWidget.class)).length > 0) {
                    //context.startService(new Intent(context, WidgetUpdateService.class));
                    JoH.startService(WidgetUpdateService.class);
                }
            }

            // TODO I don't really think this is needed anymore
            if (!quick && Pref.getBooleanDefaultFalse("excessive_wakelocks")) {
                // just keep it alive for 3 more seconds to allow the watch to be updated
                // dangling wakelock
                JoH.getWakeLock("broadcstNightWatch", 3000);
            }

            if (!quick) {
                NewDataObserver.newBgReading(bgReading);
            }

        } finally {
            JoH.releaseWakeLock(wakeLock);
        }
    }


   /* @Deprecated
    public void markMongoSuccess() {
        this.mongo_success = true;
        save();
    }*/

}
