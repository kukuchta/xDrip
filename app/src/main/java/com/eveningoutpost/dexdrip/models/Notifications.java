package com.eveningoutpost.dexdrip.models;

import android.provider.BaseColumns;
import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.activeandroid.util.SQLiteUtils;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.google.gson.annotations.Expose;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Table(name = "Notifications", id = BaseColumns._ID)
public class Notifications extends Model {

    public static final String NOTIFICATION_TYPE_DELIVERY_STATE = "DELIVERY_STATE";
    public static final String NOTIFICATION_TYPE_FALLING_RAPIDLY = "FALLING_RAPIDLY";
    public static final String NOTIFICATION_TYPE_BEFORE_LOW_LIMIT = "BEFORE_LOW_LIMIT";
    public static final String NOTIFICATION_TYPE_LOW_LIMIT_REACHED = "LOW_LIMIT_REACHED";
    public static final String NOTIFICATION_TYPE_RISING_RAPIDLY = "RISING_RAPIDLY";
    public static final String NOTIFICATION_TYPE_BEFORE_HIGH_LIMIT = "BEFORE_HIGH_LIMIT";
    public static final String NOTIFICATION_TYPE_HIGH_LIMIT_REACHED = "HIGH_LIMIT_REACHED";
    public static final String NOTIFICATION_TYPE_ALERT = "ALERT";
    public static final String NOTIFICATION_TYPE_WARNING = "WARNING";
    public static final String NOTIFICATION_TYPE_CALIBRATION = "CALIBRATION";
    public static final String NOTIFICATION_TYPE_CALIBRATION_MISSED = "CALIBRATION_MISSED";
    public static final String NOTIFICATION_TYPE_BATTERY_WARNING = "BATTERY_WARNING";
    public static final String NOTIFICATION_TYPE_BATTERY_ALERT = "BATTERY_ALERT";

    private static final String TAG = Notifications.class.getSimpleName();
    private static boolean patched = false;

    @Expose
    @Column(name = "timestamp", index = true)
    public long timestamp;
    @Expose
    @Column(name = "type")
    public String type;
    @Expose
    @Column(name = "imageId")
    public int imageId;
    @Expose
    @Column(name = "notes")
    public String notes;
    @Expose
    @Column(name = "uuid", unique = true, onUniqueConflicts = Column.ConflictAction.IGNORE)
    public String uuid;
    @Expose
    @Column(name = "deliverySuspended")
    public int deliverySuspended;
    @Expose
    @Column(name = "created_at")
    public String created_at;

    public Notifications()
    {
        deliverySuspended = 0;
        imageId = R.drawable.empty;
    }

    public static synchronized Notifications create(String type, String note, final boolean deliverySuspended, long timestamp) {

        if (timestamp == 0) {
            timestamp = new Date().getTime();
        }

        final Notifications notification = new Notifications();
        notification.deliverySuspended = deliverySuspended ? 1 : 0;
        notification.timestamp = timestamp;
        notification.type = type;
        notification.notes = note;
        notification.created_at = DateUtil.toISOString(timestamp);
        notification.uuid = UUID.randomUUID().toString();
        notification.save();
        return notification;
    }

    private static void fixUpTable() {
        if (patched) return;
        String[] patchup = {
                "CREATE TABLE Notifications (_id INTEGER PRIMARY KEY AUTOINCREMENT);",
                "ALTER TABLE Notifications ADD COLUMN timestamp INTEGER;",
                "ALTER TABLE Notifications ADD COLUMN uuid TEXT;",
                "ALTER TABLE Notifications ADD COLUMN type TEXT;",
                "ALTER TABLE Notifications ADD COLUMN imageId INTEGER;",
                "ALTER TABLE Notifications ADD COLUMN notes TEXT;",
                "ALTER TABLE Notifications ADD COLUMN created_at TEXT;",
                "ALTER TABLE Notifications ADD COLUMN deliverySuspended INTEGER;",
                "CREATE INDEX index_Notifications_timestamp on Notifications(timestamp);",
                "CREATE UNIQUE INDEX index_Notifications_uuid on Notifications(uuid);"};

        for (String patch : patchup) {
            try {
                SQLiteUtils.execSql(patch);
                //Log.e(TAG, "Processed patch should not have succeeded!!: " + patch);
            } catch (Exception e) {
                // Log.d(TAG, "Patch: " + patch + " generated exception as it should: " + e.toString());
            }
        }
        patched = true;
    }

    public static Notifications last() {
        fixUpTable();
        return new Select()
                .from(Notifications.class)
                .orderBy("_ID desc")
                .executeSingle();
    }

    public static Notifications firstByType(final String type) {
        fixUpTable();
        return new Select()
                .from(Notifications.class)
                .where("type = ?", type)
                .orderBy("_ID")
                .executeSingle();
    }

    public static Notifications lastByType(final String type) {
        fixUpTable();
        return new Select()
                .from(Notifications.class)
                .where("type = ?", type)
                .orderBy("_ID DESC")
                .executeSingle();
    }

    public static List<Notifications> latest(int num) {
        try {
            return new Select()
                    .from(Notifications.class)
                    .orderBy("timestamp desc")
                    .limit(num)
                    .execute();
        } catch (android.database.sqlite.SQLiteException e) {
            fixUpTable();
            return null;
        }
    }

    public static Notifications byuuid(String uuid) {
        if (uuid == null) return null;
        return new Select()
                .from(Notifications.class)
                .where("uuid = ?", uuid)
                .orderBy("_ID desc")
                .executeSingle();
    }

    public static Notifications byid(long id) {
        return new Select()
                .from(Notifications.class)
                .where("_ID = ?", id)
                .executeSingle();
    }

    public static Notifications byTimestamp(long timestamp) {
        return byTimestamp(timestamp, 1500);
    }

    public static Notifications byTimestamp(long timestamp, int plus_minus_millis) {
        return new Select()
                .from(Notifications.class)
                .where("timestamp <= ? and timestamp >= ?", (timestamp + plus_minus_millis), (timestamp - plus_minus_millis)) // window
                .orderBy("abs(timestamp-" + Long.toString(timestamp) + ") asc")
                .executeSingle();
    }

    public static List<Notifications> listByTimestamp(long timestamp) {
        return new Select()
                .from(Notifications.class)
                .where("timestamp = ?", timestamp)
                .orderBy("timestamp desc")
                .execute();
    }

    public static void delete_all() {
        new Delete()
                .from(Notifications.class)
                .execute();
        Home.staticRefreshBGCharts();
        // not synced with uploader queue - should we?
    }

    public static void delete_by_timestamp(long timestamp, int accuracy) {
        final Notifications t = byTimestamp(timestamp, accuracy); // do we need to alter default accuracy?
        if (t != null) {
            Log.d(TAG, "Deleting treatment closest to: " + JoH.dateTimeText(timestamp) + " matches uuid: " + t.uuid);
            delete_by_uuid(t.uuid);
        } else {
            Log.e(TAG, "Couldn't find a treatment near enough to " + JoH.dateTimeText(timestamp) + " to delete!");
        }
    }

    public static void delete_by_uuid(String uuid) {
        Notifications thistreat = byuuid(uuid);
        if (thistreat != null) {
            thistreat.delete();
            Home.staticRefreshBGCharts();
        }
    }

    public static void delete_last() {
        Notifications thistreat = last();
        if (thistreat != null) {
            thistreat.delete();
            Home.staticRefreshBGCharts();
        }
    }

    public static void cleanup(final int retention_days) {
        fixUpTable();
        new Delete()
                .from(Notifications.class)
                .where("timestamp < ?", JoH.tsl() - (retention_days * Constants.DAY_IN_MS))
                .execute();
    }

    public static List<Notifications> latestForGraph(int number, double startTime) {
        return latestForGraph(number, startTime, JoH.ts());
    }

    public static List<Notifications> latestForGraph(int number, double startTime, double endTime) {
        fixUpTable();
        DecimalFormat df = new DecimalFormat("#");
        df.setMaximumFractionDigits(1); // are there decimal points in the database??
        return new Select()
                .from(Notifications.class)
                .where("timestamp >= ? and timestamp <= ?", df.format(startTime), df.format(endTime))
                .orderBy("timestamp asc")
                .limit(number)
                .execute();
    }

    public static List<Notifications> latestForGraph(final int number, final long startTime, final long endTime) {
        fixUpTable();
        return new Select()
                .from(Notifications.class)
                .where("timestamp >= ? and timestamp <= ?", startTime, endTime)
                .orderBy("timestamp asc")
                .limit(number)
                .execute();
    }

    public static long getTimeStampWithOffset(double offset) {
        //  optimisation instead of creating a new date each time?
        return (long) (new Date().getTime() - offset);
    }

    public String getBestShortText() {
        //if (!eventType.equals(DEFAULT_EVENT_TYPE)) {
        //    return eventType;
        //} else {
            return noteHasContent() ? notes : "Treatment";
        //}
    }

    public boolean noteHasContent() {
        return notes != null && notes.length() > 0;
    }
}



