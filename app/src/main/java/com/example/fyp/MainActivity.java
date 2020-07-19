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
import android.graphics.Matrix;
import android.graphics.PointF;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.fyp.customutilities.ImageUtilities;
import com.example.fyp.customutilities.SharedPreferencesUtils;
import com.example.fyp.customutilities.SharedValues;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    BluetoothAdapter mBluetoothAdapter;
    Button btn1, assistanceMode;
    ImageView imgv1;

    private static final int MY_PERMISSIONS_REQUEST_CAMERA_ACCESS = 5;
    private static final int MY_PERMISSIONS_REQUEST_CAMERA_ACCESS2 = 7;

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
                        Toast.makeText(getApplicationContext(), "You need to connect to your corresponding obd II device to enjoy accident detection feature.", Toast.LENGTH_LONG).show();
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
////                Intent intent = new Intent(getApplicationContext(),AssistantModeActivity.class);
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
            enableDisableBT();
            if (!SharedValues.isBlueToothConnected && mBluetoothAdapter.isEnabled()){
                Toast.makeText(getApplicationContext(), "You need to connect to your corresponding obd II device to enjoy accident detection feature.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(MainActivity.this, Bluetooth.class);
                startActivity(intent);
            }
        }



        SharedPreferences sp_ld = getSharedPreferences(getString(R.string.sp_laneDetection),0);
        String ld_key_t_mask = getString(R.string.sp_ld_key_transformed_mask_pts);
        String ld_key_mask = getString(R.string.sp_ld_key_original_mask_pts);

        SharedPreferences sp_bt = getSharedPreferences(getString(R.string.sp_blueTooth),0);
        String key_bt_conn = getString(R.string.sp_bt_key_isDeviceConnected);
        SharedPreferencesUtils.saveBool(sp_bt,key_bt_conn,false);
        String key_bt_speed = getString(R.string.sp_bt_key_car_speed);
        SharedPreferencesUtils.saveString(sp_bt,key_bt_speed,"25");

        SharedPreferences sp_hs = getSharedPreferences(getString(R.string.sp_homeSettings),0);
        String hs_preview_size = getString(R.string.sp_hs_key_previewSize);
        try {
            SharedPreferencesUtils.loadObject(sp_ld,ld_key_mask, PointF[].class);
            SharedPreferencesUtils.loadObject(sp_ld,ld_key_t_mask, PointF[].class);
//            SharedPreferencesUtils.loadObject(sp_hs,hs_preview_size,Size.class);
        }catch (IllegalArgumentException ex){
            PointF[] p_original = new PointF[4];
            p_original[0] = new PointF(0,0);
            p_original[1] = new PointF(100,0);
            p_original[2] = new PointF(100,100);
            p_original[3] = new PointF(0,100);

            SharedPreferencesUtils.saveObject(sp_ld,ld_key_mask,p_original);
            int auto_index = getAutomaticPreviewSize();
            Size auto = SharedValues.DESIRED_PREVIEW_SIZES[auto_index];
            Matrix matrix = ImageUtilities.getTransformationMatrix(auto.getWidth(),auto.getHeight(),
                    SharedValues.CROP_SIZE.getWidth(),SharedValues.CROP_SIZE.getHeight(),
                    0,false);
            PointF[] transformedPoints = new PointF[4];

            float[] pts_temp = new float[8];

            int j = 0;
            for (int i = 0; i < p_original.length; i++) {
                pts_temp[j] = p_original[i].x;
                pts_temp[j+1]= p_original[i].y;
                j+=2;
            }
            matrix.mapPoints(pts_temp);
            for (int i = 0; i < pts_temp.length; i+=2) {
                transformedPoints[i/2] = new PointF(pts_temp[i],pts_temp[i+1]);
            }
            SharedPreferencesUtils.saveObject(sp_ld,ld_key_t_mask,transformedPoints);
//            SharedPreferencesUtils.saveObject(sp_hs,hs_preview_size,auto);
            SharedPreferences.Editor editor = sp_hs.edit();
            editor.putInt(hs_preview_size,auto_index);
            editor.apply();
        }
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
        SharedPreferences sp_bt = getSharedPreferences(getString(R.string.sp_blueTooth),0);
        String key_bt_conn = getString(R.string.sp_bt_key_isDeviceConnected);
        if (!mBluetoothAdapter.isEnabled() ) {
            Log.d(TAG, "enableDisableBT: enabling BT.");

            Toast.makeText(getApplicationContext(), "You need to turn on your device's Bluetooth to enjoy accident detection feature.", Toast.LENGTH_LONG).show();
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);
        }
//        }else if (!SharedPreferencesUtils.loadBool(sp_bt,key_bt_conn)){
//            Toast.makeText(getApplicationContext(), "You need to connect to your corresponding obd II device to enjoy accident detection feature.", Toast.LENGTH_LONG).show();
//            Intent intent = new Intent(MainActivity.this, Bluetooth.class);
//            startActivity(intent);
//        }

    }

    @Override
    public void onBackPressed() {
        finishAffinity();
        System.exit(0);
        return;
    }
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
                    getCameraPermission2();
                    if (CameraPermissionsGranted) {
                        Intent intent = new Intent(this, MapsActivity.class);
                        startActivity(intent);
                    }
                }
            }
        } else if (v.getId() == imgv1.getId()) {

            Intent intent = new Intent(this, HomeSettings.class);
            startActivity(intent);
            finish();

        } else if (v.getId() == assistanceMode.getId()) {
            getCameraPermission();
            if (CameraPermissionsGranted) {
                Intent intent = new Intent(getApplicationContext(), LanePointsActivity.class);
                intent.putExtra(SharedValues.intent_to_assistant_mode,true);
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

    private void getCameraPermission2() {
        Log.d("TAG", "getCameraPermission: getting camera permissions");
        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED) {
            CameraPermissionsGranted = true;

        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_REQUEST_CAMERA_ACCESS2);
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
                    getCameraPermission2();
                    if (CameraPermissionsGranted) {
                        Intent intent = new Intent(this, MapsActivity.class);
                        startActivity(intent);
                    }
//                    Intent intent = new Intent(this, MapsActivity.class);
//                    startActivity(intent);
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
                    Intent intent = new Intent(getApplicationContext(), AssistantModeActivity.class);
                    startActivity(intent);
                    return;
                }
            }
            case MY_PERMISSIONS_REQUEST_CAMERA_ACCESS2: {
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
                    Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
                    startActivity(intent);
                }
            }

        }
    }

    public int getAutomaticPreviewSize() {
        Size[] DESIRED_PREVIEW_SIZES = SharedValues.DESIRED_PREVIEW_SIZES;
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int dheight = displayMetrics.heightPixels;
        int dwidth = displayMetrics.widthPixels;

        int height = Math.min(dheight,dwidth);
        int width = Math.max(dheight,dwidth);

        Log.d(TAG, String.format("getDesiredPreviewSize: (device resolution) device width = %d :height = %d", width,height));
        ArrayList<Size> temp = new ArrayList<>();
        ArrayList<Integer> indexes = new ArrayList<>();
        int i = 0;
        for (Size choice :
                DESIRED_PREVIEW_SIZES) {
            if (height == choice.getHeight()) {
                temp.add(choice);
                indexes.add(i);
            }
            i++;
        }

        if (temp.size() == 0) return 0;
        i = 0;
        for (i = 0; i < temp.size(); i++) {
            if (temp.get(i).getWidth() == width){
                return indexes.get(i);
            }else if (temp.get(i).getWidth() > width){
                return indexes.get(Math.max(i - 1, 0));
            }
        }
        return indexes.get(i-1);
//        return SharedValues.DESIRED_PREVIEW_SIZES[0];
    }
}

