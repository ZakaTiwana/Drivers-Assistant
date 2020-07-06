package com.example.fyp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
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

        btn_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(),ImageProcessor.class);
                startActivity(i);
            }
        });

        btn_to_sign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext() , TestSignActivity.class);
                startActivity(i);
            }
        });

        btn_lane.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent i = new Intent(getApplicationContext(),TestLaneActivity.class);
                startActivity(i);
            }
        });

        btn_distance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext() , TestObjectDistance.class);
                startActivity(i);
            }
        });
    }
}
