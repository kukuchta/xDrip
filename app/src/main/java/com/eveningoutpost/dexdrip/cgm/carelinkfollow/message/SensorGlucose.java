package com.eveningoutpost.dexdrip.cgm.carelinkfollow.message;

import java.util.Date;

/**
 * CareLink SensorGlucose message with helper methods for processing
 */
public class SensorGlucose {

    public Integer sg;
    public String datetime;
    public boolean timeChange;
    public String sensorState;
    public String kind;
    public int version;
    public int relativeOffset;

    public Date datetimeAsDate;
    public long timestamp = -1;
    public Date date = null;

    public String toS() {
        String dt;
        if (datetime == null) {
            dt = "";
        } else {
            dt = datetime;
        }
        return dt + " " + sg;
    }

}