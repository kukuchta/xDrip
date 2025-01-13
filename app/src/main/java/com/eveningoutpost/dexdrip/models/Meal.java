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

@Table(name = "Meal", id = BaseColumns._ID)
public class Meal extends Model {

    private static final String TAG = Meal.class.getSimpleName();
    private static boolean patched = false;


    @Expose
    @Column(name = "timestamp", index = true)
    public long timestamp;

    @Expose
    @Column(name = "uuid", unique = true, onUniqueConflicts = Column.ConflictAction.IGNORE)
    public String uuid;

    @Expose
    @Column(name = "carbs")
    public double carbs;

    @Expose
    @Column(name = "created_at")
    public String created_at;

    public Meal()
    {
        carbs = 0d;
    }

    private static void fixUpTable() {
        if (patched) {
            return;
        }

        String[] commands = {
                "CREATE TABLE Meal (_id INTEGER PRIMARY KEY AUTOINCREMENT);",
                "ALTER TABLE Meal ADD COLUMN timestamp INTEGER;",
                "ALTER TABLE Meal ADD COLUMN uuid TEXT;",
                "ALTER TABLE Meal ADD COLUMN carbs REAL;",
                "ALTER TABLE Meal ADD COLUMN created_at TEXT;",
                "CREATE INDEX index_Meal_timestamp on Meal(timestamp);",
                "CREATE UNIQUE INDEX index_Meal_uuid on Meal(uuid);"};

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

    public static boolean exists(double carbs, long timestamp) {
        fixUpTable();
        List<Meal> markers = new Select()
                .from(Meal.class)
                .where("timestamp = ? and carbs = ?", timestamp, carbs)
                .orderBy("timestamp desc")
                .execute();
        return !markers.isEmpty();
    }

    public static synchronized void create(double carbs, long timestamp) {
        fixUpTable();
        final Meal marker = new Meal();
        marker.timestamp = timestamp;
        marker.uuid = UUID.randomUUID().toString();
        marker.carbs = carbs;
        marker.created_at = DateUtil.toISOString(timestamp);
        marker.save();
    }



    public static Meal last() {
        fixUpTable();
        return new Select()
                .from(Meal.class)
                .orderBy("_ID desc")
                .executeSingle();
    }

    public static List<Meal> latest(int amount) {
        fixUpTable();
        return new Select()
                .from(Meal.class)
                .orderBy("timestamp desc")
                .limit(amount)
                .execute();
    }

    public static Meal byUuid(String uuid) {
        if (uuid == null) {
            return null;
        }
        fixUpTable();
        return new Select()
                .from(Meal.class)
                .where("uuid = ?", uuid)
                .orderBy("_ID desc")
                .executeSingle();
    }

    public static Meal byId(long id) {
        fixUpTable();
        return new Select()
                .from(Meal.class)
                .where("_ID = ?", id)
                .executeSingle();
    }

    public static Meal byTimestamp(long timestamp) {
        return byTimestamp(timestamp, 1500);
    }

    public static Meal byTimestamp(long timestamp, int accuracy) {
        fixUpTable();
        return new Select()
                .from(Meal.class)
                .where("timestamp <= ? and timestamp >= ?", (timestamp + accuracy), (timestamp - accuracy)) // window
                .orderBy("abs(timestamp-" + Long.toString(timestamp) + ") asc")
                .executeSingle();
    }

    public static List<Meal> listByTimestamp(long timestamp) {
        fixUpTable();
        return new Select()
                .from(Meal.class)
                .where("timestamp = ?", timestamp)
                .orderBy("timestamp desc")
                .execute();
    }

    public static void deleteAll() {
        fixUpTable();
        new Delete()
                .from(Meal.class)
                .execute();
        Home.staticRefreshBGCharts();
        // not synced with uploader queue - should we?
    }

    public static void deleteByTimestamp(long timestamp, int accuracy) {
        final Meal marker = byTimestamp(timestamp, accuracy); // do we need to alter default accuracy?
        if (marker != null) {
            Log.d(TAG, "Deleting treatment closest to: " + JoH.dateTimeText(timestamp) + " matches uuid: " + marker.uuid);
            deleteByUuid(marker.uuid);
        } else {
            Log.e(TAG, "Couldn't find a treatment near enough to " + JoH.dateTimeText(timestamp) + " to delete!");
        }
    }

    public static void deleteByUuid(String uuid) {
        Meal marker = byUuid(uuid);
        if (marker != null) {
            marker.delete();
            Home.staticRefreshBGCharts();
        }
    }

    public static void deleteLast() {
        Meal marker = last();
        if (marker != null) {
            marker.delete();
            Home.staticRefreshBGCharts();
        }
    }

    public static void cleanup(final int retentionDays) {
        fixUpTable();
        new Delete()
                .from(Meal.class)
                .where("timestamp < ?", JoH.tsl() - (retentionDays * Constants.DAY_IN_MS))
                .execute();
    }

    public static List<Meal> latestForGraph(int number, double startTime) {
        return latestForGraph(number, startTime, JoH.ts());
    }

    public static List<Meal> latestForGraph(int number, double startTime, double endTime) {
        fixUpTable();
        DecimalFormat df = new DecimalFormat("#");
        df.setMaximumFractionDigits(1); // are there decimal points in the database??
        return new Select()
                .from(Meal.class)
                .where("timestamp >= ? and timestamp <= ?", df.format(startTime), df.format(endTime))
                .orderBy("timestamp asc")
                .limit(number)
                .execute();
    }

    public static List<Meal> latestForGraph(final int number, final long startTime, final long endTime) {
        fixUpTable();
        return new Select()
                .from(Meal.class)
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



