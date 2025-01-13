package com.eveningoutpost.dexdrip;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;

import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.utilitymodels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.utilitymodels.SensorStatus;
import com.eveningoutpost.dexdrip.databinding.ActivitySystemStatusBinding;
import com.eveningoutpost.dexdrip.ui.MicroStatus;
import com.eveningoutpost.dexdrip.ui.MicroStatusImpl;

import java.util.List;

import static com.eveningoutpost.dexdrip.utils.DatabaseUtil.getDataBaseSizeInBytes;
import static com.eveningoutpost.dexdrip.xdrip.gs;

public class SystemStatusFragment extends Fragment {
    private static final int SMALL_SCREEN_WIDTH = 300;
    //public static final String menu_name = "System Status";
    private TextView version_name_view;
    private TextView collection_method;
    private TextView sensor_status_view;
    private TextView notes;
    private Button restart_collection_service;
    private Button futureDataDeleteButton;
    private ImageButton refresh;
    private SharedPreferences prefs;
    private static final String TAG = "SystemStatus";
    private TextView db_size_view;

    //@Inject
    MicroStatus microStatus;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        final ActivitySystemStatusBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.activity_system_status, container, false);
        microStatus = new MicroStatusImpl();
        binding.setMs(microStatus);
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // setContentView(R.layout.activity_system_status);
        // JoH.fixActionBar(this);
        prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
        final View v = getView();
        version_name_view = (TextView) v.findViewById(R.id.version_name);
        collection_method = (TextView) v.findViewById(R.id.collection_method);
        sensor_status_view = (TextView) v.findViewById(R.id.sensor_status);
        db_size_view = (TextView) v.findViewById(R.id.db_size);
        notes = (TextView) v.findViewById(R.id.other_notes);
        restart_collection_service = (Button) v.findViewById(R.id.restart_collection_service);
        refresh = (ImageButton) v.findViewById(R.id.refresh_current_values);
        futureDataDeleteButton = (Button) v.findViewById(R.id.delete_future_data);

        //check for small devices:
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        if (width < SMALL_SCREEN_WIDTH) {
            //adapt to small screen
            LinearLayout layout = (LinearLayout) v.findViewById(R.id.layout_collectionmethod);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout = (LinearLayout) v.findViewById(R.id.layout_version);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout = (LinearLayout) v.findViewById(R.id.layout_status);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout = (LinearLayout) v.findViewById(R.id.layout_device);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout = (LinearLayout) v.findViewById(R.id.layout_sensor);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout = (LinearLayout) v.findViewById(R.id.layout_transmitter);
            layout.setOrientation(LinearLayout.VERTICAL);
        }

        set_current_values();
        restartButtonListener();
        refreshButtonListener();
    }

    private void set_current_values() {
        notes.setText("");
        setVersionName();
        setSensorStatus();
        futureDataCheck();
        setDbSize();

       /* if (notes.getText().length()==0) {
            notes.setText("Swipe for more status pages!");
        }*/
    }

    private void setDbSize() {
        long dbSizeLengthLong = getDataBaseSizeInBytes();
        String dbSizeString = "0";
        if (dbSizeLengthLong > 0) { // If there is a database
            if (dbSizeLengthLong < 31457280) { // When smaller than 30M, round and show one decimal point
                dbSizeString = JoH.roundFloat((float) dbSizeLengthLong / (1024 * 1024), 1) + "";
            } else { // When greater than 30M, round and just show integer
                dbSizeString = (int) (JoH.roundFloat((float) dbSizeLengthLong / (1024 * 1024), 0)) + "";
            }
            db_size_view.setText(dbSizeString + "M");
        }
    }

    private void setSensorStatus() {
        sensor_status_view.setText(SensorStatus.status());
    }


    private void setVersionName() {
        String versionName;
        try {
            versionName = safeGetContext().getPackageManager().getPackageInfo(safeGetContext().getPackageName(), PackageManager.GET_META_DATA).versionName;
            int versionNumber = safeGetContext().getPackageManager().getPackageInfo(safeGetContext().getPackageName(), PackageManager.GET_META_DATA).versionCode;
            versionName += "\nCode: " + BuildConfig.buildVersion;
            version_name_view.setText(versionName);
        } catch (PackageManager.NameNotFoundException e) {
            //e.printStackTrace();
            Log.e(this.getClass().getSimpleName(), "PackageManager.NameNotFoundException:" + e.getMessage());
        }
    }

    private void futureDataCheck() {
        futureDataDeleteButton.setVisibility(View.GONE);
        final List<BgReading> futureReadings = BgReading.futureReadings();
        if ((futureReadings != null && !futureReadings.isEmpty())) {
            notes.append("\n- Your device has future data on it, Please double check the time and timezone on this phone.");
            futureDataDeleteButton.setVisibility(View.VISIBLE);
        }
        futureDataDeleteButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (futureReadings != null && futureReadings.size() > 0) {
                    for (BgReading bgReading : futureReadings) {
                        bgReading.calculated_value = 0;
                        bgReading.raw_data = 0;
                        bgReading.timestamp = 0;
                        bgReading.save();
                    }
                }
            }
        });
    }

    private void restartButtonListener() {
        restart_collection_service.setOnClickListener(new View.OnClickListener() {
            public void onClick(final View v) {
                v.setEnabled(false);
                JoH.static_toast_short(gs(R.string.restarting_collector));
                v.setAlpha(0.2f);
                CollectionServiceStarter.restartCollectionService(safeGetContext());
                set_current_values();
                JoH.runOnUiThreadDelayed(new Runnable() {
                    @Override
                    public void run() {
                        v.setEnabled(true);
                        v.setAlpha(1.0f);
                        set_current_values();
                    }
                }, 2000);
            }
        });
    }

    private void refreshButtonListener() {
        refresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                set_current_values();
            }
        });
    }

    private Context safeGetContext() {
        if (isAdded()) {
            return getActivity();
        } else {
            return xdrip.getAppContext();
        }
    }

}
