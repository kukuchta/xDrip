package com.eveningoutpost.dexdrip.models;

/*
 * Created by jamorham on 31/12/15.
 */

import android.content.Context;
import android.provider.BaseColumns;

import androidx.annotation.Nullable;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.activeandroid.util.SQLiteUtils;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.UndoRedo;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.internal.bind.DateTypeAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.eveningoutpost.dexdrip.models.JoH.msSince;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.HOUR_IN_MS;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.MINUTE_IN_MS;
import static com.eveningoutpost.dexdrip.models.JoH.emptyString;

@Table(name = "Treatments", id = BaseColumns._ID)
public class Treatments extends Model {
    private static final String TAG = Treatments.class.getSimpleName();

    public static final String SENSOR_START_EVENT_TYPE = "Sensor Start";
    public static final String SENSOR_STOP_EVENT_TYPE = "Sensor Stop";
    private static final String DEFAULT_EVENT_TYPE = "<none>";
    public static final String DELIVERY_TYPE_NONE = "None";
    public static final String DELIVERY_TYPE_BOLUS = "Bolus";
    public static final String DELIVERY_TYPE_AUTOCORRECTION = "Autocorrection";
    public static final String DELIVERY_TYPE_AUTO_BASAL_DELIVERY = "AutoBasal";
    public final static String XDRIP_TAG = "xdrip";
    private static boolean patched = false;

    @Expose
    @Column(name = "timestamp", index = true)
    public long timestamp;
    @Expose
    @Column(name = "eventType")
    public String eventType;
    @Expose
    @Column(name = "enteredBy")
    public String enteredBy;
    @Expose
    @Column(name = "notes")
    public String notes;
    @Expose
    @Column(name = "uuid", unique = true, onUniqueConflicts = Column.ConflictAction.IGNORE)
    public String uuid;
    @Expose
    @Column(name = "carbs")
    public double carbs;
    @Expose
    @Column(name = "deliveryType")
    public String deliveryType;
    @Expose
    @Column(name = "insulinFastAmount")
    public double insulinFastAmount;
    @Expose
    @Column(name = "insulinExtendedAmount")
    public double insulinExtendedAmount;
    @Expose
    @Column(name = "insulinJSON")
    public String insulinJSON;
    @Expose
    @Column(name = "created_at")
    public String created_at;

    public double getTotalInsulinAmount(){
        return insulinFastAmount + insulinExtendedAmount;
    }

    public Treatments()
    {
        eventType = DEFAULT_EVENT_TYPE;
        carbs = 0;
        deliveryType = DELIVERY_TYPE_NONE;
        insulinFastAmount = 0;
        insulinExtendedAmount = 0;
        //setInsulinInjections(null);
    }

    public static boolean bolusExists(double insulinFastAmount, double insulinExtendedAmount, long timestamp) {
        fixUpTable();
        List<Treatments> treatments = new Select()
                .from(Treatments.class)
                .where("timestamp = ? and deliveryType = ? and insulinFastAmount = ? and insulinExtendedAmount = ?", timestamp, DELIVERY_TYPE_BOLUS, insulinFastAmount, insulinExtendedAmount)
                .orderBy("timestamp desc")
                .execute();
        return !treatments.isEmpty();
    }

    public static boolean autocorrectionExists(double insulinFastAmount, long timestamp) {
        fixUpTable();
        List<Treatments> treatments = new Select()
                .from(Treatments.class)
                .where("timestamp = ? and deliveryType = ? and insulinFastAmount = ? and insulinExtendedAmount = ?", timestamp, DELIVERY_TYPE_AUTOCORRECTION, insulinFastAmount, 0)
                .orderBy("timestamp desc")
                .execute();
        return !treatments.isEmpty();
    }

    public static boolean autoBasalDeliveryExists(double insulinFastAmount, long timestamp) {
        fixUpTable();
        List<Treatments> treatments = new Select()
                .from(Treatments.class)
                .where("timestamp = ? and deliveryType = ? and insulinFastAmount = ? and insulinExtendedAmount = ?", timestamp, DELIVERY_TYPE_AUTO_BASAL_DELIVERY, insulinFastAmount, 0)
                .orderBy("timestamp desc")
                .execute();
        return !treatments.isEmpty();
    }

    public static boolean mealExists(double carbs, long timestamp) {
        fixUpTable();
        List<Treatments> treatments = new Select()
                .from(Treatments.class)
                .where("timestamp = ? and carbs = ?", timestamp, carbs)
                .orderBy("timestamp desc")
                .execute();
        return !treatments.isEmpty();
    }

    public static synchronized Treatments createBolus(double insulinFastAmount, double insulinExtendedAmount, long timestamp) {
        fixUpTable();
        final Treatments treatment = new Treatments();
        treatment.timestamp = timestamp;
        treatment.uuid = UUID.randomUUID().toString();
        treatment.deliveryType = DELIVERY_TYPE_BOLUS;
        treatment.insulinFastAmount = insulinFastAmount;
        treatment.insulinExtendedAmount = insulinExtendedAmount;
        treatment.carbs = 0;
        treatment.created_at = DateUtil.toISOString(timestamp);
        treatment.save();
        return treatment;
    }

    public static synchronized Treatments createAutocorrection(double insulinFastAmount, long timestamp) {
        fixUpTable();
        final Treatments treatment = new Treatments();
        treatment.timestamp = timestamp;
        treatment.uuid = UUID.randomUUID().toString();
        treatment.deliveryType = DELIVERY_TYPE_AUTOCORRECTION;
        treatment.insulinFastAmount = insulinFastAmount;
        treatment.insulinExtendedAmount = 0;
        treatment.carbs = 0;
        treatment.created_at = DateUtil.toISOString(timestamp);
        treatment.save();
        return treatment;
    }

    public static synchronized Treatments createAutoBasalDelivery(double insulinFastAmount, long timestamp) {
        fixUpTable();
        final Treatments treatment = new Treatments();
        treatment.timestamp = timestamp;
        treatment.uuid = UUID.randomUUID().toString();
        treatment.deliveryType = DELIVERY_TYPE_AUTO_BASAL_DELIVERY;
        treatment.insulinFastAmount = insulinFastAmount;
        treatment.insulinExtendedAmount = 0;
        treatment.carbs = 0;
        treatment.created_at = DateUtil.toISOString(timestamp);
        treatment.save();
        return treatment;
    }

    public static synchronized Treatments createMeal(double carbs, long timestamp) {
        fixUpTable();
        final Treatments treatment = new Treatments();
        treatment.timestamp = timestamp;
        treatment.uuid = UUID.randomUUID().toString();
        treatment.insulinFastAmount = 0;
        treatment.insulinExtendedAmount = 0;
        treatment.carbs = carbs;
        treatment.created_at = DateUtil.toISOString(timestamp);
        treatment.save();
        return treatment;
    }

    public static synchronized Treatments create(final double carbs, final double insulinSum, long timestamp) {
        return create(carbs, insulinSum, timestamp, null);
    }

    public static synchronized Treatments create(final double carbs, final double insulinSum, long timestamp, String suggested_uuid) {
        final long future_seconds = (timestamp - JoH.tsl()) / 1000;
        // if treatment more than 1 hour in the future
        if (future_seconds > (60 * 60)) {
            JoH.static_toast_long("Refusing to create a treatement more than 1 hours in the future!");
            return null;
        }
        // if treatment more than 3 minutes in the future
        if ((future_seconds > (3 * 60)) && (future_seconds < 86400) && ((carbs > 0) || (insulinSum > 0))) {
            final Context context = xdrip.getAppContext();
            JoH.scheduleNotification(context, "Treatment Reminder", "@" + JoH.hourMinuteString(timestamp) + " : "
                    + carbs + " g " + context.getString(R.string.carbs) + " / "
                    + insulinSum + " " + context.getString(R.string.units), (int) future_seconds, 34026);
        }
        return create(carbs, insulinSum, timestamp, -1, suggested_uuid);
    }

    public static synchronized Treatments create(final double carbs, final double insulinSum, long timestamp, double position, String suggested_uuid) {
        // TODO sanity check values
        Log.d(TAG, "Creating treatment: " +
                "Insulin: " + insulinSum + " / " +
                "Carbs: " + carbs +
                (suggested_uuid != null && !suggested_uuid.isEmpty()
                        ? " " + "uuid: " + suggested_uuid
                        : ""));

        if ((carbs == 0) && (insulinSum == 0)) return null;

        if (timestamp == 0) {
            timestamp = new Date().getTime();
        }

        final Treatments treatment = new Treatments();

        if (position > 0) {
            treatment.enteredBy = XDRIP_TAG + " pos:" + JoH.qs(position, 2);
        } else {
            treatment.enteredBy = XDRIP_TAG;
        }

        treatment.carbs = carbs;
        treatment.insulinFastAmount = insulinSum;
        treatment.timestamp = timestamp;
        treatment.created_at = DateUtil.toISOString(timestamp);
        treatment.uuid = suggested_uuid != null ? suggested_uuid : UUID.randomUUID().toString();
        treatment.save();

        UndoRedo.addUndoTreatment(treatment.uuid);
        return treatment;
    }

    // Note
    public static synchronized Treatments create_note(String note, long timestamp) {
        return create_note(note, timestamp, -1, null);
    }

    public static synchronized Treatments create_note(String note, long timestamp, double position) {
        return create_note(note, timestamp, position, null);
    }

    public static synchronized Treatments create_note(String note, long timestamp, double position, String suggested_uuid) {
        // TODO sanity check values
        Log.d(TAG, "Creating treatment note: " + note);

        if (timestamp == 0) {
            timestamp = new Date().getTime();
        }

        if ((note == null || (note.length() == 0))) {
            Log.i(TAG, "Empty treatment note - not saving");
            return null;
        }

        boolean is_new = false;
        // find treatment

        Treatments treatment = byTimestamp(timestamp, MINUTE_IN_MS * 5);
        // if unknown create
        if (treatment == null) {
            treatment = new Treatments();
            Log.d(TAG, "Creating new treatment entry for note");
            is_new = true;

            treatment.notes = note;
            treatment.timestamp = timestamp;
            treatment.created_at = DateUtil.toISOString(timestamp);
            treatment.uuid = suggested_uuid != null ? suggested_uuid : UUID.randomUUID().toString();

        } else {
            if (treatment.notes == null) treatment.notes = "";
            Log.d(TAG, "Found existing treatment for note: " + treatment.uuid + ((suggested_uuid != null) ? " vs suggested: " + suggested_uuid : "") + " distance:" + Long.toString(timestamp - treatment.timestamp) + " " + treatment.notes);
            if (treatment.notes.contains(note)) {
                Log.d(TAG, "Suggested note update already present - skipping");
                return null;
            }
            // append existing note or treatment
            if (treatment.notes.length() > 0) treatment.notes += " \u2192 ";
            treatment.notes += note;
            Log.d(TAG, "Final notes: " + treatment.notes);
        }
        //    if ((treatment.enteredBy == null) || (!treatment.enteredBy.contains(NightscoutUploader.VIA_NIGHTSCOUT_TAG))) {
        // tag it as from xdrip if it isn't being synced from nightscout right now to allow local updates to nightscout sourced notes
        if (suggested_uuid == null) {
            if (position > 0) {
                treatment.enteredBy = XDRIP_TAG + " pos:" + JoH.qs(position, 2);
            } else {
                treatment.enteredBy = XDRIP_TAG;
            }
        }

        treatment.save();
        if (is_new) UndoRedo.addUndoTreatment(treatment.uuid);

        return treatment;
    }

    static void createForTest(long timestamp, double insulin) {
        fixUpTable();
        Treatments treatment = new Treatments();
        treatment.notes = "test";
        treatment.timestamp = timestamp;
        treatment.created_at = DateUtil.toISOString(timestamp);
        treatment.uuid = UUID.randomUUID().toString();
        treatment.insulinFastAmount = insulin;
        treatment.save();
    }

    public static double getMaxAutocorrectionsAmount(final long startTime, final long endTime) {
        fixUpTable();
        Treatments maxAutocorrection = new Select()
                .from(Treatments.class)
                .where("timestamp >= ? and timestamp <= ? and deliveryType = ?", startTime, endTime, Treatments.DELIVERY_TYPE_AUTOCORRECTION)
                .orderBy("insulinFastAmount desc")
                .executeSingle();

        return maxAutocorrection != null ? maxAutocorrection.insulinFastAmount : 0.0;
    }

    public static double getMaxAutoBasalsAmount(final long startTime, final long endTime) {
        fixUpTable();
        Treatments maxAutocorrection = new Select()
                .from(Treatments.class)
                .where("timestamp >= ? and timestamp <= ? and deliveryType = ?", startTime, endTime, Treatments.DELIVERY_TYPE_AUTO_BASAL_DELIVERY)
                .orderBy("insulinFastAmount desc")
                .executeSingle();

        return maxAutocorrection != null ? maxAutocorrection.insulinFastAmount : 0.0;
    }

    /**
     * Returns a newly created treatment entry in the database for Sensor Start,
     * and pushes the new treatment to followers.
     * @param timestamp is optional, defaults to right now
     * @param notes is optional
     */
    public static synchronized Treatments sensorStart(@Nullable Long timestamp, @Nullable String notes) {
        if (timestamp == null || timestamp == 0) {
            timestamp = new Date().getTime();
        }

        final Treatments treatment = new Treatments();
        treatment.enteredBy = XDRIP_TAG;
        treatment.eventType = SENSOR_START_EVENT_TYPE;
        treatment.created_at = DateUtil.toISOString(timestamp);
        treatment.timestamp = timestamp;
        treatment.uuid = UUID.randomUUID().toString();
        if (notes != null && notes.length() > 0) {
            treatment.notes = notes;
        }
        treatment.save();
        return treatment;
    }

    /**
     * Returns a newly created treatment entry in the database for Sensor Stop,
     * and pushes the new treatment to followers.
     * @param timestamp is optional, defaults to right now
     * @param notes is optional
     */
    public static synchronized Treatments sensorStop(@Nullable Long timestamp, @Nullable String notes) {
        if (timestamp == null || timestamp == 0) {
            timestamp = new Date().getTime();
        }

        final Treatments treatment = new Treatments();
        treatment.enteredBy = XDRIP_TAG;
        treatment.eventType = SENSOR_STOP_EVENT_TYPE;
        treatment.created_at = DateUtil.toISOString(timestamp);
        treatment.timestamp = timestamp;
        treatment.uuid = UUID.randomUUID().toString();
        if (notes != null && notes.length() > 0) {
            treatment.notes = notes;
        }
        treatment.save();
        return treatment;
    }


    // This shouldn't be needed but it seems it is
    private static void fixUpTable() {
        if (patched) return;
        String[] patchup = {
                "CREATE TABLE Treatments (_id INTEGER PRIMARY KEY AUTOINCREMENT);",
                "ALTER TABLE Treatments ADD COLUMN timestamp INTEGER;",
                "ALTER TABLE Treatments ADD COLUMN uuid TEXT;",
                "ALTER TABLE Treatments ADD COLUMN eventType TEXT;",
                "ALTER TABLE Treatments ADD COLUMN enteredBy TEXT;",
                "ALTER TABLE Treatments ADD COLUMN notes TEXT;",
                "ALTER TABLE Treatments ADD COLUMN created_at TEXT;",
                "ALTER TABLE Treatments ADD COLUMN deliveryType TEXT;",
                "ALTER TABLE Treatments ADD COLUMN insulinFastAmount REAL;",
                "ALTER TABLE Treatments ADD COLUMN insulinExtendedAmount REAL;",
                "ALTER TABLE Treatments ADD COLUMN insulinJSON TEXT;",
                "ALTER TABLE Treatments ADD COLUMN carbs REAL;",
                "CREATE INDEX index_Treatments_timestamp on Treatments(timestamp);",
                "CREATE UNIQUE INDEX index_Treatments_uuid on Treatments(uuid);"};

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

    public static Treatments last() {
        fixUpTable();
        return new Select()
                .from(Treatments.class)
                .orderBy("_ID desc")
                .executeSingle();
    }

    public static Treatments lastNotFromXdrip() {
        fixUpTable();
        return new Select()
                .from(Treatments.class)
                .where("enteredBy NOT LIKE '" + XDRIP_TAG + "%'")
                .orderBy("_ID DESC")
                .executeSingle();
    }

    public static Treatments lastEventTypeFromXdrip(final String eventType) {
        fixUpTable();
        return new Select()
                .from(Treatments.class)
                .where("enteredBy LIKE '" + XDRIP_TAG + "%' and eventType = ?", eventType)
                .orderBy("_ID DESC")
                .executeSingle();       // TODO does the where clause order affect optimization ref database indexes?
    }

    public static List<Treatments> latest(int num) {
        try {
            return new Select()
                    .from(Treatments.class)
                    .orderBy("timestamp desc")
                    .limit(num)
                    .execute();
        } catch (android.database.sqlite.SQLiteException e) {
            fixUpTable();
            return null;
        }
    }

    public static Treatments byuuid(String uuid) {
        if (uuid == null) return null;
        return new Select()
                .from(Treatments.class)
                .where("uuid = ?", uuid)
                .orderBy("_ID desc")
                .executeSingle();
    }

    public static Treatments byid(long id) {
        return new Select()
                .from(Treatments.class)
                .where("_ID = ?", id)
                .executeSingle();
    }

    public static Treatments byTimestamp(long timestamp) {
        return byTimestamp(timestamp, 1500);
    }

    public static Treatments byTimestamp(long timestamp, long plus_minus_millis) {
        if (plus_minus_millis > Integer.MAX_VALUE) {
            throw new RuntimeException("Treatment by TimeStamp out of range value: " + plus_minus_millis);
        }
        return byTimestamp(timestamp, (int) plus_minus_millis);
    }

    public static Treatments byTimestamp(long timestamp, int plus_minus_millis) {
        return new Select()
                .from(Treatments.class)
                .where("timestamp <= ? and timestamp >= ?", (timestamp + plus_minus_millis), (timestamp - plus_minus_millis)) // window
                .orderBy("abs(timestamp-" + Long.toString(timestamp) + ") asc")
                .executeSingle();
    }

    public static List<Treatments> listByTimestamp(long timestamp) {
        return new Select()
                .from(Treatments.class)
                .where("timestamp = ?", timestamp)
                .orderBy("timestamp desc")
                .execute();
    }

    public static void delete_all() {
        delete_all(false);
    }

    public static void delete_all(boolean from_interactive) {
        new Delete()
                .from(Treatments.class)
                .execute();
        // not synced with uploader queue - should we?
    }

    public static Treatments delete_last() {
        return delete_last(false);
    }

    public static void delete_by_timestamp(long timestamp) {
        delete_by_timestamp(timestamp, 1500, false);
    }

    public static void delete_by_timestamp(long timestamp, int accuracy, boolean from_interactive) {
        final Treatments t = byTimestamp(timestamp, accuracy); // do we need to alter default accuracy?
        if (t != null) {
            Log.d(TAG, "Deleting treatment closest to: " + JoH.dateTimeText(timestamp) + " matches uuid: " + t.uuid);
            delete_by_uuid(t.uuid, from_interactive);
        } else {
            Log.e(TAG, "Couldn't find a treatment near enough to " + JoH.dateTimeText(timestamp) + " to delete!");
        }
    }

    public static void delete_by_uuid(String uuid) {
        delete_by_uuid(uuid, false);
    }

    public static void delete_by_uuid(String uuid, boolean from_interactive) {
        Treatments thistreat = byuuid(uuid);
        if (thistreat != null) {

            thistreat.delete();
            Home.staticRefreshBGCharts();
        }
    }

    public static Treatments delete_last(boolean from_interactive) {
        Treatments thistreat = last();
        if (thistreat != null) {
            thistreat.delete();
        }
        return null;
    }

    public static void cleanup(final int retention_days) {
        fixUpTable();
        new Delete()
                .from(Treatments.class)
                .where("timestamp < ?", JoH.tsl() - (retention_days * Constants.DAY_IN_MS))
                .execute();
    }

    public static Treatments fromJSON(String json) {
        try {
            return new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().fromJson(json, Treatments.class);
        } catch (Exception e) {
            Log.d(TAG, "Got exception parsing treatment json: " + e.toString());
            Home.toaststatic("Error on treatment, probably decryption key mismatch");
            return null;
        }
    }

    public static synchronized boolean pushTreatmentFromJson(String json) {
        return pushTreatmentFromJson(json, false);
    }

    public static synchronized boolean pushTreatmentFromJson(String json, boolean from_interactive) {
        Log.d(TAG, "converting treatment from json: " + json);
        final Treatments mytreatment = fromJSON(json);
        if (mytreatment != null) {
            if ((mytreatment.carbs == 0) && (mytreatment.getTotalInsulinAmount() == 0)
                    && (mytreatment.notes != null) && (mytreatment.notes.startsWith("AndroidAPS started"))) {
                Log.d(TAG, "Skipping AndroidAPS started message");
                return false;
            }
            if ((mytreatment.eventType != null) && (mytreatment.eventType.equals("Temp Basal"))) {
                // we don't yet parse or process these
                Log.d(TAG, "Skipping Temp Basal msg");
                return false;
            }
            if (mytreatment.timestamp < 1) {
                Log.e(TAG, "Invalid treatment timestamp or 0 or less");
                return false;
            }

            if (mytreatment.uuid == null) {
                try {
                    final JSONObject jsonobj = new JSONObject(json);
                    if (jsonobj.has("_id")) mytreatment.uuid = jsonobj.getString("_id");
                } catch (JSONException e) {
                    //
                }
                if (mytreatment.uuid == null) mytreatment.uuid = UUID.randomUUID().toString();
            }
            // anything received +- 1500 ms is going to be treated as a duplicate
            final Treatments dupe_treatment = byTimestamp(mytreatment.timestamp);
            if (dupe_treatment != null) {
                Log.i(TAG, "Duplicate treatment for: " + mytreatment.timestamp);

                if ((dupe_treatment.getTotalInsulinAmount() == 0) && (mytreatment.getTotalInsulinAmount() > 0)) {
                    dupe_treatment.deliveryType = mytreatment.deliveryType;
                    dupe_treatment.insulinFastAmount = mytreatment.insulinFastAmount;
                    dupe_treatment.insulinExtendedAmount = mytreatment.insulinExtendedAmount;
                    dupe_treatment.save();
                    Home.staticRefreshBGChartsOnIdle();
                }

                if ((dupe_treatment.carbs == 0) && (mytreatment.carbs > 0)) {
                    dupe_treatment.carbs = mytreatment.carbs;
                    dupe_treatment.save();
                    Home.staticRefreshBGChartsOnIdle();
                }

                if ((dupe_treatment.uuid != null) && (mytreatment.uuid != null) && (dupe_treatment.uuid.equals(mytreatment.uuid)) && (mytreatment.notes != null)) {

                    if ((dupe_treatment.notes == null) || (dupe_treatment.notes.length() < mytreatment.notes.length())) {
                        dupe_treatment.notes = mytreatment.notes;
                        fixUpTable();
                        dupe_treatment.save();
                        Log.d(TAG, "Saved updated treatement notes");
                        // should not end up needing to append notes and be from_interactive via undo as these
                        // would be mutually exclusive operations so we don't need to handle that here.
                        Home.staticRefreshBGChartsOnIdle();
                    }
                }

                return false;
            }
            Log.d(TAG, "Saving pushed treatment: " + mytreatment.uuid);
            if ((mytreatment.enteredBy == null) || (mytreatment.enteredBy.equals(""))) {
                mytreatment.enteredBy = "sync";
            }
            if ((mytreatment.eventType == null) || (mytreatment.eventType.equals(""))) {
                mytreatment.eventType = DEFAULT_EVENT_TYPE; // should have a default
            }
            if ((mytreatment.created_at == null) || (mytreatment.created_at.equals(""))) {
                try {
                    mytreatment.created_at = DateUtil.toISOString(mytreatment.timestamp); // should have a default
                } catch (Exception e) {
                    Log.e(TAG, "Could not convert timestamp to isostring");
                }
            }

            fixUpTable();
            long x = mytreatment.save();
            Home.staticRefreshBGChartsOnIdle();
            return true;
        } else {
            return false;
        }
    }

    public static List<Treatments> latestForGraph(int number, double startTime) {
        return latestForGraph(number, startTime, JoH.ts());
    }

    public static List<Treatments> latestForGraph(int number, double startTime, double endTime) {
        fixUpTable();
        DecimalFormat df = new DecimalFormat("#");
        df.setMaximumFractionDigits(1); // are there decimal points in the database??
        return new Select()
                .from(Treatments.class)
                .where("timestamp >= ? and timestamp <= ?", df.format(startTime), df.format(endTime))
                .orderBy("timestamp asc")
                .limit(number)
                .execute();
    }

    public static List<Treatments> latestForGraph(final int number, final long startTime, final long endTime) {
        fixUpTable();
        return new Select()
                .from(Treatments.class)
                .where("timestamp >= ? and timestamp <= ?", startTime, endTime)
                .orderBy("timestamp asc")
                .limit(number)
                .execute();
    }

    public static List<Treatments> latestAutoBasalsForGraph(final int number, final long startTime, final long endTime) {
        fixUpTable();
        return new Select()
                .from(Treatments.class)
                .where("timestamp >= ? and timestamp <= ? and deliveryType = ?", startTime, endTime, Treatments.DELIVERY_TYPE_AUTO_BASAL_DELIVERY)
                .orderBy("timestamp asc")
                .limit(number)
                .execute();
    }

    public static List<Treatments> latestAutocorrectionsForGraph(final int number, final long startTime, final long endTime) {
        fixUpTable();
        return new Select()
                .from(Treatments.class)
                .where("timestamp >= ? and timestamp <= ? and deliveryType = ?", startTime, endTime, Treatments.DELIVERY_TYPE_AUTOCORRECTION)
                .orderBy("timestamp asc")
                .limit(number)
                .execute();
    }

    public static long getTimeStampWithOffset(double offset) {
        //  optimisation instead of creating a new date each time?
        return (long) (new Date().getTime() - offset);
    }

    /// this is no longer used
    /* public static CobCalc cobCalc(Treatments treatment, double lastDecayedBy, double time) {

        double delay = 20; // minutes till carbs start decaying

        double delayms = delay * Constants.MINUTE_IN_MS;
        if (treatment.carbs > 0) {

            CobCalc thisCobCalc = new CobCalc();
            thisCobCalc.carbTime = treatment.timestamp;

            // no previous carb treatment? Set to our start time
            if (lastDecayedBy == 0) {
                lastDecayedBy = thisCobCalc.carbTime;
            }

            double carbs_hr = Profile.getCarbAbsorptionRate(time);
            double carbs_min = carbs_hr / 60;
            double carbs_ms = carbs_min / Constants.MINUTE_IN_MS;

            thisCobCalc.decayedBy = thisCobCalc.carbTime; // initially set to start time for this treatment

            double minutesleft = (lastDecayedBy - thisCobCalc.carbTime) / Constants.MINUTE_IN_MS;
            double how_long_till_carbs_start_ms = (lastDecayedBy - thisCobCalc.carbTime);
            thisCobCalc.decayedBy += (Math.max(delay, minutesleft) + treatment.carbs / carbs_min) * Constants.MINUTE_IN_MS;

            if (delay > minutesleft) {
                thisCobCalc.initialCarbs = treatment.carbs;
            } else {
                thisCobCalc.initialCarbs = treatment.carbs + minutesleft * carbs_min;
            }
            double startDecay = thisCobCalc.carbTime + (delay * Constants.MINUTE_IN_MS);

            if (time < lastDecayedBy || time > startDecay) {
                thisCobCalc.isDecaying = 1;
            } else {
                thisCobCalc.isDecaying = 0;
            }
            return thisCobCalc;

        } else {
            return null;
        }
    }
*/











    public String getBestShortText() {
        if (!eventType.equals(DEFAULT_EVENT_TYPE)) {
            return eventType;
        } else {
            return noteHasContent() ? notes : "Treatment";
        }
    }

    public String toJSON() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("uuid", uuid);
            jsonObject.put("deliveryType", deliveryType);
            jsonObject.put("insulinFastAmount", insulinFastAmount);
            jsonObject.put("insulinExtendedAmount", insulinExtendedAmount);
            jsonObject.put("insulinJSON", insulinJSON);
            jsonObject.put("carbs", carbs);
            jsonObject.put("timestamp", timestamp);
            jsonObject.put("notes", notes);
            jsonObject.put("enteredBy", enteredBy);
            return jsonObject.toString();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return "";
        }
    }

    private static final double MAX_SMB_UNITS = 0.3;
    private static final double MAX_OPENAPS_SMB_UNITS = 0.4;

    public boolean likelySMB() {
        return (carbs == 0 && insulinFastAmount > 0 && insulinExtendedAmount == 0
                && ((insulinFastAmount <= MAX_SMB_UNITS && (notes == null || notes.isEmpty())) || (enteredBy != null && enteredBy.startsWith("openaps:") && insulinFastAmount <= MAX_OPENAPS_SMB_UNITS)));
    }

    public boolean wasCreatedRecently() {
        return msSince(DateUtil.tolerantFromISODateString(created_at).getTime()) < HOUR_IN_MS * 12;
    }

    public boolean noteOnly() {
        return carbs == 0 && getTotalInsulinAmount() == 0 && noteHasContent();
    }

    public boolean hasContent() {
        return getTotalInsulinAmount() != 0 || carbs != 0 || noteHasContent() || !isEventTypeDefault();
    }

    public boolean noteHasContent() {
        return notes != null && notes.length() > 0;
    }

    public boolean isEventTypeDefault() {
        return eventType == null || eventType.equalsIgnoreCase(DEFAULT_EVENT_TYPE);
    }

    public static boolean matchUUID(final List<Treatments> treatments, final String uuid) {
        for (final Treatments treatment : treatments) {
            if (treatment.uuid.equalsIgnoreCase(uuid)) return true;
        }
        return false;
    }

    public String toS() {
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .registerTypeAdapter(Date.class, new DateTypeAdapter())
                .serializeSpecialFloatingPointValues()
                .create();
        return gson.toJson(this);
    }

    public boolean isPenSyncedDose() {
        return notes != null && notes.startsWith("PEN");
    }

    public String getPenSerial() {
        if (isPenSyncedDose()) {
            final Pattern penPattern = Pattern.compile(".*PEN ([A-Z0-9]+).*", Pattern.DOTALL);
            final Matcher m = penPattern.matcher(notes);
            if (m.matches()) {
                return m.group(1);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public boolean isPrimingDose() {
        return notes != null && notes.startsWith("Priming");
    }
}



