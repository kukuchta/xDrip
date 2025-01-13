package com.eveningoutpost.dexdrip;

import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.receiver.InfoContentProvider;
import com.eveningoutpost.dexdrip.utilitymodels.Notifications;
import com.eveningoutpost.dexdrip.ui.LockScreenWallPaper;
import com.eveningoutpost.dexdrip.wearintegration.ExternalStatusService;

import android.os.Build;

/**
 * Created by jamorham on 01/01/2018.
 *
 * Handle triggering data updates on enabled modules
 */

public class NewDataObserver {

    private static final String TAG = "NewDataObserver";

    // TODO after restructuring so that the triggering is organized by data type,
    // TODO move appropriate functions in to their responsible classes

    // when we receive new glucose reading we want to propagate
    public static void newBgReading(BgReading bgReading) {
        Notifications.start();
        InfoContentProvider.ping("bg");
        LockScreenWallPaper.setIfEnabled();
    }

    // when we receive a new external status broadcast
    public static void newExternalStatus(boolean receivedLocally) {

        final String statusLine = ExternalStatusService.getLastStatusLine();
        if (statusLine.length() > 0) {
            InfoContentProvider.ping("status");
        }

    }
}
