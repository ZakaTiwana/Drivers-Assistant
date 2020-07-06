package com.example.fyp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    BluetoothAdapter mBluetoothAdapter;
    Button btn1, assistanceMode;
    ImageView imgv1;

    private static final int MY_PERMISSIONS_REQUEST_CAMERA_ACCESS = 5;

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 6;
    private Boolean mLocationPermissionsGranted = false;
    private Boolean CameraPermissionsGranted = false;


    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (action.equals(mBluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, mBluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE ON");
                        intent = new Intent(MainActivity.this, Bluetooth.class);
                        startActivity(intent);
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING ON");
                        break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn1 = (Button) findViewById(R.id.btn);
        assistanceMode = findViewById(R.id.btn1);
        btn1.setOnClickListener(this);
        assistanceMode.setOnClickListener(this);

//        assistanceMode.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
////                Intent intent = new Intent(getApplicationContext(), AssistanceMode.class);
////                startActivity(intent);
//
////                Intent intent = new Intent(getApplicationContext(),ImageProcessor.class);
////                startActivity(intent);
//
//        });
        TextView tv1 = (TextView) findViewById(R.id.textView2);
        imgv1 = (ImageView) findViewById(R.id.backbtn1);
        SharedPreferences settings = getSharedPreferences("home_settings", 0);
        boolean darkModeUi_value = settings.getBoolean("ui_settings", false);
        if (!darkModeUi_value) {
            ConstraintLayout constLayout;
            constLayout = findViewById(R.id.mainview);
            constLayout.setBackgroundResource(R.drawable.backgroundimage8);
            tv1.setTextColor(getResources().getColor(R.color.dark_grey));
            imgv1.setImageResource(R.drawable.ic_settings_black);
            btn1.setTextColor(getResources().getColor(R.color.light_grey));
            assistanceMode.setTextColor(getResources().getColor(R.color.light_grey));
            ImageView img2=(ImageView)findViewById(R.id.imageView3);
            img2.setImageResource(R.drawable.app_ic_grey);
        }


        imgv1.setOnClickListener(this);
        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // Toast.makeText(this, "Enable gps", Toast.LENGTH_SHORT).show();
            buildAlertMessageNoGps();

        }

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        SharedPreferences settings = getSharedPreferences("home_settings", 0);
        if (settings.getBoolean("accident_detector_settings", false)) {
            //  Toast.makeText(getApplicationContext(), "Accident Detector Settings Enabled", Toast.LENGTH_SHORT).show();
            enableDisableBT();
        }
//        Toast.makeText(getApplicationContext(), "Accident Detector Settings Disabled", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: called.");
        super.onDestroy();
        try {
            unregisterReceiver(mBroadcastReceiver1);
            //Register or UnRegister your broadcast receiver here

        } catch(IllegalArgumentException e) {

            e.printStackTrace();
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

    public void enableDisableBT() {
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "enableDisableBT: Does not have BT capabilities.");
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "enableDisableBT: enabling BT.");
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);
        }

    }

//    @Override
//    public void onBackPressed() {
//        finish();
//        System.exit(0);
//        return;
//    }
//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event)
//    {
//        if(keyCode==KeyEvent.KEYCODE_BACK)
//        {
//            finish();
//            System.exit(0);
//        }
//        return true;
//    }

    @Override
    public void onClick(View v) {
        if (v.getId() == btn1.getId()) {
            getLocationPermission();
            if (mLocationPermissionsGranted) {
                LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if(!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    buildAlertMessageNoGps();
                }else{
                    Intent intent = new Intent(this, MapsActivity.class);
                    startActivity(intent);
                }
            }
        } else if (v.getId() == imgv1.getId()) {

            Intent intent = new Intent(this, HomeSettings.class);
            startActivity(intent);
            finish();

        } else if (v.getId() == assistanceMode.getId()) {
            getCameraPermission();
            if (CameraPermissionsGranted) {
                Intent intent = new Intent(getApplicationContext(), ImageProcessor.class);
                startActivity(intent);
            }
        }
    }

    private void getCameraPermission() {
        Log.d("TAG", "getCameraPermission: getting camera permissions");
        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED) {
            CameraPermissionsGranted = true;

        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_REQUEST_CAMERA_ACCESS);
        }
    }


    private void getLocationPermission() {
        Log.d("TAG", "getLocationPermission: getting location permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionsGranted = true;
            } else {
                ActivityCompat.requestPermissions(this,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: called.");
        mLocationPermissionsGranted = false;

        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            mLocationPermissionsGranted = false;
                            Toast.makeText(getApplicationContext(), "Please Enable Location permission first.", Toast.LENGTH_LONG).show();
                            Log.d(TAG, "onRequestPermissionsResult: permission failed");
                            return;
                        }
                    }
                    Log.d(TAG, "onRequestPermissionsResult: permission granted");
                    mLocationPermissionsGranted = true;
                    Intent intent = new Intent(this, MapsActivity.class);
                    startActivity(intent);
                    return;
                }
            }
            case MY_PERMISSIONS_REQUEST_CAMERA_ACCESS: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            CameraPermissionsGranted = false;
                            Log.d(TAG, "onRequestPermissionsResult: permission failed");
                            Toast.makeText(getApplicationContext(), "Please Enable Camera permission first.", Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    Log.d(TAG, "onRequestPermissionsResult: permission granted");
                    CameraPermissionsGranted = true;
                    Intent intent = new Intent(getApplicationContext(), ImageProcessor.class);
                    startActivity(intent);
                }
            }
        }
    }
}

