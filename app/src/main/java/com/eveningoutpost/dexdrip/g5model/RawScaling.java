package com.eveningoutpost.dexdrip.g5model;

// jamorham

public class RawScaling {

    public enum DType {
        G5, G6v1, G6v2
    }

    public static double scale(final long raw, final DType version, final boolean filtered) {
        switch (version) {
            case G6v1:
                return raw * 34;
            case G6v2:
                // revised float calculation suggested by tynbendad
                return Float.intBitsToFloat((int) raw) * 35;
            default:
                return raw;
        }
    }

    public static double scale(final long raw, final String transmitter_id, final boolean filtered) {
        return scale(raw, DType.G5, filtered);
    }

}
