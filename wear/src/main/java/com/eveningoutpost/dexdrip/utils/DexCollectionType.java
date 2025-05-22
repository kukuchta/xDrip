package com.eveningoutpost.dexdrip.utils;

import com.eveningoutpost.dexdrip.services.DexCollectionService;
import com.eveningoutpost.dexdrip.services.DexShareCollectionService;
import com.eveningoutpost.dexdrip.services.DoNothingService;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.cgm.medtrum.MedtrumCollectionService;
import com.eveningoutpost.dexdrip.cgm.nsfollow.NightscoutFollowService;
import com.eveningoutpost.dexdrip.cgm.sharefollow.ShareFollowService;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Created by andy on 01/06/16.
 */
public enum DexCollectionType {

    None("None"),
    DexcomShare("DexcomShare"),
    DexcomG6("DexcomG6"), // currently pseudo
    Follower("Follower"),
    NSEmulator("NSEmulator"),
    NSFollow("NSFollower"),
    SHFollow("SHFollower"),
    Medtrum("Medtrum"),
    Disabled("Disabled"),
    Manual("Manual");

    String internalName;
    private static final Map<String, DexCollectionType> mapToInternalName;
    private static final HashSet<DexCollectionType> usesBluetooth = new HashSet<>();
    private static final HashSet<DexCollectionType> usesFiltered = new HashSet<>();
    private static final HashSet<DexCollectionType> usesBattery = new HashSet<>();

    public static final String DEX_COLLECTION_METHOD = "dex_collection_method";

    public static boolean does_have_filtered = false; // TODO this could get messy with GC


    static {
        mapToInternalName = new HashMap<>();

        for (DexCollectionType dct : values()) {
            mapToInternalName.put(dct.internalName, dct);
        }

        Collections.addAll(usesBluetooth, DexcomShare, Medtrum);
        Collections.addAll(usesFiltered, Follower); // Bluetooth and Wifi+Bluetooth need dynamic mode
        Collections.addAll(usesBattery, Follower); // parakeet separate
    }


    DexCollectionType(String name) {
        this.internalName = name;
    }


    public static DexCollectionType getType(String dexCollectionType) {

        if (mapToInternalName.containsKey(dexCollectionType))
            return mapToInternalName.get(dexCollectionType);
        else
            return None;
    }

    public static DexCollectionType getDexCollectionType() {
        return getType(Pref.getString(DEX_COLLECTION_METHOD, "None"));
    }

    public static void setDexCollectionType(DexCollectionType t) {
        Pref.setString(DEX_COLLECTION_METHOD, t.internalName);
    }

    public static boolean hasBluetooth() {
        return usesBluetooth.contains(getDexCollectionType());
    }

    public static boolean hasBattery() {
        return usesBattery.contains(getDexCollectionType());
    }

    public static boolean hasFiltered() {
        return does_have_filtered || usesFiltered.contains(getDexCollectionType());
    }

    public static Class<?> getCollectorServiceClass() {
        return getCollectorServiceClass(getDexCollectionType());
    }

    public static Class<?> getCollectorServiceClass(final DexCollectionType type) {
        switch (type) {
            case DexcomShare:
                return DexShareCollectionService.class;
            case Medtrum:
                return MedtrumCollectionService.class;
            case Follower:
                return DoNothingService.class;
            case NSFollow:
                return NightscoutFollowService.class;
            case SHFollow:
                return ShareFollowService.class;
            default:
                return DexCollectionService.class;
        }
    }

    // using reflection to access static methods, could cache if needed maybe

    public static Boolean getServiceRunningState() {
        final Boolean result = getPhoneServiceRunningState();
        // if phone running don't bother checking wear
        if ((result != null) && result) return true;
        return getWatchServiceRunningState();
    }

    public static Boolean getPhoneServiceRunningState() {
        try {
            // TODO handle wear collection
            final Method method = getCollectorServiceClass().getMethod("isRunning");
            return (Boolean) method.invoke(null);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean getLocalServiceCollectingState() {
        try {
            final Method method = getCollectorServiceClass().getMethod("isCollecting");
            return (boolean) method.invoke(null);
        } catch (Exception e) {
            return false; // default to not blocking a restart
        }
    }


    public static Boolean getWatchServiceRunningState() {
        if (Pref.getBooleanDefaultFalse("wear_sync") &&
                Pref.getBooleanDefaultFalse("enable_wearG5")) {
            try {
                final Method method = getCollectorServiceClass().getMethod("isWatchRunning");
                return (Boolean) method.invoke(null);
            } catch (Exception e) {
                return null; // probably method not found
            }
        } else {
            return false; // hopefully this is sufficient to know that the service is definitely not running
        }
    }

    public static String getBestCollectorHardwareName() {
        final DexCollectionType dct = getDexCollectionType();
        switch (dct) {
            case NSEmulator:
                return "Other App";
            case NSFollow:
                return "Nightscout";
            case SHFollow:
                return "Share";

            default:
                return dct.name();
        }
    }

    public static int getBestBridgeBatteryPercent() {
        if (DexCollectionType.hasBattery()) {
            final DexCollectionType dct = getDexCollectionType();
            // TODO this logic needs double checking for multi collector types and others
            switch (dct) {
                default:
                    return Pref.getInt("bridge_battery", -1);
            }
        } else {
            return -2;
        }
    }

    public long getSamplePeriod() {
        return getCollectorSamplePeriod(this);
    }

    public static long getCollectorSamplePeriod(final DexCollectionType type) {
        switch (type) {
            default:
                return 300_000; // 5 minutes
        }
    }

    public static long getCurrentDeduplicationPeriod() {
        final long period = getDexCollectionType().getSamplePeriod();
        return period - (period / 6); // TODO this needs more validation
    }
}
