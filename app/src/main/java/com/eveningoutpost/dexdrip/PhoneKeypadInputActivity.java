package com.eveningoutpost.dexdrip;

import static com.eveningoutpost.dexdrip.Home.startHomeWithExtra;

import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


/**
 * Adapted from WearDialer which is:
 * <p/>
 * Confirmed as in the public domain by Kartik Arora who also maintains the
 * Potato Library: http://kartikarora.me/Potato-Library
 */

// jamorham xdrip plus

public class PhoneKeypadInputActivity extends BaseActivity {

    private TextView mDialTextView;
    private Button zeroButton, oneButton, twoButton, threeButton, fourButton, fiveButton,
            sixButton, sevenButton, eightButton, nineButton, starButton, backSpaceButton;
    private ImageButton insulintabbutton, carbstabbutton,
            bloodtesttabbutton, timetabbutton;

    private static String currenttab = "insulin-1";
    private static final String LAST_TAB_STORE = "phone-keypad-treatment-last-tab";
    private static final String TAG = "KeypadInput";
    private static Map<String, String> values = new HashMap<String, String>();
    private String bgUnits;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.keypad_activity_phone);
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int width = dm.widthPixels;
        int height = dm.heightPixels;
        final int refdpi = 320;
        Log.d(TAG, "Width height: " + width + " " + height + " DPI:" + dm.densityDpi);
        getWindow().setLayout((int) Math.min(((520 * dm.densityDpi) / refdpi), width), (int) Math.min((650 * dm.densityDpi) / refdpi, height));
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.dimAmount = 0.5f;

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        mDialTextView = (TextView) findViewById(R.id.dialed_no_textview);
        zeroButton = (Button) findViewById(R.id.zero_button);
        oneButton = (Button) findViewById(R.id.one_button);
        twoButton = (Button) findViewById(R.id.two_button);
        threeButton = (Button) findViewById(R.id.three_button);
        fourButton = (Button) findViewById(R.id.four_button);
        fiveButton = (Button) findViewById(R.id.five_button);
        sixButton = (Button) findViewById(R.id.six_button);
        sevenButton = (Button) findViewById(R.id.seven_button);
        eightButton = (Button) findViewById(R.id.eight_button);
        nineButton = (Button) findViewById(R.id.nine_button);
        starButton = (Button) findViewById(R.id.star_button);
        backSpaceButton = (Button) findViewById(R.id.backspace_button);
        // callImageButton = (ImageButton) stub.findViewById(R.id.call_image_button);
        // backspaceImageButton = (ImageButton) stub.findViewById(R.id.backspace_image_button);

        insulintabbutton = (ImageButton) findViewById(R.id.insulintabbutton);
        bloodtesttabbutton = (ImageButton) findViewById(R.id.bloodtesttabbutton);
        timetabbutton = (ImageButton) findViewById(R.id.timetabbutton);
        carbstabbutton = (ImageButton) findViewById(R.id.carbstabbutton);

        mDialTextView.setText("");

        mDialTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitAll();
            }
        });

        zeroButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appCurrent("0");
            }
        });

        //zeroButton.setOnLongClickListener(new View.OnLongClickListener() {
        //    @Override
        //    public boolean onLongClick(View v) {
        //        mDialTextView.setText(mDialTextView.getText() + "+");
        //        return true;
        //    }
        //});

        oneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appCurrent("1");
            }
        });

        twoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appCurrent("2");
            }
        });

        threeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appCurrent("3");
            }
        });

        fourButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appCurrent("4");
            }
        });

        fiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appCurrent("5");
            }
        });

        sixButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appCurrent("6");
            }
        });

        sevenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appCurrent("7");
            }
        });

        eightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appCurrent("8");
            }
        });

        nineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appCurrent("9");
            }
        });



        starButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!getValue(currenttab).contains(".")) appCurrent(".");
            }
        });

        //hashButton.setOnClickListener(new View.OnClickListener() {
        //    @Override
        //    public void onClick(View v) {
        //        mDialTextView.setText(mDialTextView.getText() + "#");
        //    }
        //});

        backSpaceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appBackSpace();
            }
        });
        backSpaceButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                values.put(currenttab, "");
                updateTab();
                return true;
            }
        });

        bloodtesttabbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currenttab = "bloodtest";
                updateTab();
            }
        });
        insulintabbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currenttab = "insulin-1";
                updateTab();
            }
        });
        carbstabbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currenttab = "carbs";
                updateTab();
            }
        });
        timetabbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currenttab = "time";
                updateTab();
            }
        });

        if (Pref.getString("units", "mgdl").equals("mgdl")) {
            bgUnits = "mg/dl";
        } else {
            bgUnits = "mmol/l";
        }
        updateTab();
    }

    public static void resetValues() {
        values = new HashMap<String, String>();
    }

    private static String getValue(String tab) {
        if (values.containsKey(tab)) {
            return values.get(tab);
        } else {
            values.put(tab, "");
            return values.get(tab);
        }
    }

    private static String appendValue(String tab, String append) {
        values.put(tab, getValue(tab) + append);
        return values.get(tab);
    }

    private static String appendCurrent(String append) {
        String cval = getValue(currenttab);
        if (cval.length() < 6) {
            if ((cval.length() == 0) && (append.equals("."))) append = "0.";
            return appendValue(currenttab, append);
        } else {
            return cval;
        }
    }

    private void appCurrent(String append) {
        appendCurrent(append);
        updateTab();
    }

    private void appBackSpace() {
        String cval = getValue(currenttab);
        if (cval.length() > 0) {
            values.put(currenttab, cval.substring(0, cval.length() - 1));
        }
        updateTab();
    }

    private boolean isNonzeroValueInTab(String tab)
    {
        try
        {
            return (0 != Double.parseDouble(getValue(tab)));
        }
        catch(NumberFormatException e) { return false; }
    }

    private boolean isInvalidTime()
    {
        String timeValue = getValue("time");
        if (timeValue.length() == 0) return false;
        if (!timeValue.contains("."))
            return (timeValue.length() < 3);

        String[] parts = timeValue.split("\\.");
        return (parts.length != 2) || (parts[0].length() == 0) || (parts[1].length() != 2);
    }

    private void submitAll() {

        boolean nonzeroBloodValue = isNonzeroValueInTab("bloodtest");
        boolean nonzeroCarbsValue = isNonzeroValueInTab("carbs");
        boolean nonzeroInsulin1Value = isNonzeroValueInTab("insulin-1");

        // The green tick is clickable even when it's hidden, so we might get here
        // without valid data.  Ignore the click if input is incomplete
        if(!nonzeroBloodValue && !nonzeroCarbsValue && !nonzeroInsulin1Value) {
            Log.d(TAG, "All zero values in tabs - not processing button click");
            return;
        }

        if (isInvalidTime()) {
            Log.d(TAG,"Time value is invalid - not processing button click");
            return;
        }


        // Add the dot to the time if it is missing
        String timeValue = getValue("time");
        if (timeValue.length() > 2 && !timeValue.contains(".")) {
            timeValue = timeValue.substring(0, timeValue.length()-2) + "." + timeValue.substring(timeValue.length()-2);
        }

        String mystring = "";
        double units = 0;
        final DecimalFormat df = new DecimalFormat("0.0#", new DecimalFormatSymbols(Locale.ENGLISH));
        if (timeValue.length() > 0) mystring += timeValue + " time ";
        if (nonzeroBloodValue) mystring += getValue("bloodtest") + " blood ";
        if (nonzeroCarbsValue) mystring += getValue("carbs") + " g carbs ";
        if (nonzeroInsulin1Value)
        {
            double d = Double.parseDouble(getValue("insulin-1"));
            units += d;
        }

        if (units > 0)
            mystring += df.format(units) + " units ";

        if (mystring.length() > 1) {
            //SendData(this, WEARABLE_VOICE_PAYLOAD, mystring.getBytes(StandardCharsets.UTF_8));
            resetValues();
            //WatchUpdaterService.receivedText(this, mystring); // reuse watch handling function to send data to home
            //startHomeWithExtra(this, "/xdrip_plus_voice_payload", mystring); // send data to home directly
            finish();
        }
    }

    private void updateTab() {

        final int offColor = Color.DKGRAY;
        final int onColor = Color.RED;

        insulintabbutton.setBackgroundColor(offColor);
        carbstabbutton.setBackgroundColor(offColor);
        timetabbutton.setBackgroundColor(offColor);
        bloodtesttabbutton.setBackgroundColor(offColor);

        String append = "";
        switch (currenttab.split("-")[0]) {
            case "insulin":
                insulintabbutton.setBackgroundColor(onColor);
                append = " " +  getString(R.string.units);
                break;
            case "carbs":
                carbstabbutton.setBackgroundColor(onColor);
                append = " g " + getString(R.string.carbs);
                break;
            case "bloodtest":
                bloodtesttabbutton.setBackgroundColor(onColor);
                append = " " + bgUnits;
                break;
            case "time":
                timetabbutton.setBackgroundColor(onColor);
                append = " " + getString(R.string.when);
                break;
        }
        String value = getValue(currenttab);
        mDialTextView.setText(value + append);
        // show green tick
        boolean showSubmitButton;

        if (isInvalidTime())
            showSubmitButton = false;

        else if (currenttab.equals("time"))
            showSubmitButton = value.length() > 0 && ( isNonzeroValueInTab("bloodtest") || isNonzeroValueInTab("carbs") || isNonzeroValueInTab("insulin-1"));
        else
            showSubmitButton = isNonzeroValueInTab(currenttab);

        mDialTextView.getBackground().setAlpha(showSubmitButton ? 255 : 0);    }


    @Override
    protected void onResume() {
        final String savedtab = PersistentStore.getString(LAST_TAB_STORE);
        if (savedtab.length() > 0) currenttab = savedtab;

        // snap back to insulin-1 tab if we have saved position on multiple insulins tabs
        if (currenttab.equals("insulin-2") || currenttab.equals("insulin-3")) {
            currenttab = "insulin-1";
        }

        updateTab();
        super.onResume();
    }

    @Override
    protected void onPause() {
        PersistentStore.setString(LAST_TAB_STORE, currenttab);
        super.onPause();
    }
}
