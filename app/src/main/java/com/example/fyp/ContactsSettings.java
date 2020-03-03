package com.example.fyp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;

public class ContactsSettings extends AppCompatActivity implements View.OnClickListener {

    ImageView backbutton;

    Button addContacts;

    Button viewContacts;

    TextView tv1;

    Button test;

    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 1;
    private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 2;

    LatLng fromPosition;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts_settings);

        backbutton = (ImageView) findViewById(R.id.backbtn1);

        backbutton.setOnClickListener(this);

        addContacts = (Button) findViewById(R.id.btn);
        viewContacts = (Button) findViewById(R.id.btn1);

        addContacts.setOnClickListener(this);
        viewContacts.setOnClickListener(this);

        tv1 = (TextView) findViewById(R.id.contactText);
        tv1.setOnClickListener(this);

        test = (Button) findViewById(R.id.btn2);
        test.setOnClickListener(this);

        test.setVisibility(View.GONE);


    }

    private void getUserLocation() {
        //vars
        final String TAG = "Check";
        Log.d(TAG, "getDeviceLocation: getting the devices current location");

        FusedLocationProviderClient mFusedLocationProviderClient;
        Boolean mLocationPermissionsGranted = true;
        String locationInfo = "no location";
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        //  LatLng fromPosition;

        try {
            if (mLocationPermissionsGranted) {

                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "onComplete: found location!");
                            Location currentLocation = (Location) task.getResult();
                            if (currentLocation == null) {
                                Toast.makeText(getApplicationContext(), "unable to get current location. wait for a few minutes and restart the app", Toast.LENGTH_SHORT).show();

                            } else {
                                fromPosition = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

                                Toast.makeText(getApplicationContext(), "current location " + fromPosition, Toast.LENGTH_SHORT).show();
                                String latlng = fromPosition.toString();
                                SendSms(latlng);
                            }

                        } else {
                            Log.d(TAG, "onComplete: current location is null");
                            Toast.makeText(getApplicationContext(), "unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage());
        }

    }

    @Override
    public void onClick(View v) {

        if (v.getId() == backbutton.getId()) {
            finish();
        } else if (v.getId() == addContacts.getId()) {

            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.READ_CONTACTS) !=
                    PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_CONTACTS},
                        MY_PERMISSIONS_REQUEST_READ_CONTACTS);
            } else {

                Intent intent = new Intent(this, AddContacts.class);
                startActivity(intent);
            }


        } else if (v.getId() == viewContacts.getId()) {
            Intent intent = new Intent(this, ViewTrustedContacts.class);
            startActivity(intent);
        } else if (v.getId() == tv1.getId()) {
            test.setVisibility(View.VISIBLE);
        } else if (v.getId() == test.getId()) {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.SEND_SMS) !=
                    PackageManager.PERMISSION_GRANTED) {
                // Permission not yet granted. Use requestPermissions().
                // MY_PERMISSIONS_REQUEST_SEND_SMS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.SEND_SMS},
                        MY_PERMISSIONS_REQUEST_SEND_SMS);
            } else {
                LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

                if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

                //    Toast.makeText(this, "Turn on Gps", Toast.LENGTH_SHORT).show();
                    buildAlertMessageNoGps();
                } else {


                    getUserLocation();
                }
            }


        }
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    public void SendSms(String location) {
        ArrayList<String> items = new ArrayList<String>();
        SQLiteDatabase db;
        String name = "name";
        String number = "number";
        db = openOrCreateDatabase("DriverAssistant", MODE_PRIVATE, null);
        Cursor cursor = db.rawQuery("select * from TrustedContacts", null);
        items = new ArrayList<String>();
        if (cursor.moveToFirst()) {
            do {
                items.add(cursor.getString(cursor.getColumnIndex(name)) + ":"
                        + cursor.getString(cursor.getColumnIndex(number)));
            } while (cursor.moveToNext());
        }

        SmsManager smsManager = SmsManager.getDefault();
        for (int i = 0; i < items.size(); i++) {
            String[] itemsInfo = items.get(i).split(":");
            Log.d("Check", "TrustedContactValue " + i + " " + itemsInfo[1]);
            smsManager.sendTextMessage(itemsInfo[1] + "", null, "Apk bhai ny gari thok di hai idhar! " + location, null, null);
            Toast.makeText(this, "Message Sent Successfully", Toast.LENGTH_SHORT).show();

        }

//        SmsManager smsManager = SmsManager.getDefault();
//        smsManager.sendTextMessage(03415605520 + "", null, "User location " + location, null, null);
//        Toast.makeText(this, "Message Sent Successfully", Toast.LENGTH_SHORT).show();
    }
}
