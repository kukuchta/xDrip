package com.eveningoutpost.dexdrip;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.utilitymodels.Experience;
import com.eveningoutpost.dexdrip.stats.StatsActivity;
import com.eveningoutpost.dexdrip.utils.Preferences;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Emma Black on 11/5/14.
 */
public class NavDrawerBuilder {
    public final List<Intent> nav_drawer_intents = new ArrayList<>();
    public final List<String> nav_drawer_options = new ArrayList<>();

    public NavDrawerBuilder(final Context context) {

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean IUnderstand = prefs.getBoolean("I_understand", false);
        if (!IUnderstand) {
            this.nav_drawer_options.add(context.getString(R.string.settings));
            this.nav_drawer_intents.add(new Intent(context, Preferences.class));
            return;
        }

        this.nav_drawer_options.add(context.getString(R.string.home_screen));
        this.nav_drawer_intents.add(new Intent(context, Home.class));

        this.nav_drawer_options.add(context.getString(R.string.system_status));
        this.nav_drawer_intents.add(new Intent(context, MegaStatus.class));

        boolean bg_alerts = prefs.getBoolean("bg_alerts_from_main_menu", false);
        if (bg_alerts) {
            this.nav_drawer_options.add(context.getString(R.string.level_alerts));
            this.nav_drawer_intents.add(new Intent(context, AlertList.class));
        }

        if (Experience.gotData()) {
            this.nav_drawer_options.add(context.getString(R.string.snooze_alert));
            this.nav_drawer_intents.add(new Intent(context, SnoozeActivity.class));
        }

        if (Experience.gotData()) {
            this.nav_drawer_options.add(context.getString(R.string.statistics));
            this.nav_drawer_intents.add(new Intent(context, StatsActivity.class));

            this.nav_drawer_options.add(context.getString(R.string.history));
            this.nav_drawer_intents.add(new Intent(context, BGHistory.class));
        }

        this.nav_drawer_options.add(context.getString(R.string.settings));
        this.nav_drawer_intents.add(new Intent(context, Preferences.class));
    }
}
