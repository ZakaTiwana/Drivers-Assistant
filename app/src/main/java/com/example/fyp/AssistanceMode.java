package com.example.fyp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class AssistanceMode extends AppCompatActivity {

    private Button btn_camera,btn_lane,btn_to_sign ,btn_distance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assistance_mode);

        btn_camera = findViewById(R.id.btn_camera);
        btn_lane = findViewById(R.id.btn_lane);
        btn_to_sign = findViewById(R.id.btn_to_sign);
        btn_distance = findViewById(R.id.btn_to_distance);

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
