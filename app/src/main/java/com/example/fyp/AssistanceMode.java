package com.example.fyp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class AssistanceMode extends AppCompatActivity {

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
                Intent i = new Intent(getApplicationContext(), ImageProcessor.class);
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
    }
}
