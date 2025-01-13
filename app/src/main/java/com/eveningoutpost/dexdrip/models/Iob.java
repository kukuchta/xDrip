package com.eveningoutpost.dexdrip.models;

import static com.eveningoutpost.dexdrip.utilitymodels.Constants.HOUR_IN_MS;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.MINUTE_IN_MS;

import android.util.Pair;

import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by jamorham on 02/01/16.
 */
public class Iob {
    private static final String TAG = Treatments.class.getSimpleName();

    private static double stored_default_carb_delay_minutes = 15;
    public long timestamp;
    public double iob = 0;
    public CobCalc cobCalc;
    public double cob = 0;
    public double jCarbImpact = 0;
    public double jActivity = 0;

    public static List<Iob> ioBForGraph_new(long startTime) {

        // 10 hours max look or from insulin manager if enabled
        final double dontLookThisFar = 10 * HOUR_IN_MS;

        // look back the longest effect period of all enabled insulin profiles (startTime is always 24h behind NOW)
        List<Treatments> theTreatments = Treatments.latestForGraph(2000, startTime - dontLookThisFar);
        UserError.Log.d(TAG,"TREATMENT LIST: "+theTreatments.size()+" "+JoH.dateTimeText((long)(startTime - dontLookThisFar)));
        if (theTreatments.isEmpty()) {
            return null;
        }

        int counter = 0; // iteration counter

        final double step_minutes = 5;
        final long stepms = (long) (step_minutes * MINUTE_IN_MS); // 300s = 5 mins
        long mytime = startTime;
        long tendtime = startTime;


        final double carb_delay_minutes = stored_default_carb_delay_minutes;
        final double carb_delay_ms_stepped = ((long) (carb_delay_minutes / step_minutes)) * step_minutes * MINUTE_IN_MS;

        UserError.Log.d(TAG, "Carb delay ms: " + carb_delay_ms_stepped);

        // linear array populated as needed and layered by each treatment etc
        SortedMap<Long, Iob> timeslices = new TreeMap<>();
        Iob calcreply;

        // First process all IoB calculations
        for (Treatments thisTreatment : theTreatments) {
            // early optimisation exclusion

            mytime = (long) ((thisTreatment.timestamp / stepms) * stepms); // effects of treatment occur only after it is given / fit to slot time
            tendtime = mytime + 36 * HOUR_IN_MS;     // 36 hours max look (24h history plus 12h forecast)
            if (tendtime > startTime + 30 * HOUR_IN_MS)
                tendtime = startTime + 30 * HOUR_IN_MS;   // dont look more than 6h in future // TODO review time limit
            if (thisTreatment.insulinFastAmount > 0) {
                // lay down insulin on board
                do {

                    calcreply = calcTreatment(thisTreatment.timestamp, thisTreatment.insulinFastAmount, mytime);

                    if (mytime >= startTime) {
                        timesliceInsulinWriter(timeslices, calcreply, mytime);
                    }
                    mytime = mytime + stepms; // advance time counter
                } while ((mytime < tendtime) &&
                        ((calcreply.iob == 0) || (calcreply.iob > 0.01)));
            }
        } // per insulin treatment
        return new ArrayList<Iob>(timeslices.values());
    }

    private static Iob calcTreatment(final long treatmentTimestamp, final double treatmentInsulin, final long time) {
        final Iob response = new Iob();

        Pair<Double, Double> result = calculateLegacyIobActivityFromTreatmentAtTime(treatmentTimestamp, treatmentInsulin, time);
        response.iob = result.first;
        // response.jActivity = result.second;

        return response;
    }

    private static void timesliceInsulinWriter(Map<Long, Iob> timeslices, Iob thisiob, long thistime) {
        if (thisiob.iob > 0) {
            if (timeslices.containsKey(thistime)) {
                Iob tempiob = timeslices.get(thistime);
                tempiob.iob += thisiob.iob;
                timeslices.put(thistime, tempiob);
            } else {
                thisiob.timestamp = (long) thistime;
                //   thisiob.date = new Date((long)thistime);
                timeslices.put(thistime, thisiob); // first entry at timeslice so put the record in as is
            }
        }
    }

    private static void timesliceCarbWriter(Map<Long, Iob> timeslices, long thistime, double carbs) {
        // offset for carb action time??
        Iob tempiob;
        if (timeslices.containsKey(thistime)) {
            tempiob = timeslices.get(thistime);
            tempiob.cob = tempiob.cob + carbs;
        } else {
            tempiob = new Iob();
            tempiob.timestamp = (long) thistime;
            //   tempiob.date = new Date((long)thistime);
            tempiob.cob = carbs;
        }
        timeslices.put(thistime, tempiob);
    }

    // using the original calculation
    private static Pair<Double, Double> calculateLegacyIobActivityFromTreatmentAtTime(final long treatmentTimestamp, final double treatmentInsulin, final long time) {

        final double dia = 3.0; // duration insulin action in hours
        final double peak = 75; // minutes in based on a 3 hour DIA - scaled proportionally (orig 75)

        double insulin_delay_minutes = 0;

        double insulin_timestamp = treatmentTimestamp + (insulin_delay_minutes * 60 * 1000);

        //Iob response = new Iob();

        final double scaleFactor = 3.0 / dia;
        double iobContrib = 0;
        //double activityContrib = 0;

        // only use treatments with insulin component which have already happened
        if ((treatmentInsulin > 0) && (insulin_timestamp < time)) {
            //  double bolusTime = insulin_timestamp; // bit of a dupe
            double minAgo = scaleFactor * (((time - insulin_timestamp) / 1000) / 60);

            if (minAgo < peak) {
                double x1 = minAgo / 5 + 1;
                iobContrib = treatmentInsulin * (1 - 0.001852 * x1 * x1 + 0.001852 * x1);
                // units: BG (mg/dL)  = (BG/U) *    U insulin     * scalar
                // activityContrib = sens * activityMultipler * treatment.insulin * (2 / dia / 60 / peak) * minAgo;

            } else if (minAgo < 180) {
                double x2 = (minAgo - peak) / 5;
                iobContrib = treatmentInsulin * (0.001323 * x2 * x2 - .054233 * x2 + .55556);
                //   activityContrib = sens * activityMultipler * treatment.insulin * (2 / dia / 60 - (minAgo - peak) * 2 / dia / 60 / (60 * dia - peak));
            }

        }
        if (iobContrib < 0) iobContrib = 0;
        //if (activityContrib < 0) activityContrib = 0;
        return new Pair<>(iobContrib, 0d);
    }
}


