package com.example.fyp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

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
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    BluetoothAdapter mBluetoothAdapter;
    Button btn1, assistanceMode;
    ImageView imgv1;

    private static final int MY_PERMISSIONS_REQUEST_CAMERA_ACCESS = 3;

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

        btn1.setOnClickListener(this);

        imgv1 = (ImageView) findViewById(R.id.backbtn1);

        imgv1.setOnClickListener(this);
        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // Toast.makeText(this, "Enable gps", Toast.LENGTH_SHORT).show();
            buildAlertMessageNoGps();

        }

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        SharedPreferences settings = getSharedPreferences("home_settings", 0);
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
        unregisterReceiver(mBroadcastReceiver1);

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

    @Override
    public void onClick(View v) {
        if (v.getId() == btn1.getId()) {
            Intent intent = new Intent(this, MapsActivity.class);
            startActivity(intent);
        } else if (v.getId() == imgv1.getId()) {
            Intent intent = new Intent(this, HomeSettings.class);
            startActivity(intent);
        } else if (v.getId() == assistanceMode.getId()) {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.CAMERA) !=
                    PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        MY_PERMISSIONS_REQUEST_CAMERA_ACCESS);
            } else {
                Intent intent = new Intent(getApplicationContext(), ImageProcessor.class);
                startActivity(intent);
            }
        }
    }
}

