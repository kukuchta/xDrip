package com.eveningoutpost.dexdrip.cgm.carelinkfollow.message;

import androidx.annotation.DrawableRes;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.models.Notifications;

import java.util.HashMap;

public class TextMap {

    public static class NotificationMapEntry {
        String message = null;
        int id;

        NotificationMapEntry(String message, @DrawableRes int id) {
            this.message = message;
            this.id = id;
        }

        public String getMessage() {
            return message;
        }

        public int getImage() {
            return id;
        }

    }
    public static final String ERROR_TEXT_PREFIX_NGP = "N";

    private static HashMap<String, String> errorTextMap;
    private static HashMap<String, NotificationMapEntry> notificationMap;
    private static HashMap<String, String> errorCodeMap;

    static {

        notificationMap = new HashMap<>();
        notificationMap.put("", new NotificationMapEntry("", R.drawable.empty));
        notificationMap.put("3", new NotificationMapEntry("Battery out limit", R.drawable.battery_yellow));
        notificationMap.put("4", new NotificationMapEntry("Delivery stopped. Check BG", R.drawable.warning_yellow));
        notificationMap.put("5", new NotificationMapEntry("Pump battery depleted. Insulin delivery stopped", R.drawable.warning_yellow));
        notificationMap.put("6", new NotificationMapEntry("Auto Off. Insulin delivery stopped", R.drawable.warning_yellow));
        notificationMap.put("16", new NotificationMapEntry("Pump reset. Insulin delivery stopped", R.drawable.warning_yellow));
        notificationMap.put("43", new NotificationMapEntry("Pump motor error. Insulin delivery stopped", R.drawable.warning_yellow));
        notificationMap.put("50", new NotificationMapEntry("Bolus stopped", R.drawable.warning_yellow));
        notificationMap.put("51", new NotificationMapEntry("Delivery limit exceeded. Check BG", R.drawable.warning_yellow));
        notificationMap.put("55", new NotificationMapEntry("Pump battery failed. Replace battery", R.drawable.down_yellow));
        notificationMap.put("59", new NotificationMapEntry("Button error", R.drawable.down_yellow));
        notificationMap.put("61", new NotificationMapEntry("Check settings. Insulin delivery stopped", R.drawable.down_yellow));
        notificationMap.put("62", new NotificationMapEntry("Empty reservoir", R.drawable.down_yellow));
        notificationMap.put("66", new NotificationMapEntry("No reservoir", R.drawable.down_yellow));
        notificationMap.put("74", new NotificationMapEntry("Finish loading", R.drawable.down_yellow));
        notificationMap.put("81", new NotificationMapEntry("Replace pump battery now", R.drawable.down_yellow));
        notificationMap.put("82", new NotificationMapEntry("Low Reservoir", R.drawable.down_yellow));
        notificationMap.put("83", new NotificationMapEntry("Check BG", R.drawable.down_yellow));
        notificationMap.put("84", new NotificationMapEntry("Alarm clock", R.drawable.down_yellow));
        notificationMap.put("85", new NotificationMapEntry("Max fill reached", R.drawable.down_yellow));
        notificationMap.put("86", new NotificationMapEntry("Weak battery detected", R.drawable.down_yellow));
        notificationMap.put("87", new NotificationMapEntry("Missed bolus", R.drawable.down_yellow));
        notificationMap.put("88", new NotificationMapEntry("Silenced sensor alert. Check alarm history", R.drawable.down_yellow));
        notificationMap.put("101", new NotificationMapEntry("High SG. CHECK BG", R.drawable.down_yellow));
        notificationMap.put("102", new NotificationMapEntry("Low SG", R.drawable.down_yellow));
        notificationMap.put("103", new NotificationMapEntry("Threshold Suspend", R.drawable.down_yellow));
        notificationMap.put("104", new NotificationMapEntry("Meter BG now", R.drawable.down_yellow));
        notificationMap.put("105", new NotificationMapEntry("Calibration Reminder", R.drawable.down_yellow));
        notificationMap.put("106", new NotificationMapEntry("Calibration error", R.drawable.down_yellow));
        notificationMap.put("107", new NotificationMapEntry("Sensor expired", R.drawable.down_yellow));
        notificationMap.put("108", new NotificationMapEntry("Change sensor", R.drawable.down_yellow));
        notificationMap.put("109", new NotificationMapEntry("Sensor error", R.drawable.down_yellow));
        notificationMap.put("110", new NotificationMapEntry("Recharge transmitter", R.drawable.down_yellow));
        notificationMap.put("111", new NotificationMapEntry("Transmitter battery low", R.drawable.down_yellow));
        notificationMap.put("112", new NotificationMapEntry("Weak signal", R.drawable.down_yellow));
        notificationMap.put("113", new NotificationMapEntry("Lost sensor", R.drawable.down_yellow));
        notificationMap.put("114", new NotificationMapEntry("Sensor glucose approaching high limit", R.drawable.down_yellow));
        notificationMap.put("115", new NotificationMapEntry("Sensor glucose approaching low limit", R.drawable.down_yellow));
        notificationMap.put("116", new NotificationMapEntry("Sensor glucose rising rapidly", R.drawable.down_yellow));
        notificationMap.put("117", new NotificationMapEntry("Sensor glucose falling rapidly", R.drawable.down_yellow));
        notificationMap.put("Axx", new NotificationMapEntry("Pump error Anull", R.drawable.down_yellow));
        notificationMap.put("Exx", new NotificationMapEntry("Pump error Enull", R.drawable.down_yellow));
        notificationMap.put("N002", new NotificationMapEntry("Pump Error. Delivery Stopped", R.drawable.down_yellow));
        notificationMap.put("N006", new NotificationMapEntry("Pump Battery Out Limit", R.drawable.down_yellow));
        notificationMap.put("N007", new NotificationMapEntry("Delivery Stopped. Check BG", R.drawable.down_yellow));
        notificationMap.put("N011", new NotificationMapEntry("Replace Pump Battery Now", R.drawable.down_yellow));
        notificationMap.put("N012", new NotificationMapEntry("Auto Suspend Limit Reached. Delivery Stopped", R.drawable.down_yellow));
        notificationMap.put("N024", new NotificationMapEntry("Critical Pump Error. Stop Pump Use. Use Other Treatment", R.drawable.down_yellow));
        notificationMap.put("N025", new NotificationMapEntry("Pump Power Error. Record Settings", R.drawable.down_yellow));
        notificationMap.put("N029", new NotificationMapEntry("Pump Restarted. Delivery Stopped", R.drawable.down_yellow));
        notificationMap.put("N037", new NotificationMapEntry("Pump Motor Error. Delivery Stopped", R.drawable.down_yellow));
        notificationMap.put("N051", new NotificationMapEntry("Bolus Stopped", R.drawable.down_yellow));
        notificationMap.put("N052", new NotificationMapEntry("Delivery Limit Exceeded. Check BG", R.drawable.down_yellow));
        notificationMap.put("N057", new NotificationMapEntry("Pump Battery Not Compatible", R.drawable.down_yellow));
        notificationMap.put("N058", new NotificationMapEntry("Insert A New AA Battery", R.drawable.down_yellow));
        notificationMap.put("N061", new NotificationMapEntry("Pump Button Error. Delivery Stopped", R.drawable.down_yellow));
        notificationMap.put("N062", new NotificationMapEntry("New Notification Received From Pump", R.drawable.down_yellow));
        notificationMap.put("N066", new NotificationMapEntry("No Reservoir Detected During Infusion Set Change", R.drawable.down_yellow));
        notificationMap.put("N069", new NotificationMapEntry("Loading Incomplete During Infusion Set Change", R.drawable.down_yellow));
        notificationMap.put("N073", new NotificationMapEntry("Replace Pump Battery Now", R.drawable.down_yellow));
        notificationMap.put("N077", new NotificationMapEntry("Pump Settings Error. Delivery Stopped", R.drawable.down_yellow));
        notificationMap.put("N084", new NotificationMapEntry("Pump Battery Removed. Replace Battery", R.drawable.down_yellow));
        notificationMap.put("N100", new NotificationMapEntry("Bolus Entry Timed Out Before Delivery", R.drawable.down_yellow));
        notificationMap.put("N103", new NotificationMapEntry("BG Check Reminder", R.drawable.down_yellow));
        notificationMap.put("N104", new NotificationMapEntry("Replace Pump Battery Soon", R.drawable.down_yellow));
        notificationMap.put("N105", new NotificationMapEntry("Reservoir Low. Change Reservoir Soon", R.drawable.down_yellow));
        notificationMap.put("N107", new NotificationMapEntry("Missed Meal Bolus Reminder", R.drawable.down_yellow));
        notificationMap.put("N109", new NotificationMapEntry("Set Change Reminder", R.drawable.down_yellow));
        notificationMap.put("N110", new NotificationMapEntry("Silenced Sensor Alert. Check Alarm History", R.drawable.down_yellow));
        notificationMap.put("N113", new NotificationMapEntry("Reservoir Empty. Change Reservoir Now", R.drawable.down_yellow));
        notificationMap.put("N117", new NotificationMapEntry("Active Insulin Cleared", R.drawable.down_yellow));
        notificationMap.put("N130", new NotificationMapEntry("Rewind Required. Delivery Stopped", R.drawable.down_yellow));
        notificationMap.put("N140", new NotificationMapEntry("Delivery Suspended. Connect Infusion Set", R.drawable.down_yellow));
        notificationMap.put("N775", new NotificationMapEntry("Calibrate Now", R.drawable.down_yellow));
        notificationMap.put("N776", new NotificationMapEntry("Calibration Error", R.drawable.down_yellow));
        notificationMap.put("N777", new NotificationMapEntry("Change Sensor", R.drawable.down_yellow));
        notificationMap.put("N779", new NotificationMapEntry("Recharge Transmitter Now", R.drawable.down_yellow));
        notificationMap.put("N780", new NotificationMapEntry("Lost Sensor Signal", R.drawable.down_yellow));
        notificationMap.put("N784", new NotificationMapEntry("SG Rising Rapidly", R.drawable.up2_yellow)); // Rapid rise
        notificationMap.put("N794", new NotificationMapEntry("Sensor Expired. Change Sensor", R.drawable.down_yellow));
        notificationMap.put("N795", new NotificationMapEntry("Lost Sensor Signal. Check Transmitter", R.drawable.down_yellow));
        notificationMap.put("N796", new NotificationMapEntry("No Sensor Signal", R.drawable.down_yellow));
        notificationMap.put("N797", new NotificationMapEntry("Sensor Connected", R.drawable.down_yellow));
        notificationMap.put("N801", new NotificationMapEntry("Do Not Calibrate. Wait Up To 3 Hours", R.drawable.down_yellow));
        notificationMap.put("N802", new NotificationMapEntry("Low Sensor Glucose", R.drawable.down3_red)); // Below low limit
        notificationMap.put("N803", new NotificationMapEntry("Low Sensor Glucose. Check BG", R.drawable.down_yellow));
        notificationMap.put("N805", new NotificationMapEntry("Alert Before Low. Check BG", R.drawable.down_yellow)); // Before low, not stopping delivery
        notificationMap.put("N807", new NotificationMapEntry("Basal Delivery Resumed. Check BG", R.drawable.down_yellow));
        notificationMap.put("N809", new NotificationMapEntry("Suspend On Low. Delivery Stopped. Check BG", R.drawable.down_yellow));
        notificationMap.put("N810", new NotificationMapEntry("Suspend Before Low. Delivery Stopped. Check BG", R.drawable.down_yellow)); // Before low, stopping delivery
        notificationMap.put("N812", new NotificationMapEntry("Call Emergency Assistance", R.drawable.down_yellow));
        notificationMap.put("N814", new NotificationMapEntry("Basal Resumed. SG Still Under Low Limit. Check BG", R.drawable.down_yellow));
        notificationMap.put("N815", new NotificationMapEntry("Low Limit Changed. Basal Manually Resumed. Check BG", R.drawable.down_yellow));
        notificationMap.put("N816", new NotificationMapEntry("High Sensor Glucose", R.drawable.up3_red)); // Above high limit
        notificationMap.put("N817", new NotificationMapEntry("Alert Before High. Check BG", R.drawable.down_yellow));
        notificationMap.put("N819", new NotificationMapEntry("Auto Mode Exit. Basal Delivery Started. BG Required", R.drawable.down_yellow));
        notificationMap.put("N821", new NotificationMapEntry("Minimum Delivery Timeout. BG Required", R.drawable.down_yellow));
        notificationMap.put("N822", new NotificationMapEntry("Maximum Delivery Timeout. BG Required", R.drawable.down_yellow));
        notificationMap.put("N823", new NotificationMapEntry("High Sensor Glucose For Over 1 Hour", R.drawable.down_yellow));
        notificationMap.put("N827", new NotificationMapEntry("Urgent Low Sensor Glucose. Check BG", R.drawable.down_yellow));
        notificationMap.put("N829", new NotificationMapEntry("BG Required", R.drawable.down_yellow));
        notificationMap.put("N832", new NotificationMapEntry("Calibration Required", R.drawable.down_yellow));
        notificationMap.put("N833", new NotificationMapEntry("Correction Bolus Recommended", R.drawable.down_yellow));
        notificationMap.put("N869", new NotificationMapEntry("Calibration Reminder", R.drawable.calibration_grey));
        notificationMap.put("N870", new NotificationMapEntry("Recharge Transmitter Soon", R.drawable.down_yellow));
        notificationMap.put("Nnodata1", new NotificationMapEntry("Reconnecting To Pump", R.drawable.down_yellow));
        notificationMap.put("Nnodata2", new NotificationMapEntry("Lost Signal. Check Mobile Application", R.drawable.down_yellow));
        notificationMap.put("Nalert.sg.threshold.low.urgent", new NotificationMapEntry("Urgent Low Sensor Glucose", R.drawable.down_yellow));
        notificationMap.put("Nalert.sg.threshold.low", new NotificationMapEntry("Low Sensor Glucose", R.drawable.down_yellow));
        notificationMap.put("Nalert.sg.predictive.low", new NotificationMapEntry("Low Predicted", R.drawable.down_yellow));
        notificationMap.put("Nalert.sg.rate.falling", new NotificationMapEntry("Fall Alert", R.drawable.down_yellow));
        notificationMap.put("Nalert.sg.threshold.high", new NotificationMapEntry("High Sensor Glucose", R.drawable.down_yellow));
        notificationMap.put("Nalert.sg.predictive.high", new NotificationMapEntry("High Predicted", R.drawable.down_yellow));
        notificationMap.put("Nalert.sg.rate.rising", new NotificationMapEntry("Rise Alert", R.drawable.down_yellow));
        notificationMap.put("Nalert.transmitter.battery", new NotificationMapEntry("Transmitter Battery Empty", R.drawable.down_yellow));
        notificationMap.put("Nalert.sensor.replace.calibrationError", new NotificationMapEntry("Change Sensor", R.drawable.down_yellow));
        notificationMap.put("Nalert.sensor.replace.sensorError", new NotificationMapEntry("Change Sensor", R.drawable.down_yellow));
        notificationMap.put("Nalert.sensor.replace.lifetime", new NotificationMapEntry("Sensor End of Life", R.drawable.down_yellow));
        notificationMap.put("Nalert.transmitter.signal", new NotificationMapEntry("Lost Sensor Communication", R.drawable.down_yellow));
        notificationMap.put("Nalert.sensor.connection", new NotificationMapEntry("Sensor Connected", R.drawable.down_yellow));
        notificationMap.put("Nalert.sensor.calibration.rejected", new NotificationMapEntry("Calibration Not Accepted", R.drawable.down_yellow));
        notificationMap.put("Nalert.sensor.calibration.calibrate_now", new NotificationMapEntry("Calibrate Now", R.drawable.down_yellow));
        notificationMap.put("Nalert.sensor.error", new NotificationMapEntry("Sensor Glucose Not Available", R.drawable.down_yellow));
        notificationMap.put("Nalert.calibration.reminder", new NotificationMapEntry("Calibration Reminder", R.drawable.down_yellow));
        notificationMap.put("Nalert.transmitter.error", new NotificationMapEntry("Transmitter Error", R.drawable.down_yellow));
        notificationMap.put("Nalert.receiver.battery.low", new NotificationMapEntry("Mobile Device Battery Low", R.drawable.down_yellow));

        errorCodeMap = new HashMap<>();
        errorCodeMap.put("002", "002");
        errorCodeMap.put("003", "002");
        errorCodeMap.put("004", "002");
        errorCodeMap.put("013", "002");
        errorCodeMap.put("014", "002");
        errorCodeMap.put("015", "002");
        errorCodeMap.put("016", "002");
        errorCodeMap.put("017", "002");
        errorCodeMap.put("018", "002");
        errorCodeMap.put("019", "002");
        errorCodeMap.put("020", "002");
        errorCodeMap.put("022", "002");
        errorCodeMap.put("023", "002");
        errorCodeMap.put("026", "002");
        errorCodeMap.put("027", "002");
        errorCodeMap.put("028", "002");
        errorCodeMap.put("030", "002");
        errorCodeMap.put("031", "002");
        errorCodeMap.put("033", "002");
        errorCodeMap.put("034", "002");
        errorCodeMap.put("044", "002");
        errorCodeMap.put("045", "002");
        errorCodeMap.put("046", "002");
        errorCodeMap.put("049", "002");
        errorCodeMap.put("053", "002");
        errorCodeMap.put("054", "002");
        errorCodeMap.put("060", "002");
        errorCodeMap.put("063", "002");
        errorCodeMap.put("064", "002");
        errorCodeMap.put("065", "002");
        errorCodeMap.put("067", "002");
        errorCodeMap.put("068", "002");
        errorCodeMap.put("074", "002");
        errorCodeMap.put("075", "002");
        errorCodeMap.put("076", "002");
        errorCodeMap.put("079", "002");
        errorCodeMap.put("080", "002");
        errorCodeMap.put("081", "002");
        errorCodeMap.put("082", "002");
        errorCodeMap.put("117", "117");
        errorCodeMap.put("817", "817");
        errorCodeMap.put("805", "805");
        errorCodeMap.put("819", "819");
        errorCodeMap.put("820", "819");
        errorCodeMap.put("012", "012");
        errorCodeMap.put("807", "807");
        errorCodeMap.put("808", "807");
        errorCodeMap.put("814", "814");
        errorCodeMap.put("103", "103");
        errorCodeMap.put("829", "829");
        errorCodeMap.put("830", "829");
        errorCodeMap.put("831", "829");
        errorCodeMap.put("100", "100");
        errorCodeMap.put("051", "051");
        errorCodeMap.put("775", "775");
        errorCodeMap.put("776", "776");
        errorCodeMap.put("869", "869");
        errorCodeMap.put("832", "832");
        errorCodeMap.put("812", "812");
        errorCodeMap.put("777", "777");
        errorCodeMap.put("778", "777");
        errorCodeMap.put("789", "777");
        errorCodeMap.put("833", "833");
        errorCodeMap.put("024", "024");
        errorCodeMap.put("035", "024");
        errorCodeMap.put("040", "024");
        errorCodeMap.put("047", "024");
        errorCodeMap.put("048", "024");
        errorCodeMap.put("050", "024");
        errorCodeMap.put("055", "024");
        errorCodeMap.put("131", "024");
        errorCodeMap.put("052", "052");
        errorCodeMap.put("007", "007");
        errorCodeMap.put("008", "007");
        errorCodeMap.put("140", "140");
        errorCodeMap.put("801", "801");
        errorCodeMap.put("816", "816");
        errorCodeMap.put("823", "823");
        errorCodeMap.put("824", "823");
        errorCodeMap.put("058", "058");
        errorCodeMap.put("069", "069");
        errorCodeMap.put("780", "780");
        errorCodeMap.put("781", "780");
        errorCodeMap.put("795", "795");
        errorCodeMap.put("815", "815");
        errorCodeMap.put("802", "802");
        errorCodeMap.put("803", "803");
        errorCodeMap.put("822", "822");
        errorCodeMap.put("821", "821");
        errorCodeMap.put("107", "107");
        errorCodeMap.put("066", "066");
        errorCodeMap.put("796", "796");
        errorCodeMap.put("057", "057");
        errorCodeMap.put("006", "006");
        errorCodeMap.put("084", "084");
        errorCodeMap.put("061", "061");
        errorCodeMap.put("037", "037");
        errorCodeMap.put("038", "037");
        errorCodeMap.put("039", "037");
        errorCodeMap.put("041", "037");
        errorCodeMap.put("042", "037");
        errorCodeMap.put("043", "037");
        errorCodeMap.put("025", "025");
        errorCodeMap.put("029", "029");
        errorCodeMap.put("077", "077");
        errorCodeMap.put("779", "779");
        errorCodeMap.put("870", "870");
        errorCodeMap.put("011", "011");
        errorCodeMap.put("073", "011");
        errorCodeMap.put("104", "104");
        errorCodeMap.put("113", "113");
        errorCodeMap.put("105", "105");
        errorCodeMap.put("106", "105");
        errorCodeMap.put("130", "130");
        errorCodeMap.put("797", "797");
        errorCodeMap.put("798", "797");
        errorCodeMap.put("794", "794");
        errorCodeMap.put("109", "109");
        errorCodeMap.put("784", "784");
        errorCodeMap.put("110", "110");
        errorCodeMap.put("810", "810");
        errorCodeMap.put("811", "810");
        errorCodeMap.put("809", "809");
        errorCodeMap.put("062", "062");
        errorCodeMap.put("070", "062");
        errorCodeMap.put("071", "062");
        errorCodeMap.put("072", "062");
        errorCodeMap.put("108", "062");
        errorCodeMap.put("114", "062");
        errorCodeMap.put("786", "062");
        errorCodeMap.put("787", "062");
        errorCodeMap.put("788", "062");
        errorCodeMap.put("799", "062");
        errorCodeMap.put("806", "062");
        errorCodeMap.put("825", "062");
        errorCodeMap.put("828", "062");
        errorCodeMap.put("827", "827");

    }

    public static NotificationMapEntry parseNgpNotification(ActiveNotification notification) {
        return parseNgpNotification(notification.faultId);
    }

    public static NotificationMapEntry parseNgpNotification(ClearedNotification notification) {
        return parseNgpNotification(notification.faultId);
    }

    public static NotificationMapEntry parseNgpNotification(int ngpErrorCode) {
        String errorTextId;
        String internalEC;

        String formattedEC = String.format("%03d", ngpErrorCode);
        if (errorCodeMap.containsKey(formattedEC)) {
            internalEC = errorCodeMap.get(formattedEC);
        } else {
            internalEC = formattedEC;
        }
        errorTextId = ERROR_TEXT_PREFIX_NGP + internalEC;

        if (notificationMap.containsKey(errorTextId)) {
            return notificationMap.get(errorTextId);
        }

        return null;
    }
}
