package com.eveningoutpost.dexdrip.models;

import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.activeandroid.util.SQLiteUtils;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.google.gson.annotations.Expose;

import java.text.DecimalFormat;
import java.util.List;


@Table(name = "PumpBasal", id = BaseColumns._ID)
public class PumpBasal extends Model {
    private static boolean patched = false;

    @Expose
    @Column(name = "timestamp", index = true)
    public long timestamp;
    @Expose
    @Column(name = "basalRate")
    public double basalRate;
    @Expose
    @Column(name = "autoBasalDelivery")
    public double autoBasalDelivery;



    public PumpBasal()
    {
        basalRate = 0.0;
        autoBasalDelivery = 0.0;
    }

    public static boolean basalRateExists(final double basalRate, final long timestamp) {
        fixUpTable();
        List<PumpBasal> pumpBasals = new Select()
                .from(PumpBasal.class)
                .where("timestamp = ? and basalRate = ?", timestamp, basalRate)
                .orderBy("timestamp desc")
                .execute();
        return !pumpBasals.isEmpty();
    }

    public static synchronized void createBasalRate(final double basalRate, long timestamp) {
        if (basalRate == 0) {
            return;
        }

        fixUpTable();
        final PumpBasal pumpBasal = new PumpBasal();
        pumpBasal.basalRate = basalRate;
        pumpBasal.autoBasalDelivery = 0.0;
        pumpBasal.timestamp = timestamp;
        pumpBasal.save();
    }

    public static boolean autoBasalDeliveryExists(final double bolusAmount, final long timestamp) {
        fixUpTable();
        List<PumpBasal> pumpBasals = new Select()
                .from(PumpBasal.class)
                .where("timestamp = ? and autoBasalDelivery = ?", timestamp, bolusAmount)
                .orderBy("timestamp desc")
                .execute();
        return !pumpBasals.isEmpty();
    }

    public static synchronized void createAutoBasalDelivery(final double bolusAmount, final long timestamp) {
        if (bolusAmount == 0) {
            return;
        }

        fixUpTable();
        final PumpBasal pumpBasal = new PumpBasal();
        pumpBasal.basalRate = 0.0;
        pumpBasal.autoBasalDelivery = bolusAmount;
        pumpBasal.timestamp = timestamp;
        pumpBasal.save();
    }

    private static void fixUpTable() {
        if (patched) return;
        String[] patchup = {
                "CREATE TABLE PumpBasal (_id INTEGER PRIMARY KEY AUTOINCREMENT);",
                "ALTER TABLE PumpBasal ADD COLUMN timestamp INTEGER;",
                "ALTER TABLE PumpBasal ADD COLUMN basalRate REAL;",
                "ALTER TABLE PumpBasal ADD COLUMN autoBasalDelivery REAL;",
                "CREATE INDEX index_PumpBasal_timestamp on PumpBasal(timestamp);"};

        for (String patch : patchup) {
            try {
                SQLiteUtils.execSql(patch);
            }
            catch (Exception ignored) { }
        }
        patched = true;
    }

    public static PumpBasal last() {
        fixUpTable();
        return new Select()
                .from(PumpBasal.class)
                .orderBy("_ID desc")
                .executeSingle();
    }

    public static List<PumpBasal> latest(int num) {
        fixUpTable();
        return new Select()
            .from(PumpBasal.class)
            .orderBy("timestamp desc")
            .limit(num)
            .execute();
    }

    public static void cleanup(final int retention_days) {
        fixUpTable();
        new Delete()
                .from(PumpBasal.class)
                .where("timestamp < ?", JoH.tsl() - (retention_days * Constants.DAY_IN_MS))
                .execute();
    }

    public static List<PumpBasal> latestAutoBasalDeliveryForGraph(final int number, final long startTime, final long endTime) {
        fixUpTable();
        return new Select()
                .from(PumpBasal.class)
                .where("timestamp >= ? and timestamp <= ? and autoBasalDelivery > ?", startTime, endTime, 0)
                .orderBy("timestamp asc")
                .limit(number)
                .execute();
    }

    public static List<PumpBasal> latestBasalRateForGraph(final int number, final long startTime, final long endTime) {
        fixUpTable();
        return new Select()
                .from(PumpBasal.class)
                .where("timestamp >= ? and timestamp <= ? and basalRate > ?", startTime, endTime, 0)
                .orderBy("timestamp asc")
                .limit(number)
                .execute();
    }

    public static double getMaxAutoBasalDelivery(final long startTime, final long endTime) {
        fixUpTable();
        PumpBasal maxAutoBasalDelivery = new Select()
                .from(PumpBasal.class)
                .where("timestamp >= ? and timestamp <= ? and autoBasalDelivery > ?", startTime, endTime, 0)
                .orderBy("autoBasalDelivery desc")
                .executeSingle();

        return maxAutoBasalDelivery != null ? maxAutoBasalDelivery.autoBasalDelivery : 0.0;
    }
}



