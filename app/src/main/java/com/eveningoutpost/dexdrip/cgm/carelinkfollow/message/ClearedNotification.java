package com.eveningoutpost.dexdrip.cgm.carelinkfollow.message;

import java.util.Date;

public class ClearedNotification {

    public static final String NOTIFICATION_TYPE_ALERT = "ALERT";
    public static final String NOTIFICATION_TYPE_ALARM = "ALARM";
    public static final String NOTIFICATION_TYPE_REMINDER = "REMINDER";
    public String referenceGUID;
    public Date dateTime;
    public String type;
    public int faultId;
    public int instanceId;
    public String messageId;
    public int sg;
    public String secondaryTime;
    public boolean pumpDeliverySuspendState;
    public String pnpId;
    public int relativeOffset;
    public Date triggeredDateTime;

}
