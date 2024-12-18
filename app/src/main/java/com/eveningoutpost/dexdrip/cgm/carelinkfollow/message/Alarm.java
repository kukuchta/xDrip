package com.eveningoutpost.dexdrip.cgm.carelinkfollow.message;

import java.util.Date;

/**
 * CareLink Alarm data
 */
public class Alarm {

    public int code;
    public String datetime;
    public String type;
    public boolean flash;
    public Integer instanceId;
    public String messageId;
    public Integer sg;
    public Boolean pumpDeliverySuspendState;
    public String referenceGUID;
    public int relativeOffset;
    public String kind;
    public long version;

    public Date datetimeAsDate;

}