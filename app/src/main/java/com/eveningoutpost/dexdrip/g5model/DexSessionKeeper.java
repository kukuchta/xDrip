package com.eveningoutpost.dexdrip.g5model;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;

// jamorham

// track active session time

public class DexSessionKeeper {

    private static final String PREF_SESSION_START = "OB1-SESSION-START";

    public static void setStart(final long when) {
        // TODO sanity check
        PersistentStore.setLong(PREF_SESSION_START, when);
    }

    public static void setStart(final Long when) {
        if (when != null) {
            setStart((long) when);
        }
    }

    public static long getStart() {
        // value 0 == not started
        return PersistentStore.getLong(PREF_SESSION_START);
    }

    public static boolean isStarted() {
        return getStart() != 0;
    }
}
