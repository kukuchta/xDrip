package com.eveningoutpost.dexdrip.cgm.carelinkfollow.message;

import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.util.CareLinkJsonAdapter;
import com.google.gson.annotations.JsonAdapter;

import java.util.Date;

/**
 * CareLink Marker data
 */
public class Marker {

    public static final String TYPE_MEAL = "MEAL";
    public static final String TYPE_CALIBRATION = "CALIBRATION";
    public static final String TYPE_BG_READING = "BG_READING";
    public static final String TYPE_BG = "BG";
    public static final String TYPE_INSULIN = "INSULIN";
    public static final String TYPE_AUTO_BASAL_DELIVERY = "AUTO_BASAL_DELIVERY";
    public static final String TYPE_AUTO_MODE_STATUS = "AUTO_MODE_STATUS";
    public static final String ACTIVATION_TYPE_AUTOCORRECTION = "AUTOCORRECTION";
    public static final String ACTIVATION_TYPE_BOLUS = "RECOMMENDED";

    public boolean isBloodGlucose() {
        if (type == null)
            return false;
        else
            return (type.equals(TYPE_BG_READING) || type.equals(TYPE_CALIBRATION) || type.equals(TYPE_BG));
    }

    public boolean isAutoBasalDelivery() {
        if (type == null)
            return false;
        else
            return (type.equals(TYPE_AUTO_BASAL_DELIVERY));
    }

    public String type;
    public int index;
    public Double value;
    public String kind;
    public int version;
    public Date dateTime;
    @JsonAdapter(CareLinkJsonAdapter.class)
    public Date timestamp = null;
    public Integer relativeOffset;
    public Boolean calibrationSuccess;
    public Double amount;
    public Float programmedExtendedAmount;
    public String activationType;
    public Float deliveredExtendedAmount;
    public Float programmedFastAmount;
    public Integer programmedDuration;
    public Float deliveredFastAmount;
    public Integer effectiveDuration;
    public Boolean completed;
    public String bolusType;
    public Boolean autoModeOn;
    public Float bolusAmount;
    public MarkerData data;

    public Date getDate(){
        if(timestamp != null)
            return timestamp;
        else if(dateTime != null)
            return dateTime;
        else
            return null;
    }

    public Float getInsulinAmount(){
        if(deliveredExtendedAmount != null && deliveredFastAmount != null)
            return deliveredExtendedAmount + deliveredFastAmount;
        else if(data != null && data.dataValues != null && data.dataValues.deliveredFastAmount != null) { // TODO: get also the extended amount
            return data.dataValues.deliveredFastAmount;
        }
        else {
            return null;
        }
    }

    public String getBolusType(){
        if(data != null && data.dataValues != null && data.dataValues.activationType != null)
            return data.dataValues.activationType;
        else
            return null;
    }

    public Double getCarbAmount(){
        if(amount != null)
            return amount;
        else if(data != null && data.dataValues != null && data.dataValues.amount != null)
            return data.dataValues.amount;
        else
            return null;
    }

    public Double getBloodGlucose()
    {
        if(value != null)
            return value;
        else if(data != null && data.dataValues != null && data.dataValues.unitValue != null)
            return data.dataValues.unitValue;
        else
            return null;
    }

    public Float getBolusAmount()
    {
        if(bolusAmount != null)
            return bolusAmount;
        else if(data != null && data.dataValues != null && data.dataValues.bolusAmount != null)
            return data.dataValues.bolusAmount;
        else
            return null;
    }
}