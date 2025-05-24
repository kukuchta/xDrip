package com.eveningoutpost.dexdrip.cgm.dex;

import static com.eveningoutpost.dexdrip.utilitymodels.Constants.HOUR_IN_MS;

import com.eveningoutpost.dexdrip.g5model.BackFillStream;
import com.eveningoutpost.dexdrip.g5model.DexTimeKeeper;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.cgm.dex.g7.BackfillControlRx;
import com.eveningoutpost.dexdrip.cgm.dex.g7.EGlucoseRxMessage;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;

import lombok.val;

/**
 * JamOrHam
 */

public class ClassifierAction {

    private static final String TAG = ClassifierAction.class.getSimpleName();
    static final String CONNECT = "connect";
    static final String BACKFILL = "backfill";
    static final String CONTROL = "control";
    static final String TXID = "UIUIUI";
    public static volatile long lastReadingTimestamp;
    static final BackFillStream stream = new BackFillStream();

    public static void action(final String type, final byte[] data) {

        if (data == null || data.length == 0) return;
        UserError.Log.d(TAG, "Type: " + type + " hex: " + JoH.bytesToHex(data));
        switch (type) {

            case CONNECT:
                UserError.Log.d(TAG, "Connect");
                stream.reset();
                break;

            case BACKFILL:
                stream.pushNew(data);
                UserError.Log.d(TAG, "Added backfill cache: " + JoH.bytesToHex(data));
                break;



        }
    }

    private static void processBackfill() {
        UserError.Log.d(TAG, "Processing backfill");
        val decoded = stream.decode();
        stream.reset();
        for (BackFillStream.Backsie backsie : decoded) {
            UserError.Log.d(TAG, "Backsie: " + backsie.getDextime());
            val time = DexTimeKeeper.fromDexTime(TXID, backsie.getDextime());
            val since = JoH.msSince(time);
            if ((since > HOUR_IN_MS * 12) || (since < 0)) {
                UserError.Log.wtf(TAG, "Backfill timestamp unrealistic: " + JoH.dateTimeText(time) + " (ignored)");
            } else {
                if (BgReading.getForPreciseTimestamp(time, DexCollectionType.getCurrentDeduplicationPeriod()) == null) {
                    final BgReading bgr = BgReading.bgReadingInsertFromG5(backsie.getGlucose(), time, "Backfill");
                    UserError.Log.d(TAG, "Adding backfilled reading: " + JoH.dateTimeText(time) + " " + BgGraphBuilder.unitized_string_static(backsie.getGlucose()));
                }
                UserError.Log.d(TAG, "Backsie: " + JoH.dateTimeText(time) + " " + BgGraphBuilder.unitized_string_static(backsie.getGlucose()));
            }
        }
    }

}
