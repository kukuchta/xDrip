package com.eveningoutpost.dexdrip.g5model;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;

import lombok.val;

/**
 * Created by jamorham on 25/11/2016.
 */

public class DexTimeKeeper {
    private static final String TAG = DexTimeKeeper.class.getSimpleName();

    private static final String DEX_XMIT_START = "DEX_XMIT_START-";

    private static String lastTransmitterId = null;

    public static long fromDexTimeCached(int dexTimeStamp) {
        return fromDexTime(lastTransmitterId, dexTimeStamp);
    }

    public static long fromDexTime(String transmitterId, int dexTimeStamp) {
        if ((transmitterId == null) || (transmitterId.length() != 6 && transmitterId.length() != 4)) {
            UserError.Log.e(TAG, "Invalid dex transmitter in fromDexTime: " + transmitterId);
            return -3;
        }
        lastTransmitterId = transmitterId;
        final long transmitter_start_timestamp = PersistentStore.getLong(DEX_XMIT_START + transmitterId);
        if (transmitter_start_timestamp > 0) {
            return transmitter_start_timestamp + (((long) dexTimeStamp) * 1000L);
        } else {
            return -1;
        }

    }
}
