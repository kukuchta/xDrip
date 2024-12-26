package com.eveningoutpost.dexdrip.models;

import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.activeandroid.util.SQLiteUtils;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.google.gson.annotations.Expose;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Table(name = "Autocorrection", id = BaseColumns._ID)
public class Autocorrection extends Model {

    private static final String TAG = Autocorrection.class.getSimpleName();
    private static boolean patched = false;


    @Expose
    @Column(name = "timestamp", index = true)
    public long timestamp;

    @Expose
    @Column(name = "uuid", unique = true, onUniqueConflicts = Column.ConflictAction.IGNORE)
    public String uuid;

    @Expose
    @Column(name = "deliveredFastAmount")
    public double deliveredFastAmount;

    @Expose
    @Column(name = "created_at")
    public String created_at;

    public Autocorrection()
    {
        deliveredFastAmount = 0d;
    }

    private static void fixUpTable() {
        if (patched) {
            return;
        }

        String[] commands = {
                "CREATE TABLE Autocorrection (_id INTEGER PRIMARY KEY AUTOINCREMENT);",
                "ALTER TABLE Autocorrection ADD COLUMN timestamp INTEGER;",
                "ALTER TABLE Autocorrection ADD COLUMN uuid TEXT;",
                "ALTER TABLE Autocorrection ADD COLUMN deliveredFastAmount REAL;",
                "ALTER TABLE Autocorrection ADD COLUMN created_at TEXT;",
                "CREATE INDEX index_Autocorrection_timestamp on Autocorrection(timestamp);",
                "CREATE UNIQUE INDEX index_Autocorrection_uuid on Autocorrection(uuid);"};

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

    public static boolean exists(double deliveredFastAmount, long timestamp) {
        fixUpTable();
        List<Autocorrection> markers = new Select()
                .from(Autocorrection.class)
                .where("timestamp = ? and deliveredFastAmount = ?", timestamp, deliveredFastAmount)
                .orderBy("timestamp desc")
                .execute();
        return !markers.isEmpty();
    }

    public static synchronized void create(double deliveredFastAmount, long timestamp) {
        fixUpTable();
        final Autocorrection marker = new Autocorrection();
        marker.timestamp = timestamp;
        marker.uuid = UUID.randomUUID().toString();
        marker.deliveredFastAmount = deliveredFastAmount;
        marker.created_at = DateUtil.toISOString(timestamp);
        marker.save();
    }

    public static double getMaxAmount(final long startTime, final long endTime) {
        fixUpTable();
        Autocorrection maxAutocorrection = new Select()
                .from(Autocorrection.class)
                .where("timestamp >= ? and timestamp <= ?", startTime, endTime)
                .orderBy("deliveredFastAmount desc")
                .executeSingle();

        return maxAutocorrection != null ? maxAutocorrection.deliveredFastAmount : 0.0;
    }

    public static Autocorrection last() {
        fixUpTable();
        return new Select()
                .from(Autocorrection.class)
                .orderBy("_ID desc")
                .executeSingle();
    }

    public static List<Autocorrection> latest(int amount) {
        fixUpTable();
        return new Select()
                .from(Autocorrection.class)
                .orderBy("timestamp desc")
                .limit(amount)
                .execute();
    }

    public static Autocorrection byUuid(String uuid) {
        if (uuid == null) {
            return null;
        }
        fixUpTable();
        return new Select()
                .from(Autocorrection.class)
                .where("uuid = ?", uuid)
                .orderBy("_ID desc")
                .executeSingle();
    }

    public static Autocorrection byId(long id) {
        fixUpTable();
        return new Select()
                .from(Autocorrection.class)
                .where("_ID = ?", id)
                .executeSingle();
    }

    public static Autocorrection byTimestamp(long timestamp) {
        return byTimestamp(timestamp, 1500);
    }

    public static Autocorrection byTimestamp(long timestamp, int accuracy) {
        fixUpTable();
        return new Select()
                .from(Autocorrection.class)
                .where("timestamp <= ? and timestamp >= ?", (timestamp + accuracy), (timestamp - accuracy)) // window
                .orderBy("abs(timestamp-" + Long.toString(timestamp) + ") asc")
                .executeSingle();
    }

    public static List<Autocorrection> listByTimestamp(long timestamp) {
        fixUpTable();
        return new Select()
                .from(Autocorrection.class)
                .where("timestamp = ?", timestamp)
                .orderBy("timestamp desc")
                .execute();
    }

    public static void deleteAll() {
        fixUpTable();
        new Delete()
                .from(Autocorrection.class)
                .execute();
        Home.staticRefreshBGCharts();
        // not synced with uploader queue - should we?
    }

    public static void deleteByTimestamp(long timestamp, int accuracy) {
        final Autocorrection marker = byTimestamp(timestamp, accuracy); // do we need to alter default accuracy?
        if (marker != null) {
            Log.d(TAG, "Deleting treatment closest to: " + JoH.dateTimeText(timestamp) + " matches uuid: " + marker.uuid);
            deleteByUuid(marker.uuid);
        } else {
            Log.e(TAG, "Couldn't find a treatment near enough to " + JoH.dateTimeText(timestamp) + " to delete!");
        }
    }

    public static void deleteByUuid(String uuid) {
        Autocorrection marker = byUuid(uuid);
        if (marker != null) {
            marker.delete();
            Home.staticRefreshBGCharts();
        }
    }

    public static void deleteLast() {
        Autocorrection marker = last();
        if (marker != null) {
            marker.delete();
            Home.staticRefreshBGCharts();
        }
    }

    public static void cleanup(final int retentionDays) {
        fixUpTable();
        new Delete()
                .from(Autocorrection.class)
                .where("timestamp < ?", JoH.tsl() - (retentionDays * Constants.DAY_IN_MS))
                .execute();
    }

    public static List<Autocorrection> latestForGraph(int number, double startTime) {
        return latestForGraph(number, startTime, JoH.ts());
    }

    public static List<Autocorrection> latestForGraph(int number, double startTime, double endTime) {
        fixUpTable();
        DecimalFormat df = new DecimalFormat("#");
        df.setMaximumFractionDigits(1); // are there decimal points in the database??
        return new Select()
                .from(Autocorrection.class)
                .where("timestamp >= ? and timestamp <= ?", df.format(startTime), df.format(endTime))
                .orderBy("timestamp asc")
                .limit(number)
                .execute();
    }

    public static List<Autocorrection> latestForGraph(final int number, final long startTime, final long endTime) {
        fixUpTable();
        return new Select()
                .from(Autocorrection.class)
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



