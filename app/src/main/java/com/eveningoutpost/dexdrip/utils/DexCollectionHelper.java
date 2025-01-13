package com.eveningoutpost.dexdrip.utils;

import android.app.Activity;
import android.text.InputType;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.utilitymodels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.cgm.sharefollow.ShareFollowService;

import static com.eveningoutpost.dexdrip.ui.dialog.QuickSettingsDialogs.booleanSettingDialog;
import static com.eveningoutpost.dexdrip.ui.dialog.QuickSettingsDialogs.textSettingDialog;

/**
 * Created by jamorham on 02/03/2018.
 */

public class DexCollectionHelper {

    private static final String TAG = DexCollectionHelper.class.getSimpleName();


    public static void assistance(Activity activity, DexCollectionType type) {

        switch (type) {
            case SHFollow:
                textSettingDialog(activity,
                        "shfollow_user", "Dex Share Username",
                        "Enter Share Follower Username",
                        InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                        new Runnable() {
                            @Override
                            public void run() {
                                textSettingDialog(activity,
                                        "shfollow_pass", "Dex Share Password",
                                        "Enter Share Follower Password",
                                        InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                booleanSettingDialog(activity,
                                                        "dex_share_us_acct", "Select Servers", "My account is on USA servers", "Select whether using USA or rest-of-world account", new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                Home.staticRefreshBGCharts();
                                                                ShareFollowService.resetInstanceAndInvalidateSession();
                                                                CollectionServiceStarter.restartCollectionServiceBackground();
                                                            }
                                                        });
                                            }
                                        });
                            }
                        });
                break;


        }


    }


}
