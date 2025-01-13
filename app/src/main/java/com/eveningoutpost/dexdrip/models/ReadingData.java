package com.eveningoutpost.dexdrip.models;

// class from LibreAlarm

import com.eveningoutpost.dexdrip.models.UserError.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class ReadingData {

    private static final String TAG = "ReadingData";
    public List<GlucoseData> trend; // Per minute data.
    public List<GlucoseData> history;  // Per 15 minutes data.
    public byte[] raw_data;

    private static final byte ERROR_INFLUENCE = 4; //  The influence of each error
    private static final byte PREFERRED_AVERAGE = 5; //  Try to use 5 numbers for the average
    private static final byte MAX_DISTANCE_FOR_SMOOTHING = 7; //  If points have been removed, use up to 7 numbers for the average.

    public ReadingData() {
        this.trend = new ArrayList<GlucoseData>();
        this.history = new ArrayList<GlucoseData>();
        // The two bytes are needed here since some components don't like a null pointer.
        this.raw_data = new byte[2];
    }

    public ReadingData(List<GlucoseData> trend, List<GlucoseData> history) {
        this.trend = trend;
        this.history = history;
    }

    public String toString() {
        String ret = "ternd ";
        for (GlucoseData gd : trend) {
            ret += gd.toString();
            ret += " ";
        }
        ret += "history ";
        for (GlucoseData gd : history) {
            ret += gd.toString();
            ret += " ";
        }
        return "{" + ret + "}";
    }


    private void CalculateSmothedData5Points() {
        // In all places in the code, there should be exactly 16 points.
        // Since that might change, and I'm doing an average of 5, then in the case of less then 5 points,
        // I'll only copy the data as is (to make sure there are reasonable values when the function returns).
        if (trend.size() < 5) {
            for (int i = 0; i < trend.size() - 4; i++) {
                trend.get(i).glucoseLevelRawSmoothed = trend.get(i).glucoseLevelRaw;
            }
            return;
        }

        for (int i = 0; i < trend.size() - 4; i++) {
            trend.get(i).glucoseLevelRawSmoothed =
                    (trend.get(i).glucoseLevelRaw +
                            trend.get(i + 1).glucoseLevelRaw +
                            trend.get(i + 2).glucoseLevelRaw +
                            trend.get(i + 3).glucoseLevelRaw +
                            trend.get(i + 4).glucoseLevelRaw) / 5;
        }
        // We now have to calculate the last 4 points, will do our best...
        trend.get(trend.size() - 4).glucoseLevelRawSmoothed =
                (trend.get(trend.size() - 4).glucoseLevelRaw +
                        trend.get(trend.size() - 3).glucoseLevelRaw +
                        trend.get(trend.size() - 2).glucoseLevelRaw +
                        trend.get(trend.size() - 1).glucoseLevelRaw) / 4;

        trend.get(trend.size() - 3).glucoseLevelRawSmoothed =
                (trend.get(trend.size() - 3).glucoseLevelRaw +
                        trend.get(trend.size() - 2).glucoseLevelRaw +
                        trend.get(trend.size() - 1).glucoseLevelRaw) / 3;

        // Use the last two points for both last points
        trend.get(trend.size() - 2).glucoseLevelRawSmoothed =
                (trend.get(trend.size() - 2).glucoseLevelRaw +
                        trend.get(trend.size() - 1).glucoseLevelRaw) / 2;

        trend.get(trend.size() - 1).glucoseLevelRawSmoothed = trend.get(trend.size() - 2).glucoseLevelRawSmoothed;
    }

    public void CalculateSmothedData() {
        CalculateSmothedData5Points();
        // print the values, remove before release
        for (int i = 0; i < trend.size(); i++) {
            Log.e("xxx", "" + i + " raw val " + trend.get(i).glucoseLevelRaw + " smoothed " + trend.get(i).glucoseLevelRawSmoothed);
        }
    }

}
