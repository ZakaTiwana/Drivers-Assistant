package com.example.fyp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class HomeSettings extends AppCompatActivity implements View.OnClickListener {

    Switch accidentDetector;

    Switch voiceCommands;

    Switch darkui;

    ImageView backbutton;

    TextView featureSettings;

    TextView contactSettings;

    TextView featureSettingsText;

    TextView homeSettingText;

    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 1;
    private static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 4;

    private Boolean MicrophonePermissionsGranted = false;
    private Boolean SmsPermissionsGranted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_settings);


        accidentDetector = findViewById(R.id.switch1);

        voiceCommands = findViewById(R.id.switch2);

        darkui = findViewById(R.id.switch3);

        homeSettingText = findViewById(R.id.textView2);

        backbutton = (ImageView) findViewById(R.id.backbtn1);


        SharedPreferences settings = getSharedPreferences("home_settings", 0);
        boolean darkModeUi_value = settings.getBoolean("ui_settings", false);
        if (!darkModeUi_value) {
            ConstraintLayout constLayout;
            constLayout = findViewById(R.id.homesettings);
            constLayout.setBackgroundResource(R.drawable.backgroundimage8);

            TextView tv, tv1, tv2, tv3, tv4;
            tv = (TextView) findViewById(R.id.tv);
            tv1 = (TextView) findViewById(R.id.tv1);
            tv2 = (TextView) findViewById(R.id.tv2);
            tv3 = (TextView) findViewById(R.id.tv3);
            tv4 = (TextView) findViewById(R.id.tv4);
            accidentDetector.setTextColor(getResources().getColor(R.color.light_grey));
            voiceCommands.setTextColor(getResources().getColor(R.color.light_grey));
            darkui.setTextColor(getResources().getColor(R.color.light_grey));
            homeSettingText.setTextColor(getResources().getColor(R.color.dark_grey));
            backbutton.setImageResource(R.drawable.ic_back_button_black);
            tv.setTextColor(getResources().getColor(R.color.light_grey));
            tv1.setTextColor(getResources().getColor(R.color.dark_grey));
            tv2.setTextColor(getResources().getColor(R.color.dark_grey));
            tv3.setTextColor(getResources().getColor(R.color.dark_grey));
            tv4.setTextColor(getResources().getColor(R.color.dark_grey));
        }

//        TextView tv1=(TextView)findViewById(R.id.textView2);
//        tv1.setTextColor(getResources().getColor(R.color.dark_grey));
//        imgv1 = (ImageView) findViewById(R.id.backbtn1);
//        imgv1.setImageResource(R.drawable.ic_settings_black);
//        btn1.setTextColor(getResources().getColor(R.color.light_grey));
//        assistanceMode.setTextColor(getResources().getColor(R.color.light_grey));


        boolean accidentDetector_value = settings.getBoolean("accident_detector_settings", false);
        accidentDetector.setChecked(accidentDetector_value);

        boolean voice_commands_value = settings.getBoolean("voice_commands_settings", false);
        voiceCommands.setChecked(voice_commands_value);

        boolean darkModeUi_value2 = settings.getBoolean("ui_settings", false);
        darkui.setChecked(darkModeUi_value2);

        accidentDetector.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                SharedPreferences settings = getSharedPreferences("home_settings", 0);
                SharedPreferences.Editor editor = settings.edit();
                if (isChecked) {
                    getSmsPermission();
                    if (!SmsPermissionsGranted) {
                        accidentDetector.setChecked(false);
                    } else {
                        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

                        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                            accidentDetector.setChecked(false);
                            Toast.makeText(getApplicationContext(), "Please Enable GPS permission first.", Toast.LENGTH_LONG).show();
                            buildAlertMessageNoGps();
                        } else {
                            editor.putBoolean("accident_detector_settings", true);
                            editor.commit();
                            Toast.makeText(getApplicationContext(), "Accident Detector Settings Enabled", Toast.LENGTH_SHORT).show();
                        }
                    }

                } else {
                    editor.putBoolean("accident_detector_settings", false);
                    editor.commit();
                    Toast.makeText(getApplicationContext(), "Accident Detector Settings Disabled", Toast.LENGTH_SHORT).show();
                }

            }
        });


        voiceCommands.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                SharedPreferences settings = getSharedPreferences("home_settings", 0);
                SharedPreferences.Editor editor = settings.edit();
                if (isChecked) {
                    getMicrophonePermission();
                    if (!MicrophonePermissionsGranted) {
                        voiceCommands.setChecked(false);
                    } else {
                        editor.putBoolean("voice_commands_settings", true);
                        editor.commit();
                        Toast.makeText(getApplicationContext(), "Voice Commands Enabled", Toast.LENGTH_SHORT).show();
                    }

                } else {
                    editor.putBoolean("voice_commands_settings", false);
                    editor.commit();
                    Toast.makeText(getApplicationContext(), "Voice Commands Disabled", Toast.LENGTH_SHORT).show();
                }
            }
        });

        darkui.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                SharedPreferences settings = getSharedPreferences("home_settings", 0);
                SharedPreferences.Editor editor = settings.edit();
                if (isChecked) {
                    Toast.makeText(getApplicationContext(), "Dark Mode Enabled", Toast.LENGTH_SHORT).show();
                    editor.putBoolean("ui_settings", true);
                    editor.commit();
//                    Intent intent = new Intent(HomeSettings.this, MainActivity.class);
//                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    startActivity(intent);
//                   finish();
                    HomeSettings.this.recreate();

                } else {
                    Toast.makeText(getApplicationContext(), "Dark Mode Disabled", Toast.LENGTH_SHORT).show();
                    editor.putBoolean("ui_settings", false);
                    editor.commit();
//                    Intent intent = new Intent(HomeSettings.this, MainActivity.class);
//                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    startActivity(intent);
//                    finish();
                    HomeSettings.this.recreate();
                }


            }
        });


        backbutton.setOnClickListener(this);

        featureSettings = (TextView) findViewById(R.id.tv);

        featureSettings.setOnClickListener(this);

        contactSettings = (TextView) findViewById(R.id.tv1);

        contactSettings.setOnClickListener(this);

        featureSettingsText = (TextView) findViewById(R.id.tv3);

        featureSettingsText.setOnClickListener(this);

        homeSettingText.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        if (v.getId() == backbutton.getId()) {
            Intent intent = new Intent(HomeSettings.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else if (v.getId() == featureSettings.getId()) {
            Intent intent = new Intent(this, FeatureSettings.class);
            startActivity(intent);
        } else if (v.getId() == featureSettingsText.getId()) {
            Intent intent = new Intent(this, FeatureSettings.class);
            startActivity(intent);
        } else if (v.getId() == contactSettings.getId()) {
            Intent intent = new Intent(this, ContactsSettings.class);
            startActivity(intent);
        } else if (v.getId() == homeSettingText.getId()) {
            Intent intent = new Intent(this, AssistanceMode.class);
            startActivity(intent);
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(HomeSettings.this, MainActivity.class);
        startActivity(intent);
        finish();
        return;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent intent = new Intent(HomeSettings.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
        return true;
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
            ActivityCompat.requestPermissions(HomeSettings.this,
                    new String[]{Manifest.permission.SEND_SMS},
                    MY_PERMISSIONS_REQUEST_SEND_SMS);
        }
    }

    private void getMicrophonePermission() {
        Log.d("TAG", "getMicrophonePermission: getting microphone permission");
        SharedPreferences settings = getSharedPreferences("home_settings", 0);
        SharedPreferences.Editor editor = settings.edit();
        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED) {
            MicrophonePermissionsGranted = true;
        } else {
            ActivityCompat.requestPermissions(HomeSettings.this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d("TAG", "onRequestPermissionsResult: called.");

        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_SEND_SMS: {
                SharedPreferences settings = getSharedPreferences("home_settings", 0);
                SharedPreferences.Editor editor = settings.edit();
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
                        accidentDetector.setChecked(false);
                        Toast.makeText(getApplicationContext(), "Please Enable GPS permission first.", Toast.LENGTH_LONG).show();
                        buildAlertMessageNoGps();
                    } else {
                        accidentDetector.setChecked(true);
                        editor.putBoolean("accident_detector_settings", true);
                        editor.commit();
                        Toast.makeText(getApplicationContext(), "Accident Detector Settings Enabled", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }
            case MY_PERMISSIONS_REQUEST_RECORD_AUDIO: {
                SharedPreferences settings = getSharedPreferences("home_settings", 0);
                SharedPreferences.Editor editor = settings.edit();
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            MicrophonePermissionsGranted = false;
                            Log.d("TAG", "onRequestPermissionsResult: permission failed");
                            Toast.makeText(getApplicationContext(), "Please Enable Microphone permission first.", Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    voiceCommands.setChecked(true);
                    Log.d("TAG", "onRequestPermissionsResult: permission granted");
                    MicrophonePermissionsGranted = true;
                    editor.putBoolean("voice_commands_settings", true);
                    editor.commit();
                    Toast.makeText(getApplicationContext(), "Voice Commands Enabled", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


}
