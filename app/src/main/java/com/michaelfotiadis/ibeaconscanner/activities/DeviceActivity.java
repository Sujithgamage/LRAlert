package com.michaelfotiadis.ibeaconscanner.activities;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.commonsware.cwac.merge.MergeAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.michaelfotiadis.ibeaconscanner.R;
import com.michaelfotiadis.ibeaconscanner.containers.CustomConstants;
import com.michaelfotiadis.ibeaconscanner.utils.TimeFormatter;

import java.text.DateFormat;
import java.util.Locale;
import java.util.Calendar;
import java.util.Map;

import uk.co.alt236.bluetoothlelib.device.BluetoothLeDevice;
import uk.co.alt236.bluetoothlelib.device.beacon.BeaconType;
import uk.co.alt236.bluetoothlelib.device.beacon.BeaconUtils;
import uk.co.alt236.bluetoothlelib.device.beacon.ibeacon.IBeaconManufacturerData;
import uk.co.alt236.bluetoothlelib.resolvers.CompanyIdentifierResolver;


public class DeviceActivity extends BaseActivity {

    private FirebaseDatabase fd;
    private DatabaseReference mRef;

    private TextView hallName;
    private TextView lec1;
    private TextView lec2;
    private TextView lec3;
    private TextView lec4;
    private TextView year1;
    private TextView year2;
    private TextView year3;
    private TextView year4;
    private String[] words;

    private TextView hall;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setDisplayHomeAsUpEnabled(true);

        Calendar calendar = Calendar.getInstance();
        String currentDate=DateFormat.getDateInstance(DateFormat.FULL).format(calendar.getTime());

        words=currentDate.split("\\,");


        final BluetoothLeDevice device = getIntent().getParcelableExtra(CustomConstants.Payloads.PAYLOAD_1.toString());

        setTitle(device.getName());

        final ListView listView = (ListView) findViewById(R.id.list_view);

        populateDetails(listView, device);

    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_device;
    }

    @SuppressLint("InflateParams")
    private void appendDeviceInfo(final MergeAdapter adapter, final BluetoothLeDevice device) {
        final View layout = getLayoutInflater().inflate(R.layout.list_item_view_device_info, null);
        final TextView textViewName = (TextView) layout.findViewById(R.id.deviceName);
        final TextView textViewAddress = (TextView) layout.findViewById(R.id.deviceAddress);

    }

    /**
     * Append a header to the MergeAdapter
     *
     * @param adapter {@link MergeAdapter} to be used
     * @param title   String title to be appended
     */
    @SuppressLint("InflateParams")
    private void appendHeader(final MergeAdapter adapter, final String title) {
        final View layout = getLayoutInflater().inflate(R.layout.list_item_view_header, null);
        final TextView textViewTitle = (TextView) layout.findViewById(R.id.title);
        textViewTitle.setText(title);

        adapter.addView(layout);
    }

    /**
     * Append body text to the MergeAdapter
     *
     * @param adapter {@link MergeAdapter} to be used
     * @param data    String text to be appended
     */
    @SuppressLint("InflateParams")
    private void appendSimpleText(final MergeAdapter adapter, final String data) {
        final View lt = getLayoutInflater().inflate(R.layout.list_item_view_textview, null);
        final TextView tvData = (TextView) lt.findViewById(R.id.data);

        tvData.setText(data);

        adapter.addView(lt);
    }

    @SuppressLint("InflateParams")
    private void appendIBeaconInfo(final MergeAdapter adapter, final IBeaconManufacturerData iBeaconData) {
        final View lt = getLayoutInflater().inflate(R.layout.list_item_view_ibeacon_details, null);
        hallName = (TextView) lt.findViewById(R.id.hallName);
        lec1 = (TextView) lt.findViewById(R.id.lec1);
        lec2 = (TextView) lt.findViewById(R.id.lec2);
        lec3 = (TextView) lt.findViewById(R.id.lec3);
        lec4 = (TextView) lt.findViewById(R.id.lec4);
        year1 = (TextView) lt.findViewById(R.id.year1);
        year2 = (TextView) lt.findViewById(R.id.year2);
        year3 = (TextView) lt.findViewById(R.id.year3);
        year4 = (TextView) lt.findViewById(R.id.year4);
        hall = (TextView) lt.findViewById(R.id.lectureHall);

        final TextView a = (TextView) lt.findViewById(R.id.textView6);
        final TextView b = (TextView) lt.findViewById(R.id.textView7);
        final TextView c = (TextView) lt.findViewById(R.id.textView8);
        final TextView d = (TextView) lt.findViewById(R.id.textView12);
        final TextView e = (TextView) lt.findViewById(R.id.textView13);
        final TextView f = (TextView) lt.findViewById(R.id.textView14);
        final TextView g = (TextView) lt.findViewById(R.id.textView15);


            fd = FirebaseDatabase.getInstance();
        mRef = fd.getReference();
        mRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild(iBeaconData.getUUID())){
                    setValues(iBeaconData);
                }else{
                    hall.setText("This is not a valid Beacon!");
                    a.setText("");
                    b.setText("");
                    c.setText("");
                    d.setText("");
                    e.setText("");
                    f.setText("");
                    g.setText("");

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        adapter.addView(lt);
    }

    public void setValues(IBeaconManufacturerData iBeaconData){

        mRef = fd.getReference(iBeaconData.getUUID());

        mRef.child("HallName").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String hname = dataSnapshot.getValue(String.class);
                hallName.setText(hname);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });



        display(words[0]);
    }

    @SuppressLint("InflateParams")
    private void appendRssiInfo(final MergeAdapter adapter, final BluetoothLeDevice device) {

    }

    private String formatRssi(final int rssi) {
        return getString(R.string.formatter_db, String.valueOf(rssi));
    }

    private void populateDetails(final ListView listView, final BluetoothLeDevice device) {
        final MergeAdapter adapter = new MergeAdapter();

        if (device == null) {
            appendHeader(adapter, "");
            appendSimpleText(adapter, "");
        } else {
            appendHeader(adapter, "");
            appendDeviceInfo(adapter, device);

            if (BeaconUtils.getBeaconType(device) == BeaconType.IBEACON) {
                final IBeaconManufacturerData iBeaconData = new IBeaconManufacturerData(device);
                appendHeader(adapter, "About Nearest Lecture Hall");
                appendIBeaconInfo(adapter, iBeaconData);
            }

            appendHeader(adapter, "");
            appendRssiInfo(adapter, device);

        }

        listView.setAdapter(adapter);
    }

    private static String formatTime(final long time) {
        return TimeFormatter.getIsoDateTime(time);
    }

    private static String hexEncode(final int integer) {
        return "0x" + Integer.toHexString(integer).toUpperCase(Locale.US);
    }

    public void display(String day){

        DatabaseReference time1l = mRef.child(day).child("08:30-10:30").child("Lecture");

        time1l.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String l1 = dataSnapshot.getValue(String.class);
                lec1.setText(l1);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        DatabaseReference time1y = mRef.child(day).child("08:30-10:30").child("Year");

        time1y.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String l1 = dataSnapshot.getValue(String.class);
                year1.setText(l1);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        DatabaseReference time2l = mRef.child(day).child("10:30-12:30").child("Lecture");

        time2l.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String l1 = dataSnapshot.getValue(String.class);
                lec2.setText(l1);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        DatabaseReference time2y = mRef.child(day).child("10:30-12:30").child("Year");

        time2y.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String l1 = dataSnapshot.getValue(String.class);
                year2.setText(l1);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        DatabaseReference time3l = mRef.child(day).child("13:30-15:30").child("Lecture");

        time3l.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String l1 = dataSnapshot.getValue(String.class);
                lec3.setText(l1);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        DatabaseReference time3y = mRef.child(day).child("13:30-15:30").child("Year");

        time3y.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String l1 = dataSnapshot.getValue(String.class);
                year3.setText(l1);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        DatabaseReference time4l = mRef.child(day).child("15:30-17:30").child("Lecture");

        time4l.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String l1 = dataSnapshot.getValue(String.class);
                lec4.setText(l1);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        DatabaseReference time4y = mRef.child(day).child("15:30-17:30").child("Year");

        time4y.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String l1 = dataSnapshot.getValue(String.class);
                year4.setText(l1);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


//    public void pushNotification(String msg, int id) {
//        String CHANNEL_ID = "mychannel";
//
//        NotificationCompat.Builder mBuilder =
//                new NotificationCompat.Builder(this)
//                .setSmallIcon(R.mipmap.ic_launcher)
//                .setContentTitle("New Location!")
//                .setContentText(msg);
//        NotificationManager notificationManager =
//                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        notificationManager.notify(id, mBuilder.build());
//    }

}
