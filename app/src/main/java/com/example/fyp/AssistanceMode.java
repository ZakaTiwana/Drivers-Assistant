package com.example.fyp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.example.fyp.customutilities.SharedValues;

import java.util.ArrayList;

public class AssistanceMode extends AppCompatActivity {

    private static final String TAG = "AssistanceMode";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assistance_mode);

        Button btn_camera = findViewById(R.id.btn_camera);
        Button btn_lane = findViewById(R.id.btn_lane);
        Button btn_to_sign = findViewById(R.id.btn_to_sign);
        Button btn_distance = findViewById(R.id.btn_to_distance);
        Button btn_to_video = findViewById(R.id.btn_to_video);
        Button btn_to_lane_adv = findViewById(R.id.btn_to_lane_adv);

        btn_to_lane_adv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(),TestLaneAdvanceActivity.class);
                startActivity(i);
            }
        });

        btn_to_video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(),TestVideoActivity.class);
                startActivity(i);
            }
        });

        SharedPreferences settings = getSharedPreferences("home_settings", 0);
        boolean darkModeUi_value = settings.getBoolean("ui_settings", false);
        if (!darkModeUi_value) {
            ConstraintLayout constLayout;
            constLayout = findViewById(R.id.assistancemode);
            constLayout.setBackgroundResource(R.drawable.backgroundimage8);
            btn_camera.setTextColor(getResources().getColor(R.color.light_grey));
            btn_lane.setTextColor(getResources().getColor(R.color.light_grey));
            btn_to_sign.setTextColor(getResources().getColor(R.color.light_grey));
            btn_distance.setTextColor(getResources().getColor(R.color.light_grey));

        }

        btn_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), AssistantModeActivity.class);
                startActivity(i);
            }
        });

        btn_to_sign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), TestSignActivity.class);
                startActivity(i);
            }
        });

        btn_lane.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent i = new Intent(getApplicationContext(), TestLaneActivity.class);
                startActivity(i);
            }
        });

        btn_distance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), TestObjectDistance.class);
                startActivity(i);
            }
        });

        Spinner spinner = findViewById(R.id.spinner_preview_size);
        ArrayList<String> sizes = new ArrayList<>();
        for (Size s :
                SharedValues.DESIRED_PREVIEW_SIZES) {
            sizes.add(s.toString());
        }
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(getApplicationContext(),android.R.layout.simple_spinner_dropdown_item, sizes);
        adapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Size size = SharedValues.DESIRED_PREVIEW_SIZES[position];
                SharedPreferences sp_hs = getSharedPreferences(getString(R.string.sp_homeSettings),0);
                String hs_preview_size = getString(R.string.sp_hs_key_previewSize);
                Log.d(TAG, "onItemSelected: size = "+size.toString());
                SharedPreferences.Editor editor = sp_hs.edit();
                editor.putInt(hs_preview_size,position);
                editor.apply();
//                SharedPreferencesUtils.saveObject(sp_hs,hs_preview_size,size);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }
}
