package com.eveningoutpost.dexdrip.cgm.carelinkfollow.message;

import java.util.Date;

/**
 * CareLink Marker data
 */
public class Marker {

    public static final String TYPE_MEAL = "MEAL";
    //      "type": "MEAL",
    //      "index": 279,
    //      "kind": "Marker",
    //      "version": 1,
    //      "dateTime": "2024-12-11T20:53:56.000-00:00",
    //      "relativeOffset": -2425,
    //      "amount": 8

    public static final String TYPE_INSULIN = "INSULIN";
    public static final String ACTIVATION_TYPE_RECOMMENDED = "RECOMMENDED";
    public static final String ACTIVATION_TYPE_AUTOCORRECTION = "AUTOCORRECTION";
    //      "type": "INSULIN",
    //      "index": 279,
    //      "kind": "Marker",
    //      "version": 1,
    //      "dateTime": "2024-12-11T20:53:57.000-00:00",
    //      "relativeOffset": -2424,
    //      "programmedExtendedAmount": 0,
    //      "activationType": "RECOMMENDED" / "AUTOCORRECTION" / "UNDETERMINED" (740g),
    //      "deliveredExtendedAmount": 0,
    //      "programmedFastAmount": 0.225,
    //      "programmedDuration": 0,
    //      "deliveredFastAmount": 0.225,
    //      "id": 45,
    //      "effectiveDuration": 0,
    //      "completed": true,
    //      "bolusType": "FAST"

    public static final String TYPE_CALIBRATION = "CALIBRATION";
    //      "type": "CALIBRATION",
    //      "index": 263,
    //      "value": 328,
    //      "kind": "Marker",
    //      "version": 1,
    //      "dateTime": "2024-12-11T19:30:00.000-00:00",
    //      "relativeOffset": -7461,
    //      "calibrationSuccess": true

    public static final String TYPE_AUTO_BASAL_DELIVERY = "AUTO_BASAL_DELIVERY";
    //      "type": "AUTO_BASAL_DELIVERY",
    //      "index": 243,
    //      "kind": "Marker",
    //      "version": 1,
    //      "dateTime": "2024-12-11T17:54:11.000-00:00",
    //      "relativeOffset": -13210,
    //      "id": 25,
    //      "bolusAmount": 0.025
    public static final String TYPE_AUTO_MODE_STATUS = "AUTO_MODE_STATUS";
    //      "type": "AUTO_MODE_STATUS",
    //      "index": 0,
    //      "kind": "Marker",
    //      "version": 1,
    //      "dateTime": "2024-12-10T21:34:22.000-00:00",
    //      "relativeOffset": -86399,
    //      "autoModeOn": false
    public static final String TYPE_LOW_GLUCOSE_SUSPENDED = "LOW_GLUCOSE_SUSPENDED";
    //      "type": "LOW_GLUCOSE_SUSPENDED",
    //      "index": 0,
    //      "kind": "Marker",
    //      "version": 1,
    //      "dateTime": "2024-12-10T21:34:22.000-00:00",
    //      "relativeOffset": -86399,
    //      "deliverySuspended": false

    public boolean isMeal() {
        if (type == null)
            return false;
        else
            return (type.equals(TYPE_MEAL));
    }

    public boolean isInsulin() {
        if (type == null || activationType == null)
            return false;
        else
            return (type.equals(TYPE_INSULIN) && !activationType.equals(ACTIVATION_TYPE_AUTOCORRECTION));
    }

    public boolean isAutocorrection() {
        if (type == null || activationType == null)
            return false;
        else
            return (type.equals(TYPE_INSULIN) && activationType.equals(ACTIVATION_TYPE_AUTOCORRECTION));
    }

    public boolean isBloodGlucose() {
        if (type == null)
            return false;
        else
            return (type.equals(TYPE_CALIBRATION));
    }

    public boolean isAutoBasalDelivery() {
        if (type == null)
            return false;
        else
            return (type.equals(TYPE_AUTO_BASAL_DELIVERY));
    }

    public boolean isAutoModeStatus() {
        if (type == null)
            return false;
        else
            return (type.equals(TYPE_AUTO_MODE_STATUS));
    }

    public boolean isLowGlucoseSuspend() {
        if (type == null)
            return false;
        else
            return (type.equals(TYPE_LOW_GLUCOSE_SUSPENDED));
    }

    public String type;
    public int index;
    public Double value;
    public String kind;
    public int version;
    public Date dateTime;
    public int relativeOffset;
    public boolean calibrationSuccess;
    public Double amount;
    public Float programmedExtendedAmount;
    public String activationType;
    public Float deliveredExtendedAmount;
    public Float programmedFastAmount;
    public int programmedDuration;
    public Float deliveredFastAmount;
    public int id;
    public int effectiveDuration;
    public boolean completed;
    public String bolusType;
    public boolean autoModeOn;
    public Float bolusAmount;
    public boolean deliverySuspended;

}