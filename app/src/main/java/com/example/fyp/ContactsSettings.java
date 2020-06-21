package com.example.fyp;


import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;

import android.view.View;

import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


public class ContactsSettings extends AppCompatActivity implements View.OnClickListener {

    ImageView backbutton;

    Button addContacts;

    Button viewContacts;

    TextView tv1;

    Button test;

    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 1;
    private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 2;




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
                    Intent intent = new Intent(this, Sms.class);
                    startActivity(intent);

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

}
