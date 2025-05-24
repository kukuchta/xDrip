package com.eveningoutpost.dexdrip.g5model;

// created by jamorham

import android.util.SparseArray;

import com.eveningoutpost.dexdrip.models.UserError;
import com.google.common.collect.ImmutableSet;

import lombok.Getter;

public enum CalibrationState {

    // TODO i18n

    Unknown(0x00, "Unknown"),
    WarmingUp(0x02, "Warming Up"),
    NeedsFirstCalibration(0x04, "Needs Initial Calibration"),
    NeedsSecondCalibration(0x05, "Needs Second Calibration"),
    Ok(0x06, "OK"),
    NeedsCalibration(0x07, "Needs Calibration"),
    InsufficientCalibration(0x0e, "Insufficient Calibration"),
    Errors(0x12, "Sensor Errors"),
    SensorFailed7(0x19, "Sensor Failed 7"); // apparently not a failure state

    @Getter
    byte value;
    @Getter
    String text;


    private static final SparseArray<CalibrationState> lookup = new SparseArray<>();

    CalibrationState(int value, String text) {
        this.value = (byte) value;
        this.text = text;
    }

    static {
        for (CalibrationState state : values()) {
            lookup.put(state.value, state);
        }
    }

    public static CalibrationState parse(byte state) {
        final CalibrationState result = lookup.get(state);
        if (result == null) UserError.Log.e("deleted", "Unknown calibration state: " + state);
        return result != null ? result : Unknown;
    }

    public static CalibrationState parse(int state) {
        return parse((byte) state);
    }

    public boolean usableGlucose() {
        return this == Ok
                || this == NeedsCalibration
                || this == SensorFailed7;
    }

    public boolean ok() {
        return this == Ok;
    }

}
