package com.example.fyp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.location.config.LocationAccuracy;
import io.nlopez.smartlocation.location.config.LocationParams;

public class Sms extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 1;
    private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 2;

    LatLng fromPosition = new LatLng(0.0, 0.0);

    Boolean sendMsgFlag = true;
    long seconds;
    Button cancel;
    TextView timer, text1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms);
        getUserLocation();


        text1 = (TextView) findViewById(R.id.textView);
        cancel = (Button) findViewById(R.id.cancel);
        timer = (TextView) findViewById(R.id.textView5);

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Cancelled!", Toast.LENGTH_SHORT).show();
                sendMsgFlag = false;

            }
        });
        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

            //    Toast.makeText(this, "Turn on Gps", Toast.LENGTH_SHORT).show();
            buildAlertMessageNoGps();
        } else {
            sendMsgFlag = true;
            new CountDownTimer(15000, 1000) {

                @Override
                public void onTick(long millisUntilFinished) {
                    seconds = (millisUntilFinished / 1000);
                    //     Toast.makeText(getApplicationContext(), "seconds remaining: " + millisUntilFinished / 1000, Toast.LENGTH_SHORT).show();
                    timer.setText("Seconds left: " + seconds + "");
                }

                @Override
                public void onFinish() {
                    if (sendMsgFlag) {
                        text1.setText("Sms Sent to Emergency Contacts Successfully!");
                        cancel.setVisibility(View.GONE);
                        Toast.makeText(getApplicationContext(), "Message Sent", Toast.LENGTH_SHORT).show();
                    //    SendSms(fromPosition);
                    }
                }
            }.start();


        }
    }

    private void getUserLocation() {
        //vars
        final String TAG = "Check";
        Log.d(TAG, "getDeviceLocation: getting the devices current location");
        Boolean mLocationPermissionsGranted = true;
        try {
            if (mLocationPermissionsGranted) {


                SmartLocation smartLocation = null;
                LocationParams.Builder builder;


                smartLocation = new SmartLocation.Builder(this).logging(true).build();
                builder = new LocationParams.Builder()
                        .setAccuracy(LocationAccuracy.HIGH)
                        .setDistance(0)
                        .setInterval(5000);
                try {
                    smartLocation.with(this)
                            .location()
                            .config(LocationParams.BEST_EFFORT)
                            .continuous()
                            .config(builder.build())
                            .start(new OnLocationUpdatedListener() {
                                @Override
                                public void onLocationUpdated(Location location) {
                                    fromPosition = new LatLng(location.getLatitude(), location.getLongitude());
                                    //   Toast.makeText(getApplicationContext(), "current location " + fromPosition, Toast.LENGTH_SHORT).show();


                                }
                            });


                } catch (SecurityException se) {
                    se.printStackTrace();
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage());
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

    public void SendSms(LatLng location) {

        Geocoder geocoder;
        List<Address> addresses = null;
        geocoder = new Geocoder(this, Locale.getDefault());

        try {
            addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
        } catch (IOException e) {
            e.printStackTrace();
        }

        String address = addresses.get(0).getAddressLine(0);


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

 //       String message= "Alert! It appears that your friend have been in an accident! The person's current location is " +address;

        SmsManager smsManager = SmsManager.getDefault();
        for (int i = 0; i < items.size(); i++) {
            String[] itemsInfo = items.get(i).split(":");
            Log.d("Check", "TrustedContactValue " + i + " " + itemsInfo[1]);
            smsManager.sendTextMessage(itemsInfo[1] + "", null, "Alert! It appears that your friend have been in an accident! ", null, null);
            smsManager.sendTextMessage(itemsInfo[1] + "", null, "The person's current location is " +address, null, null);
            Toast.makeText(this, "Message Sent Successfully", Toast.LENGTH_SHORT).show();

        }

//        SmsManager smsManager = SmsManager.getDefault();
//     smsManager.sendTextMessage(03415605520 + "", null, "Alert! It appears that your friend have been in an accident! ", null, null);
//        smsManager.sendTextMessage(03415605520 + "", null, "The person's current location is " +location, null, null);
//        Toast.makeText(this, "Message Sent Successfully", Toast.LENGTH_SHORT).show();
    }
}