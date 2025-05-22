package com.eveningoutpost.dexdrip.g5model;

// jamorham

import static com.eveningoutpost.dexdrip.models.JoH.emptyString;

import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.google.common.collect.ImmutableSet;

import lombok.val;

public class FirmwareCapability {

    private static final ImmutableSet<String> KNOWN_G6_PLUS_FIRMWARES = ImmutableSet.of("2.4.2.88");

    public static boolean isG6Plus(final String version) {
        return version != null && (KNOWN_G6_PLUS_FIRMWARES.contains(version) || version.startsWith("2.4."));
    }

    public static boolean isFirmwareTemperatureCapable(final String version) {
        return !isG6Plus(version);
    }



    public static boolean isTransmitterModified(final String tx_id) {
        return false;
    }

    public static boolean isTransmitterStandardFirefly(final String tx_id) { // Firefly that has not been modified
        if (!isTransmitterModified(tx_id) && isTransmitterRawIncapable(tx_id)) {
            return true;
        }
        return false;
    }

    public static boolean isDeviceAltOrAlt2(final String tx_id) {
        return false;
    }

    public static boolean isDeviceAlt2(final String tx_id) {
        return false;
    }

    public static boolean isTransmitterG5(final String tx_id) {
        return false;
    }

    public static boolean isTransmitterG6(final String tx_id) {
        return false;
    }

    public static boolean isTransmitterG6Rev2(final String tx_id) {
        return false;
    }

    public static boolean isTransmitterTimeTravelCapable(final String tx_id) {
        return false;
    }

    public static boolean isTransmitterRawCapable(final String tx_id) {
        return false;
    }

    public static boolean isTransmitterRawIncapable(final String tx_id) {
        return false;
    }

    public static boolean isTransmitterPreemptiveRestartCapable(final String tx_id) {
        return false;
    }
}
