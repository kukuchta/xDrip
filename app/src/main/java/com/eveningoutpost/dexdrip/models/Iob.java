package com.eveningoutpost.dexdrip.models;

import static com.eveningoutpost.dexdrip.utilitymodels.Constants.HOUR_IN_MS;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.MINUTE_IN_MS;

import static java.lang.StrictMath.abs;

import android.util.Pair;

import com.eveningoutpost.dexdrip.insulin.Insulin;
import com.eveningoutpost.dexdrip.insulin.InsulinManager;
import com.eveningoutpost.dexdrip.insulin.MultipleInsulins;
import com.eveningoutpost.dexdrip.services.UiBasedCollector;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import lombok.val;

/**
 * Created by jamorham on 02/01/16.
 */
public class Iob {
    private static final String TAG = Treatments.class.getSimpleName();
    public long timestamp;
    public double iob = 0;
    public CobCalc cobCalc;
    public double cob = 0;
    public double jCarbImpact = 0;
    public double jActivity = 0;

    public static Double getCurrentIoB() {
        if (Pref.getBooleanDefaultFalse("fetch_iob_from_companion_app")) {
            return getCurrentIoBFromCompanionApp();
        } else {
            return getCurrentIoBFromGraphCalculation();
        }
    }

    public static Double getCurrentIoBFromCompanionApp() {


        return UiBasedCollector.getCurrentIoB();
    }

    public static Double getCurrentIoBFromGraphCalculation() {
        long now = System.currentTimeMillis();

        final List<Iob> iobInfo = Iob.ioBForGraph_new(now - Constants.DAY_IN_MS);

        if (iobInfo != null) {
            for (Iob iob : iobInfo) {
                // Find IoB sample close to the current timestamp.
                if (iob.timestamp > now - 5 * MINUTE_IN_MS && iob.timestamp < now + 5 * MINUTE_IN_MS) {
                    return iob.iob;
                }
            }
        }

        return null;
    }

    public static List<Iob> ioBForGraph_new(long startTime) {

        JoH.benchmark_method_start();

        // 10 hours max look or from insulin manager if enabled
        final double dontLookThisFar = 10 * HOUR_IN_MS;

        // look back the longest effect period of all enabled insulin profiles (startTime is always 24h behind NOW)
        List<Treatments> theTreatments = Treatments.latestForGraph(2000, startTime - dontLookThisFar);
        List<Autocorrection> theAutocorrections = Autocorrection.latestForGraph(2000, startTime - dontLookThisFar);
        List<AutoBasalDelivery> theAutoBasalDeliveries = AutoBasalDelivery.latestForGraph(2000, startTime - dontLookThisFar);
        UserError.Log.d(TAG,"TREATMENT LIST: "+theTreatments.size()+" "+JoH.dateTimeText((long)(startTime - dontLookThisFar)));
        if (theTreatments.isEmpty()) {
            return null;
        }



        int counter = 0; // iteration counter

        final double step_minutes = 5;
        final long stepms = (long) (step_minutes * MINUTE_IN_MS); // 300s = 5 mins
        long mytime = startTime;
        long tendtime = startTime;


        final double carb_delay_minutes = Profile.carbDelayMinutes(mytime); // not likely a time dependent parameter
        final double carb_delay_ms_stepped = ((long) (carb_delay_minutes / step_minutes)) * step_minutes * MINUTE_IN_MS;

        UserError.Log.d(TAG, "Carb delay ms: " + carb_delay_ms_stepped);

        Map<String, Boolean> carbsEaten = new HashMap<String, Boolean>();

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
            if (thisTreatment.insulin > 0) {
                // lay down insulin on board
                do {

                    calcreply = calcTreatment(thisTreatment.timestamp, thisTreatment.insulin, mytime);
                    calcreply.jActivity *= step_minutes;    // has to be multiplied because derivation function of IOB calculates a step_minutes lower activity as the "old" logic
                    calcreply.jActivity *= Profile.getSensitivity(mytime);

                    if (mytime >= startTime) {
                        timesliceInsulinWriter(timeslices, calcreply, mytime);
                    }
                    mytime = mytime + stepms; // advance time counter
                } while ((mytime < tendtime) &&
                        ((calcreply.iob == 0) || (calcreply.iob > 0.01)));
            }
        } // per insulin treatment

        for (Autocorrection thisAutocorrection : theAutocorrections) {
            // early optimisation exclusion

            mytime = (long) ((thisAutocorrection.timestamp / stepms) * stepms); // effects of treatment occur only after it is given / fit to slot time
            tendtime = mytime + 36 * HOUR_IN_MS;     // 36 hours max look (24h history plus 12h forecast)
            if (tendtime > startTime + 30 * HOUR_IN_MS)
                tendtime = startTime + 30 * HOUR_IN_MS;   // dont look more than 6h in future // TODO review time limit
            if (thisAutocorrection.deliveredFastAmount > 0) {
                // lay down insulin on board
                do {

                    calcreply = calcTreatment(thisAutocorrection.timestamp, thisAutocorrection.deliveredFastAmount, mytime);
                    calcreply.jActivity *= step_minutes;    // has to be multiplied because derivation function of IOB calculates a step_minutes lower activity as the "old" logic
                    calcreply.jActivity *= Profile.getSensitivity(mytime);

                    if (mytime >= startTime) {
                        timesliceInsulinWriter(timeslices, calcreply, mytime);
                    }
                    mytime = mytime + stepms; // advance time counter
                } while ((mytime < tendtime) &&
                        ((calcreply.iob == 0) || (calcreply.iob > 0.01)));
            }
        } // per autocorrection

        for (AutoBasalDelivery thisAutoBasalDelivery : theAutoBasalDeliveries) {
            // early optimisation exclusion

            mytime = (long) ((thisAutoBasalDelivery.timestamp / stepms) * stepms); // effects of treatment occur only after it is given / fit to slot time
            tendtime = mytime + 36 * HOUR_IN_MS;     // 36 hours max look (24h history plus 12h forecast)
            if (tendtime > startTime + 30 * HOUR_IN_MS)
                tendtime = startTime + 30 * HOUR_IN_MS;   // dont look more than 6h in future // TODO review time limit
            if (thisAutoBasalDelivery.bolusAmount > 0) {
                // lay down insulin on board
                do {

                    calcreply = calcTreatment(thisAutoBasalDelivery.timestamp, thisAutoBasalDelivery.bolusAmount, mytime);
                    calcreply.jActivity *= step_minutes;    // has to be multiplied because derivation function of IOB calculates a step_minutes lower activity as the "old" logic
                    calcreply.jActivity *= Profile.getSensitivity(mytime);

                    if (mytime >= startTime) {
                        timesliceInsulinWriter(timeslices, calcreply, mytime);
                    }
                    mytime = mytime + stepms; // advance time counter
                } while ((mytime < tendtime) &&
                        ((calcreply.iob == 0) || (calcreply.iob > 0.01)));
            }
        } // per autocorrection

        // legacy jActivity calculation
        UserError.Log.d(TAG, "Single insulin type iteration counter: " + counter);

        // evaluate insulin impact
        Iob lastiob = null;
        for (Map.Entry<Long, Iob> entry : timeslices.entrySet()) {
            Iob thisiob = entry.getValue();
            if (lastiob != null) {
                if ((thisiob.iob != 0) || (lastiob.iob != 0)) {
                    if (thisiob.iob < lastiob.iob) {
                        // decaying iob
                        thisiob.jActivity = (lastiob.iob - thisiob.iob) * Profile.getSensitivity(thisiob.timestamp);
                    } else {
                        // more insulin added
                        thisiob.jActivity = 0; // TODO THIS IS NOT RIGHT IT MISSES ONE DECAY STEP
                    }
                }
            }

            //Log.d(TAG,"iobinfo2 iob debug: "+JoH.qs(thisiob.timestamp)+" C:"+JoH.qs(thisiob.cob,4)+" I:"+JoH.qs(thisiob.iob,4)+" CA:"+JoH.qs(thisiob.jCarbImpact)+" IA:"+JoH.qs(thisiob.jActivity));
            counter++;
            lastiob = thisiob;
        }
        //


        // calculate carb treatments
        for (Treatments thisTreatment : theTreatments) {

            if (thisTreatment.carbs > 0) {

                mytime = ((long) (thisTreatment.timestamp / stepms)) * stepms; // effects of treatment occur only after it is given / fit to slot time
                tendtime = mytime + 6 * HOUR_IN_MS;     // 6 hours max look

                long cob_time = (long) (mytime + carb_delay_ms_stepped);
                double stomachDiff = ((Profile.getCarbAbsorptionRate(cob_time) * stepms) / HOUR_IN_MS); // initial value
                double newdelayedCarbs = 0;
                double cob_remain = thisTreatment.carbs;
                while ((cob_remain > 0) && (stomachDiff > 0) && (cob_time < tendtime)) {

                    if (cob_time >= startTime) {
                        timesliceCarbWriter(timeslices, cob_time, cob_remain);
                    }
                    cob_time += stepms;

                    stomachDiff = ((Profile.getCarbAbsorptionRate(cob_time) * stepms) / HOUR_IN_MS);
                    cob_remain -= stomachDiff;

                    newdelayedCarbs = (timesliceIactivityAtTime(timeslices, cob_time) * Profile.getLiverSensRatio(cob_time) / Profile.getSensitivity(cob_time)) * Profile.getCarbRatio(cob_time);

                    if (newdelayedCarbs > 0) {
                        final double maximpact = stomachDiff * Profile.maxLiverImpactRatio(cob_time);
                        if (newdelayedCarbs > maximpact) newdelayedCarbs = maximpact;
                        cob_remain += newdelayedCarbs; // add back on liverfactor adjustment
                    }

                    counter++;

                }
                // end record if not present
                if (cob_time >= startTime) {
                    timesliceCarbWriter(timeslices, cob_time, 0);
                }
            }
        }

        // evaluate carb impact
        lastiob = null;
        for (Map.Entry<Long, Iob> entry : timeslices.entrySet()) {
            Iob thisiob = entry.getValue();
            if (lastiob != null) {
                if ((thisiob.cob != 0 || (lastiob.cob != 0))) {
                    if (thisiob.cob < lastiob.cob) {
                        // decaying cob
                        thisiob.jCarbImpact = (lastiob.cob - thisiob.cob) / Profile.getCarbRatio(thisiob.timestamp) * Profile.getSensitivity(thisiob.timestamp);
                    } else {
                        // more carbs added
                        thisiob.jCarbImpact = 0; // TODO THIS IS NOT RIGHT IT MISSES ONE DECAY STEP
                    }
                }
            }

            //   Log.d(TAG,"iobinfo2carb  debug: "+JoH.qs(thisiob.timestamp)+" C:"+JoH.qs(thisiob.cob,4)+" I:"+JoH.qs(thisiob.iob,4)+" CA:"+JoH.qs(thisiob.jCarbImpact)+" IA:"+JoH.qs(thisiob.jActivity));
            counter++;
            lastiob = thisiob;
        }

        UserError.Log.d(TAG, "second iteration counter: " + counter);
        UserError.Log.d(TAG, "Timeslices size: " + timeslices.size());
        JoH.benchmark_method_end();
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
                tempiob.jActivity+= thisiob.jActivity;
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

    // requires stepms granularity which we should already have
    private static double timesliceIactivityAtTime(Map<Long, Iob> timeslices, long thistime) {
        if (timeslices.containsKey(thistime)) {
            return timeslices.get(thistime).jActivity;
        } else {
            return 0;
        }
    }

    // when using multiple insulins
    private static Pair<Double, Double> calculateIobActivityFromTreatmentAtTime(final Treatments treatment, final long time, final boolean useBasal) {

        double iobContrib = 0, activityContrib = 0;
        if (treatment.insulin > 0) {
            // Log.d(TAG,"NEW TYPE insulin: "+treatment.insulin+ " "+treatment.insulinJSON);
            // translate a legacy entry to be bolus insulin
            List<InsulinInjection> injectionsList = treatment.getInsulinInjections();
            if (injectionsList == null || injectionsList.size() == 0) {
                UserError.Log.d(TAG,"CONVERTING LEGACY: "+treatment.insulinJSON+ " "+injectionsList);
                injectionsList = convertLegacyDoseToBolusInjectionList(treatment.insulin);
                treatment.insulinInjections = injectionsList; // cache but best not to save it
            }

            for (final InsulinInjection injection : injectionsList)
                if (injection.getUnits() > 0 && (useBasal || !injection.isBasal())) {
                    iobContrib += injection.getUnits() * abs(injection.getProfile().calculateIOB((time - treatment.timestamp) / MINUTE_IN_MS));
                    activityContrib += injection.getUnits() * abs(injection.getProfile().calculateActivity((time - treatment.timestamp) / MINUTE_IN_MS));
                }
            if (iobContrib < 0) iobContrib = 0;
            if (activityContrib < 0) activityContrib = 0;
        }
        return new Pair<>(iobContrib, activityContrib);
    }

    // take a simple insulin value and produce a list assuming it is bolus insulin - for legacy conversion
    static public List<InsulinInjection> convertLegacyDoseToBolusInjectionList(final double insulinSum) {
        final ArrayList<InsulinInjection> injections = new ArrayList<>();
        val profile = (val) InsulinManager.getBolusProfile();
        if (profile != null) {
            injections.add(new InsulinInjection((Insulin) profile, insulinSum));
        } else {
            UserError.Log.e(TAG, "bolus profile is null");
        }
        return injections;
    }

    // using the original calculation
    private static Pair<Double, Double> calculateLegacyIobActivityFromTreatmentAtTime(final long treatmentTimestamp, final double treatmentInsulin, final long time) {

        final double dia = Profile.insulinActionTime(time); // duration insulin action in hours
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


