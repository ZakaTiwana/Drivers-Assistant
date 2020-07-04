package com.example.fyp;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;

import android.util.Log;
import android.view.View;

import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


public class ContactsSettings extends AppCompatActivity implements View.OnClickListener {

    ImageView backbutton;

    Button addContacts;

    Button viewContacts;

    TextView tv1;

    Button test;

    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 1;
    private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 2;

    private Boolean SmsPermissionsGranted = false;
    private Boolean ContactPermissionsGranted = false;


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



        SharedPreferences Home_settings = getSharedPreferences("home_settings", 0);
        boolean darkModeUi_value = Home_settings.getBoolean("ui_settings", false);
        if (!darkModeUi_value) {
            ConstraintLayout constLayout;
            constLayout = findViewById(R.id.contactsettings);
            constLayout.setBackgroundResource(R.drawable.backgroundimage8);
            backbutton.setImageResource(R.drawable.ic_back_button_black);
            tv1.setTextColor(getResources().getColor(R.color.dark_grey));
            addContacts.setTextColor(getResources().getColor(R.color.light_grey));
            viewContacts.setTextColor(getResources().getColor(R.color.light_grey));
            test.setTextColor(getResources().getColor(R.color.light_grey));
            ImageView img1=(ImageView)findViewById(R.id.imageView);
            img1.setImageResource(R.drawable.ic_contact_diary_black);
        }

        test.setVisibility(View.GONE);

    }


    @Override
    public void onClick(View v) {

        if (v.getId() == backbutton.getId()) {
            finish();
        } else if (v.getId() == addContacts.getId()) {
            getContactPermission();
            if (ContactPermissionsGranted) {
                Intent intent = new Intent(this, AddContacts.class);
                startActivity(intent);
            }


        } else if (v.getId() == viewContacts.getId()) {
            Intent intent = new Intent(this, ViewTrustedContacts.class);
            startActivity(intent);
        } else if (v.getId() == tv1.getId()) {
            test.setVisibility(View.VISIBLE);
        } else if (v.getId() == test.getId()) {
            getSmsPermission();
            if (SmsPermissionsGranted) {
                LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    Toast.makeText(getApplicationContext(), "Please Enable GPS permission first.", Toast.LENGTH_LONG).show();
                    buildAlertMessageNoGps();
                } else {
                    Intent intent = new Intent(this, Sms.class);
                    startActivity(intent);
                }
            }


        }
    }

    private void getSmsPermission() {
        Log.d("TAG", "getSmsPermission: getting sms permission");
        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.SEND_SMS) ==
                PackageManager.PERMISSION_GRANTED) {
            // Permission not yet granted. Use requestPermissions().
            // MY_PERMISSIONS_REQUEST_SEND_SMS is an
            // app-defined int constant. The callback method gets the
            // result of the request.
            SmsPermissionsGranted = true;
        } else {
            ActivityCompat.requestPermissions(ContactsSettings.this,
                    new String[]{Manifest.permission.SEND_SMS},
                    MY_PERMISSIONS_REQUEST_SEND_SMS);
        }
    }

    private void getContactPermission() {
        Log.d("TAG", "getContactPermission: getting contact permission");
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED) {
            ContactPermissionsGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS},
                    MY_PERMISSIONS_REQUEST_READ_CONTACTS);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d("TAG", "onRequestPermissionsResult: called.");
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            ContactPermissionsGranted = false;
                            Toast.makeText(getApplicationContext(), "Please Enable Contact permission first.", Toast.LENGTH_LONG).show();
                            Log.d("TAG", "onRequestPermissionsResult: permission failed");
                            return;
                        }
                    }
                    Log.d("TAG", "onRequestPermissionsResult: permission granted");
                    ContactPermissionsGranted = true;
                    Intent intent = new Intent(this, AddContacts.class);
                    startActivity(intent);
                    return;
                }
            }
            case MY_PERMISSIONS_REQUEST_SEND_SMS: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            SmsPermissionsGranted = false;
                            Log.d("TAG", "onRequestPermissionsResult: permission failed");
                            Toast.makeText(getApplicationContext(), "Please Enable SMS permission first.", Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    Log.d("TAG", "onRequestPermissionsResult: permission granted");
                    SmsPermissionsGranted = true;
                    LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        Toast.makeText(getApplicationContext(), "Please Enable GPS permission first.", Toast.LENGTH_LONG).show();
                        buildAlertMessageNoGps();
                    } else {
                        Intent intent = new Intent(getApplicationContext(), Sms.class);
                        startActivity(intent);

                    }
                }
            }
        }
    }


}
