package com.michaelfotiadis.ibeaconscanner.activities;

import android.Manifest;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.Toast;

import android.net.Uri;

import com.anthonycr.grant.PermissionsManager;
import com.anthonycr.grant.PermissionsResultAction;
import com.github.johnpersano.supertoasts.library.SuperActivityToast;
import com.michaelfotiadis.ibeaconscanner.R;
import com.michaelfotiadis.ibeaconscanner.adapter.ExpandableListAdapter;
import com.michaelfotiadis.ibeaconscanner.containers.CustomConstants;
import com.michaelfotiadis.ibeaconscanner.datastore.Singleton;
import com.michaelfotiadis.ibeaconscanner.processes.ScanHelper;
import com.michaelfotiadis.ibeaconscanner.tasks.MonitorTask;
import com.michaelfotiadis.ibeaconscanner.tasks.MonitorTask.OnBeaconDataChangedListener;
import com.michaelfotiadis.ibeaconscanner.utils.BluetoothHelper;
import com.michaelfotiadis.ibeaconscanner.utils.Logger;
import com.michaelfotiadis.ibeaconscanner.utils.ToastUtils;

import android.media.RingtoneManager;
import android.app.Notification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import uk.co.alt236.bluetoothlelib.device.BluetoothLeDevice;
import uk.co.alt236.bluetoothlelib.device.beacon.ibeacon.IBeaconManufacturerData;

public class MainActivity extends BaseActivity implements OnChildClickListener, OnCheckedChangeListener {

    private static final int RESULT_SETTINGS = 1;
    private final String TAG = MainActivity.class.getSimpleName();
    private ExpandableListAdapter mListAdapter;
    private List<String> mListDataHeader;
    private HashMap<String, List<String>> mListDataChild;
    private BluetoothHelper mBluetoothHelper;
    private ExpandableListView mExpandableListView;
    private CharSequence mTextViewContents;
    private MonitorTask mMonitorTask;
    // Receivers
    private ResponseReceiver mScanReceiver;
    private SharedPreferences mSharedPrefs;
    private boolean mIsScanRunning = false;
    private boolean mIsToastScanningNowShown;
    private boolean mIsToastStoppingScanShown;

    @Override
    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {


        if (!isChecked) {
            if (mIsScanRunning) {
                serviceToggle();
            }
        } else {

            PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, new PermissionsResultAction() {

                        @Override
                        public void onGranted() {

                            if (!mBluetoothHelper.isBluetoothLeSupported()) {
                                ToastUtils.makeWarningToast(MainActivity.this, getString(R.string.toast_no_le));
                                buttonView.setChecked(false);
                                return;
                            } else {
                                if (!mBluetoothHelper.isBluetoothOn()) {
                                    ScanHelper.cancelService(MainActivity.this);
                                    mBluetoothHelper.askUserToEnableBluetoothIfNeeded();
                                    buttonView.setChecked(false);
                                    return;
                                }
                            }

                            serviceToggle();
                        }

                        @Override
                        public void onDenied(final String permission) {
                            ToastUtils.makeWarningToast(MainActivity.this, getString(R.string.toast_warning_permission_not_granted));
                            buttonView.setChecked(false);
                        }
                    });
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           @NonNull final String[] permissions,
                                           @NonNull final int[] grantResults) {
        PermissionsManager.getInstance().notifyPermissionsChange(permissions, grantResults);
    }

    @Override
    public boolean onChildClick(final ExpandableListView parent, final View v,
                                final int groupPosition, final int childPosition, final long id) {

        if (mListDataChild != null) {
            final String address = mListDataChild.get(mListDataHeader.get(groupPosition)).get(childPosition);
            final Intent intent = new Intent(this, DeviceActivity.class);
            intent.putExtra(
                    CustomConstants.Payloads.PAYLOAD_1.toString(),
                    Singleton.getInstance().getBluetoothLeDeviceForAddress(address));
            startActivity(intent);
            return true;
        } else {
            return false;
        }

    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(getString(R.string.app_name));



        mExpandableListView = (ExpandableListView) findViewById(R.id.listViewResults);
        mExpandableListView.setOnChildClickListener(this);
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (savedInstanceState != null) {
            mTextViewContents = savedInstanceState.getCharSequence(CustomConstants.Payloads.PAYLOAD_1.toString());
            mIsScanRunning = savedInstanceState.getBoolean(CustomConstants.Payloads.PAYLOAD_2.toString(), false);
            mIsToastScanningNowShown = savedInstanceState.getBoolean(CustomConstants.Payloads.PAYLOAD_4.toString(), false);
            mIsToastStoppingScanShown = savedInstanceState.getBoolean(CustomConstants.Payloads.PAYLOAD_5.toString(), false);
        }

        // initialise Bluetooth utilities
        mBluetoothHelper = new BluetoothHelper(this);

        // monitor the singleton
        registerMonitorTask();

        // Wait for broadcasts from the scanning process
        registerResponseReceiver();
        SuperActivityToast.cancelAllSuperToasts();
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_main;
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        final View layout = menu.findItem(R.id.action_toggle).getActionView();
        final SwitchCompat toggle = (SwitchCompat) layout.findViewById(R.id.switchForActionBar);

        toggle.setChecked(mIsScanRunning);
        toggle.setOnCheckedChangeListener(this);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // same as using a normal menu
        switch (item.getItemId()) {
            case R.id.action_settings:
                startPreferencesActivity();
                break;
            default:
                break;
        }
        return true;
    }

    private void serviceToggle() {
        SuperActivityToast.cancelAllSuperToasts();

        if (mIsScanRunning) {
            // Cancels the alarms if the scan is already running
            ScanHelper.cancelService(this);
            mIsToastScanningNowShown = false;
            ToastUtils.makeInfoToast(this, getString(R.string.toast_interrupted));
            mIsScanRunning = false;
        } else {
            // This ScanHelper will also cancel all alarms on continuation
            ScanHelper.scanForIBeacons(MainActivity.this, getScanTime(), getPauseTime());
        }
    }

    @Override
    protected void onDestroy() {
        removeReceivers();
        super.onDestroy();
    }


    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        outState.putCharSequence(CustomConstants.Payloads.PAYLOAD_1.toString(), mTextViewContents);
        outState.putBoolean(CustomConstants.Payloads.PAYLOAD_2.toString(), mIsScanRunning);
        outState.putBoolean(CustomConstants.Payloads.PAYLOAD_4.toString(), mIsToastScanningNowShown);
        outState.putBoolean(CustomConstants.Payloads.PAYLOAD_5.toString(), mIsToastStoppingScanShown);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        // Cancel the alarm
        SuperActivityToast.cancelAllSuperToasts();
        ScanHelper.cancelService(this);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SuperActivityToast.cancelAllSuperToasts();

        handleResume();

    }

    private void handleResume() {

            if (mBluetoothHelper.isBluetoothOn()
                    && mBluetoothHelper.isBluetoothLeSupported()) {
                Logger.i(TAG, "Bluetooth has been activated");
                if (mIsScanRunning) {
                    Logger.d(TAG, "Restarting Scan Service");
                    ScanHelper.scanForIBeacons(MainActivity.this, getScanTime(), getPauseTime());
                }

                if (mIsToastScanningNowShown) {
                    ToastUtils.makeProgressToast(this, getString(R.string.toast_scanning));
                }
            } else {
                SuperActivityToast.cancelAllSuperToasts();
                ToastUtils.makeProgressToast(this, getString(R.string.toast_waiting));
            }
        updateListData();
    }

    private void removeReceivers() {
        try {
            this.unregisterReceiver(mScanReceiver);
            Logger.d(TAG, "Scan Receiver Unregistered Successfully");
        } catch (final Exception e) {
            Logger.d(
                    TAG,
                    "Scan Receiver Already Unregistered. Exception : "
                            + e.getLocalizedMessage());
        }
    }

    protected void stopMonitorTask() {
        if (mMonitorTask != null) {
            Logger.d(TAG, "Monitor Task paused");
            mMonitorTask.stop();
        }
    }

    private int getPauseTime() {
        final String result = mSharedPrefs.getString(
                getString(R.string.pref_pausetime),
                String.valueOf(getResources().getInteger(R.integer.default_pausetime)));
        return Integer.parseInt(result);
    }

    private int getScanTime() {
        final String result = mSharedPrefs.getString(
                getString(R.string.pref_scantime),
                String.valueOf(getResources().getInteger(R.integer.default_scantime)));
        return Integer.parseInt(result);

    }

    private void notifyDataChanged() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (Singleton.getInstance().getAvailableDevicesList() != null) {
                    updateListData();
                }
            }
        });
    }

    private void registerMonitorTask() {
        Logger.d(TAG, "Starting Monitor Task");
        mMonitorTask = new MonitorTask(new OnBeaconDataChangedListener() {
            @Override
            public void onDataChanged() {
                Logger.d(TAG, "Singleton Data Changed");
                notifyDataChanged();
            }
        });
        mMonitorTask.start();
    }

    private void registerResponseReceiver() {
        Logger.d(TAG, "Registering Response Receiver");
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CustomConstants.Broadcasts.BROADCAST_1.getString());
        intentFilter.addAction(CustomConstants.Broadcasts.BROADCAST_2.getString());

        mScanReceiver = new ResponseReceiver();
        this.registerReceiver(mScanReceiver, intentFilter);
    }

    private void startPreferencesActivity() {
        Logger.d(TAG, "Starting Settings Activity");
        final Intent intent = new Intent(this, ScanPreferencesActivity.class);
        startActivityForResult(intent, RESULT_SETTINGS);
    }

    private void updateListData() {

        Logger.d(TAG, "Updating List Data");
        mListDataHeader = new ArrayList<>();
        mListDataHeader.add("Available Devices (" + Singleton.getInstance().getAvailableDeviceListSize() + ")");



        mListDataChild = new HashMap<>();
        mListDataChild.put(mListDataHeader.get(0), Singleton.getInstance().getDevicesAvailableAsStringList());

        mListAdapter = new ExpandableListAdapter(this, mListDataHeader, mListDataChild);
        final IBeaconManufacturerData iBeaconData;

        for (int i = 0; i < Singleton.getInstance().getAvailableDeviceListSize(); i++) {
//            pushNotification(mListDataHeader.get(i), i);
//            Log.d("NOTIFICATION", mListDataChild.toString());
//            iBeaconData =  = new IBeaconManufacturerData(mListDataChild.get("Available Devices (" + Singleton.getInstance().getAvailableDeviceListSize() + ")").get(i));
            Toast.makeText(this, mListDataChild.get("Available Devices (" + Singleton.getInstance().getAvailableDeviceListSize() + ")").get(i), Toast.LENGTH_LONG).show();
            String msg = getNotificationStr(mListDataChild.get("Available Devices (" + Singleton.getInstance().getAvailableDeviceListSize() + ")").get(i));
            pushNotification(msg, i);
        }

        Logger.d(TAG, "Setting Adapter");
        mExpandableListView.setAdapter(mListAdapter);
    }

    public class ResponseReceiver extends BroadcastReceiver {
        private final String TAG = ResponseReceiver.class.getSimpleName();

        @Override
        public void onReceive(final Context context, final Intent intent) {
            Logger.d(TAG, "On Receiver Result");
            if (intent.getAction().equalsIgnoreCase(
                    CustomConstants.Broadcasts.BROADCAST_1.getString())) {
                Logger.i(TAG, "Scan Running");
                SuperActivityToast.cancelAllSuperToasts();
                mIsScanRunning = true;
                mIsToastScanningNowShown = true;
                ToastUtils.makeProgressToast(MainActivity.this, getString(R.string.toast_scanning));

            } else if (intent.getAction().equalsIgnoreCase(
                    CustomConstants.Broadcasts.BROADCAST_2.getString())) {
                Logger.i(TAG, "Service Finished");
                SuperActivityToast.cancelAllSuperToasts();
                mIsToastScanningNowShown = false;
                //				isToastStoppingScanShown = false;
                ToastUtils.makeInfoToast(MainActivity.this, getString(R.string.toast_completed));
            }
        }
    }

    private void pushNotification(String msg, int id) {
        String CHANNEL_ID = "mychannel";
        long[] v = {500,1000};

        Uri soundUri =RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("New Location!")
                        .setContentText(msg).setSound(soundUri).setVibrate(v);
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);



        notificationManager.notify(id, mBuilder.build());
    }

    private String getNotificationStr(String deviceId) {



        if(deviceId.equals("61:CD:91:CF:21:AC"))
            return "Main hall";
        return "See about nearest Lecure Room!";
    }

}
