package com.eveningoutpost.dexdrip.cgm.carelinkfollow;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.Alarm;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.BloodTest;
import com.eveningoutpost.dexdrip.models.DateUtil;
import com.eveningoutpost.dexdrip.models.Notifications;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.models.Treatments;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.PumpStatus;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.ActiveNotification;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.ClearedNotification;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.Marker;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.RecentData;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.SensorGlucose;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.TextMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.eveningoutpost.dexdrip.models.BgReading.SPECIAL_FOLLOWER_PLACEHOLDER;
import static com.eveningoutpost.dexdrip.models.Treatments.pushTreatmentSyncToWatch;


/**
 * Medtronic CareLink Data Processor
 * - process CareLink data and convert to xDrip internal data
 * - update xDrip internal data
 */
public class CareLinkDataProcessor {


    private static final String TAG = "CareLinkFollowDP";
    private static final boolean D = false;

    private static final String SOURCE_CARELINK_FOLLOW = "CareLink Follow";

    public static final String CARELINK_NOTIFICATION_DELIVERY_SUSPENDED = "DELIVERY_SUSPENDED";
    public static final String CARELINK_NOTIFICATION_SG_APPROACH_LOW_LIMIT = "BC_SID_SG_APPROACH_LOW_LIMIT_CHECK_BG";
    public static final String CARELINK_NOTIFICATION_BUTTON_PRESSED_FOR_MOR_THAN_3_MIN = "BC_SID_BUTTON_PRESSED_FOR_MOR_THAN_3_MIN";
    public static final String CARELINK_NOTIFICATION_SG_X_CHECK_BG = "BC_MESSAGE_DELIVERY_STOPPED_SG_X_CHECK_BG";
    public static final String CARELINK_NOTIFICATION_SG_RISE_RAPID = "BC_SID_SG_RISE_RAPID";
    public static final String CARELINK_NOTIFICATION_CHECK_BG_AND_CALIBRATE_SENSOR_TO_RECEIVE = "BC_SID_CHECK_BG_AND_CALIBRATE_SENSOR_TO_RECEIVE";
    public static final String CARELINK_NOTIFICATION_SG_APPROACHING_LOW_LIMIT = "BC_MESSAGE_DELIVERY_STOPPED_SG_APPROACHILG_LOW_LIMIT_CHECK_BG";
    public static final String CARELINK_NOTIFICATION_REPLACE_BATTERY_SOON = "BC_SID_REPLACE_BATTERY_SOON";
    public static final String CARELINK_NOTIFICATION_TYPE_HIGH_SG = "BC_SID_HIGH_SG_CHECK_BG";
    public static final String CARELINK_NOTIFICATION_INSERT_NEW_BATTERY = "BC_SID_DELIVERY_STOPPED_INSERT_NEW_BATTERY";


    static synchronized void processData(final RecentData recentData, final boolean live) {

        List<SensorGlucose> filteredSgList;
        List<Marker> filteredMarkerList;

        UserError.Log.d(TAG, "Start processsing data...");

        //SKIP ALL IF EMPTY!!!
        if (recentData == null) {
            UserError.Log.e(TAG, "Recent data is null, processing stopped!");
            return;
        }

        if (recentData.sgs == null) UserError.Log.d(TAG, "SGs is null!");

        //SKIP DATA processing if NO PUMP CONNECTION (time shift seems to be different in this case, needs further analysis)
        if (recentData.isNGP() && !recentData.pumpCommunicationState) {
            UserError.Log.d(TAG, "Not connected to pump => time can be wrong, leave processing!");
            //return;
        }

        //SENSOR GLUCOSE (if available)
        if (false) {

            final BgReading lastBg = BgReading.lastNoSenssor();
            final long lastBgTimestamp = lastBg != null ? lastBg.timestamp : 0;

            //create filtered sortable SG list
            filteredSgList = new ArrayList<>();
            for (SensorGlucose sg : recentData.sgs) {
                //SG DateTime is null (sensor expired?)
                if (sg != null && sg.datetimeAsDate != null) {
                    filteredSgList.add(sg);
                }
            }

            if (filteredSgList.size() > 0) {

                final Sensor sensor = Sensor.createDefaultIfMissing();
                sensor.save();

                // place in order of oldest first
                Collections.sort(filteredSgList, (o1, o2) -> o1.datetimeAsDate.compareTo(o2.datetimeAsDate));

                for (final SensorGlucose sg : filteredSgList) {

                    //Not EPOCH 0 (warmup?)
                    if (sg.datetimeAsDate.getTime() > 1) {

                        //Not in the future
                        if (sg.datetimeAsDate.getTime() < new Date().getTime() + 300_000) {

                            //Not 0 SG (not calibrated?)
                            if (sg.sg > 0) {

                                //newer than last BG
                                if (sg.datetimeAsDate.getTime() > lastBgTimestamp) {

                                    if (sg.datetimeAsDate.getTime() > 0) {

                                        //New entry
                                        if (BgReading.getForPreciseTimestamp(sg.datetimeAsDate.getTime(), 10_000) == null) {
                                            UserError.Log.d(TAG, "NEW NEW NEW New entry: " + sg.toS());

                                            if (live) {
                                                final BgReading bg = new BgReading();
                                                bg.timestamp = sg.datetimeAsDate.getTime();
                                                bg.calculated_value = (double) sg.sg;
                                                bg.raw_data = SPECIAL_FOLLOWER_PLACEHOLDER;
                                                bg.filtered_data = (double) sg.sg;
                                                bg.noise = "";
                                                bg.uuid = UUID.randomUUID().toString();
                                                bg.calculated_value_slope = 0;
                                                bg.sensor = sensor;
                                                bg.sensor_uuid = sensor.uuid;
                                                bg.source_info = SOURCE_CARELINK_FOLLOW;
                                                bg.save();
                                                bg.find_slope();
                                                Inevitable.task("entry-proc-post-pr", 500, () -> bg.postProcess(false));
                                            }
                                        }
                                    } else {
                                        UserError.Log.e(TAG, "Could not parse a timestamp from: " + sg.toS());
                                    }
                                }

                            } else {
                                UserError.Log.d(TAG, "SG is 0 (calibration missed?)");
                            }

                        } else {
                            UserError.Log.d(TAG, "SG DateTime is 0 (warmup phase?)");
                        }
                    } else {
                        UserError.Log.d(TAG, "SG DateTime in future: " + sg.datetime);
                    }
                }
            }
        }


        //MARKERS (if available)
        if (recentData.markers != null) {

            //Filter markers
            filteredMarkerList = new ArrayList<>();
            for (Marker marker : recentData.markers) {
                if (marker != null && marker.type != null && marker.dateTime != null) {
                    filteredMarkerList.add(marker);
                }
            }

            if (filteredMarkerList.size() > 0) {
                //sort markers by time
                Collections.sort(filteredMarkerList, (o1, o2) -> o1.dateTime.compareTo(o2.dateTime));

                //process markers one-by-one
                for (Marker marker : filteredMarkerList) {

                    //FINGER BG
                    if (marker.isBloodGlucose() && Pref.getBooleanDefaultFalse("clfollow_download_finger_bgs")) {
                        //check required values
                        if (marker.value != null && !marker.value.equals(0)) {
                            //new blood test
                            if (BloodTest.getForPreciseTimestamp(marker.dateTime.getTime(), 10000) == null) {
                                BloodTest.create(marker.dateTime.getTime(), marker.value, SOURCE_CARELINK_FOLLOW);
                            }
                        }

                        //INSULIN, MEAL => Treatment
                    } else if ((marker.type.equals(Marker.MARKER_TYPE_INSULIN) && Pref.getBooleanDefaultFalse("clfollow_download_boluses"))
                            || (marker.type.equals(Marker.MARKER_TYPE_MEAL) && Pref.getBooleanDefaultFalse("clfollow_download_meals"))) {

                        //insulin, meal only for pumps (not value in case of GC)
                        if (recentData.isNGP()) {

                            final Treatments t;
                            double carbs = 0;
                            double insulin = 0;

                            //Extract treament infos (carbs, insulin)
                            //Insulin
                            if (marker.type.equals(Marker.MARKER_TYPE_INSULIN)) {
                                carbs = 0;
                                if (marker.deliveredExtendedAmount != null && marker.deliveredFastAmount != null) {
                                    insulin = marker.deliveredExtendedAmount + marker.deliveredFastAmount;
                                }
                                //SKIP if insulin = 0
                                if (insulin == 0) continue;
                                //Carbs
                            } else if (marker.type.equals(Marker.MARKER_TYPE_MEAL)) {
                                if (marker.amount != null) {
                                    carbs = marker.amount;
                                }
                                insulin = 0;
                                //SKIP if carbs = 0
                                if (carbs == 0) continue;
                            }

                            //new Treatment
                            if (isNewTreatment(carbs, insulin, marker.dateTime.getTime())) {
                                t = Treatments.create(carbs, insulin, marker.dateTime.getTime());
                                if (t != null) {
                                    t.enteredBy = SOURCE_CARELINK_FOLLOW;
                                    t.save();
                                    if (Home.get_show_wear_treatments())
                                        pushTreatmentSyncToWatch(t, true);
                                }
                            }
                        }

                    } else if (marker.type.equals(Marker.MARKER_TYPE_LOW_GLUCOSE_SUSPENDED) && Pref.getBooleanDefaultFalse("clfollow_download_notifications")) {
                        //Delivery suspended marker only for pumps (not value in case of GC)
                        if (recentData.isNGP()) {

                            final Notifications notification;
                            String notificationType = Notifications.NOTIFICATION_TYPE_DELIVERY_STATE;

                            //new Notifications
                            if (isNewDeliveryNotification(notificationType, marker.deliverySuspended, marker.dateTime.getTime())) {
                                notification = Notifications.create(notificationType, "", marker.deliverySuspended, marker.dateTime.getTime());
                                notification.save();
                            }
                        }
                    }
                }
            }
        }

        //PUMP INFO (Pump Status)
        if (recentData.isNGP()) {
            PumpStatus.setReservoir(recentData.reservoirRemainingUnits);
            PumpStatus.setBattery(recentData.medicalDeviceBatteryLevelPercent);
            if (recentData.activeInsulin != null)
                PumpStatus.setBolusIoB(recentData.activeInsulin.amount);
            PumpStatus.syncUpdate();
        }
		
        // LAST ALARM -> NOTE (only for GC)
        if (Pref.getBooleanDefaultFalse("clfollow_download_notifications")) {

            // Only Guardian Connect, NGP has all in notifications
            if (recentData.isGM() && recentData.lastAlarm != null) {
                //Add notification from alarm
                addGuardianNotification(recentData.lastAlarm);
            }
        }


        //NOTIFICATIONS -> NOTE
        if (Pref.getBooleanDefaultFalse("clfollow_download_notifications")) {
            if (recentData.notificationHistory != null) {
                //Active Notifications
                if (recentData.notificationHistory.activeNotifications != null) {
                    for (ActiveNotification activeNotification : recentData.notificationHistory.activeNotifications) {
                        addNgpNotification(activeNotification);
                    }
                }
                //Cleared Notifications
                if (recentData.notificationHistory.clearedNotifications != null) {
                    for (ClearedNotification clearedNotification : recentData.notificationHistory.clearedNotifications) {
                        addNgpNotification(clearedNotification);
                    }
                }
            }
        }

    }

    //Check if treatment is new (no identical entry (timestamp, carbs, insulin) exists)
    protected static boolean isNewTreatment(double carbs, double insulin, long timestamp) {

        List<Treatments> treatmentsList;
        //Treatment with same timestamp and carbs + insulin exists?
        treatmentsList = Treatments.listByTimestamp(timestamp);
        if (treatmentsList != null) {
            for (Treatments treatments : treatmentsList) {
                if (treatments.carbs == carbs && treatments.insulin == insulin)
                    return false;
            }
        }
        return true;
    }

    //Check if notification is new (no identical entry (timestamp) exists)
    protected static boolean isNewDeliveryNotification(String type, boolean deliverySuspended, long timestamp) {

        List<Notifications> notificationsList;
        int deliverySuspendedValue = deliverySuspended ? 1 : 0;
        //Treatment with same timestamp and data exists?
        notificationsList = Notifications.listByTimestamp(timestamp);
        if (notificationsList != null) {
            for (Notifications notification : notificationsList) {
                if (notification.timestamp == timestamp &&
                        notification.deliverySuspended == deliverySuspendedValue &&
                        notification.type.equals(type))
                    return false;
            }
        }
        return true;
    }

    //Check note is new
    protected static boolean isNewNotification(String note, String type, long timestamp) {

        List<Notifications> notificationsList;
        //Treatment with same timestamp and note text exists?
        notificationsList = Notifications.listByTimestamp(timestamp);
        if (notificationsList != null) {
            for (Notifications notification : notificationsList) {
                if (notification.timestamp == timestamp &&
                        notification.notes.contains(note) &&
                        notification.type.equals(type))
                    return false;
            }
        }

        return true;
    }

    protected static void addNgpNotification(ActiveNotification activeNotification) {
        if (activeNotification != null && activeNotification.dateTime != null) {
            TextMap.NotificationMapEntry entry = TextMap.parseNgpNotification(activeNotification);
            addNotification(activeNotification.dateTime, entry.getMessage(), entry.getType(), entry.getImage());
        }
    }

    protected static void addNgpNotification(ClearedNotification clearedNotification) {
        if (clearedNotification != null && clearedNotification.triggeredDateTime != null) {
            TextMap.NotificationMapEntry entry = TextMap.parseNgpNotification(clearedNotification);
            addNotification(clearedNotification.triggeredDateTime, entry.getMessage(), entry.getType(), entry.getImage());
        }
    }

    //Create notification from CareLink messageId
    protected static void addGuardianNotification(Alarm alarm) {
        if (alarm != null && alarm.datetimeAsDate != null && alarm.kind != null) {
            TextMap.NotificationMapEntry entry = TextMap.parseGuardianNotification(alarm);
            addNotification(alarm.datetimeAsDate, entry.getMessage(), entry.getType(), entry.getImage());
        }
    }

    //Create notification from CareLink note info
    protected static void addNotification(Date date, String noteText, String type, int imageId) {

        //Valid date
        if (date != null && noteText != null) {
            //New note
            if (isNewNotification(noteText, type, date.getTime())) {
                //create_note in Treatment is not good, because of automatic link to other treatments in 5 mins range
                Notifications notification = new Notifications();
                notification.notes = noteText;
                notification.type = type;
                notification.imageId = imageId;
                notification.timestamp = date.getTime();
                notification.created_at = DateUtil.toISOString(notification.timestamp);
                notification.uuid = UUID.randomUUID().toString();
                notification.save();
            }
        }
    }
}