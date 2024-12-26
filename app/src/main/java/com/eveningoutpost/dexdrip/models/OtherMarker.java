package com.eveningoutpost.dexdrip.models;

import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.activeandroid.util.SQLiteUtils;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.Marker;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.google.gson.annotations.Expose;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Table(name = "OtherMarker", id = BaseColumns._ID)
public class OtherMarker extends Model {

    private static final String TAG = OtherMarker.class.getSimpleName();
    private static boolean patched = false;


    @Expose
    @Column(name = "timestamp", index = true)
    public long timestamp;

    @Expose
    @Column(name = "uuid", unique = true, onUniqueConflicts = Column.ConflictAction.IGNORE)
    public String uuid;

    @Expose
    @Column(name = "type")
    public String type;

    @Expose
    @Column(name = "state")
    public int state;

    @Expose
    @Column(name = "created_at")
    public String created_at;

    public OtherMarker()
    {
        type = "";
        state = 0;
    }

    private static void fixUpTable() {
        if (patched) {
            return;
        }

        String[] commands = {
                "CREATE TABLE OtherMarker (_id INTEGER PRIMARY KEY AUTOINCREMENT);",
                "ALTER TABLE OtherMarker ADD COLUMN timestamp INTEGER;",
                "ALTER TABLE OtherMarker ADD COLUMN uuid TEXT;",
                "ALTER TABLE OtherMarker ADD COLUMN type TEXT;",
                "ALTER TABLE OtherMarker ADD COLUMN state INTEGER;",
                "ALTER TABLE OtherMarker ADD COLUMN created_at TEXT;",
                "CREATE INDEX index_OtherMarker_timestamp on OtherMarker(timestamp);",
                "CREATE UNIQUE INDEX index_OtherMarker_uuid on OtherMarker(uuid);"};

        for (String command : commands) {
            try {
                SQLiteUtils.execSql(command);
                //Log.e(TAG, "Processed command should not have succeeded!!: " + command);
            } catch (Exception e) {
                // Log.d(TAG, "Patch: " + command + " generated exception as it should: " + e.toString());
            }
        }
        patched = true;
    }

    public static boolean autoModeStatusExists(boolean state, long timestamp) {
        fixUpTable();
        int markerState = state ? 1 : 0;
        List<OtherMarker> markers = new Select()
                .from(OtherMarker.class)
                .where("timestamp = ? and type = ? and state = ?", timestamp, Marker.TYPE_AUTO_MODE_STATUS, markerState)
                .orderBy("timestamp desc")
                .execute();
        return !markers.isEmpty();
    }

    public static synchronized void createAutoModeStatus(boolean autoModeOn, long timestamp) {
        fixUpTable();
        final OtherMarker marker = new OtherMarker();
        marker.timestamp = timestamp;
        marker.uuid = UUID.randomUUID().toString();
        marker.type = Marker.TYPE_AUTO_MODE_STATUS;
        marker.state = autoModeOn ? 1 : 0;
        marker.created_at = DateUtil.toISOString(timestamp);
        marker.save();
    }

    public static boolean lowGlucoseSuspendExists(boolean deliverySuspended, long timestamp) {
        fixUpTable();
        int markerState = deliverySuspended ? 1 : 0;
        List<OtherMarker> markers = new Select()
                .from(OtherMarker.class)
                .where("timestamp = ? and type = ? and state = ?", timestamp, Marker.TYPE_LOW_GLUCOSE_SUSPENDED, markerState)
                .orderBy("timestamp desc")
                .execute();
        return !markers.isEmpty();
    }

    public static synchronized void createLowGlucoseSuspend(boolean deliverySuspended, long timestamp) {
        fixUpTable();
        final OtherMarker marker = new OtherMarker();
        marker.timestamp = timestamp;
        marker.uuid = UUID.randomUUID().toString();
        marker.type = Marker.TYPE_LOW_GLUCOSE_SUSPENDED;
        marker.state = deliverySuspended ? 1 : 0;
        marker.created_at = DateUtil.toISOString(timestamp);
        marker.save();
    }

    public static OtherMarker last() {
        fixUpTable();
        return new Select()
                .from(OtherMarker.class)
                .orderBy("_ID desc")
                .executeSingle();
    }

    public static OtherMarker firstByType(final String type) {
        fixUpTable();
        return new Select()
                .from(OtherMarker.class)
                .where("type = ?", type)
                .orderBy("_ID")
                .executeSingle();
    }

    public static OtherMarker lastByType(final String type) {
        fixUpTable();
        return new Select()
                .from(OtherMarker.class)
                .where("type = ?", type)
                .orderBy("_ID DESC")
                .executeSingle();
    }


    public static List<OtherMarker> latest(int amount) {
        fixUpTable();
        return new Select()
                .from(OtherMarker.class)
                .orderBy("timestamp desc")
                .limit(amount)
                .execute();
    }

    public static OtherMarker byUuid(String uuid) {
        if (uuid == null) {
            return null;
        }
        fixUpTable();
        return new Select()
                .from(OtherMarker.class)
                .where("uuid = ?", uuid)
                .orderBy("_ID desc")
                .executeSingle();
    }

    public static OtherMarker byId(long id) {
        fixUpTable();
        return new Select()
                .from(OtherMarker.class)
                .where("_ID = ?", id)
                .executeSingle();
    }

    public static OtherMarker byTimestamp(long timestamp) {
        return byTimestamp(timestamp, 1500);
    }

    public static OtherMarker byTimestamp(long timestamp, int accuracy) {
        fixUpTable();
        return new Select()
                .from(OtherMarker.class)
                .where("timestamp <= ? and timestamp >= ?", (timestamp + accuracy), (timestamp - accuracy)) // window
                .orderBy("abs(timestamp-" + Long.toString(timestamp) + ") asc")
                .executeSingle();
    }

    public static List<OtherMarker> listByTimestamp(long timestamp) {
        fixUpTable();
        return new Select()
                .from(OtherMarker.class)
                .where("timestamp = ?", timestamp)
                .orderBy("timestamp desc")
                .execute();
    }

    public static void deleteAll() {
        fixUpTable();
        new Delete()
                .from(OtherMarker.class)
                .execute();
        Home.staticRefreshBGCharts();
        // not synced with uploader queue - should we?
    }

    public static void deleteByTimestamp(long timestamp, int accuracy) {
        final OtherMarker marker = byTimestamp(timestamp, accuracy); // do we need to alter default accuracy?
        if (marker != null) {
            Log.d(TAG, "Deleting treatment closest to: " + JoH.dateTimeText(timestamp) + " matches uuid: " + marker.uuid);
            deleteByUuid(marker.uuid);
        } else {
            Log.e(TAG, "Couldn't find a treatment near enough to " + JoH.dateTimeText(timestamp) + " to delete!");
        }
    }

    public static void deleteByUuid(String uuid) {
        OtherMarker marker = byUuid(uuid);
        if (marker != null) {
            marker.delete();
            Home.staticRefreshBGCharts();
        }
    }

    public static void deleteLast() {
        OtherMarker marker = last();
        if (marker != null) {
            marker.delete();
            Home.staticRefreshBGCharts();
        }
    }

    public static void cleanup(final int retentionDays) {
        fixUpTable();
        new Delete()
                .from(OtherMarker.class)
                .where("timestamp < ?", JoH.tsl() - (retentionDays * Constants.DAY_IN_MS))
                .execute();
    }

    public static List<OtherMarker> latestForGraph(int number, double startTime) {
        return latestForGraph(number, startTime, JoH.ts());
    }

    public static List<OtherMarker> latestForGraph(int number, double startTime, double endTime) {
        fixUpTable();
        DecimalFormat df = new DecimalFormat("#");
        df.setMaximumFractionDigits(1); // are there decimal points in the database??
        return new Select()
                .from(OtherMarker.class)
                .where("timestamp >= ? and timestamp <= ?", df.format(startTime), df.format(endTime))
                .orderBy("timestamp asc")
                .limit(number)
                .execute();
    }

    public static List<OtherMarker> latestForGraph(final int number, final long startTime, final long endTime) {
        fixUpTable();
        return new Select()
                .from(OtherMarker.class)
                .where("timestamp >= ? and timestamp <= ?", startTime, endTime)
                .orderBy("timestamp asc")
                .limit(number)
                .execute();
    }

    public static long getTimestampWithOffset(double offset) {
        //  optimisation instead of creating a new date each time?
        return (long) (new Date().getTime() - offset);
    }
}



