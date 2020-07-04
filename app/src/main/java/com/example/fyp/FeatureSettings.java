package com.example.fyp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class FeatureSettings extends AppCompatActivity implements View.OnClickListener {

    ImageView backButton;

    Switch laneGuide, distanceCalculator, objectDetection, signDetection, muteWarnings;

    TextView testbtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feature_settings);

        testbtn=(TextView)findViewById(R.id.textView);
        testbtn.setOnClickListener(this);

        laneGuide = findViewById(R.id.switch1);
        distanceCalculator = findViewById(R.id.switch2);
        objectDetection = findViewById(R.id.switch3);
        signDetection = findViewById(R.id.switch4);
        muteWarnings = findViewById(R.id.switch5);

        backButton = (ImageView) findViewById(R.id.backbtn1);

        SharedPreferences Home_settings = getSharedPreferences("home_settings", 0);
        boolean darkModeUi_value = Home_settings.getBoolean("ui_settings", false);
        if (!darkModeUi_value) {
            ConstraintLayout constLayout;
            TextView tv1,tv2,tv3,tv4,tv5;
            tv1=(TextView)findViewById(R.id.textView1);
            tv2=(TextView)findViewById(R.id.textView2);
            tv3=(TextView)findViewById(R.id.textView3);
            tv4=(TextView)findViewById(R.id.textView4);
            tv5=(TextView)findViewById(R.id.textView5);
            constLayout = findViewById(R.id.featuresettings);
            constLayout.setBackgroundResource(R.drawable.backgroundimage8);
            laneGuide.setTextColor(getResources().getColor(R.color.light_grey));
            distanceCalculator.setTextColor(getResources().getColor(R.color.light_grey));
            objectDetection.setTextColor(getResources().getColor(R.color.light_grey));
            signDetection.setTextColor(getResources().getColor(R.color.light_grey));
            muteWarnings.setTextColor(getResources().getColor(R.color.light_grey));
            testbtn.setTextColor(getResources().getColor(R.color.dark_grey));
            backButton.setImageResource(R.drawable.ic_back_button_black);
            tv1.setTextColor(getResources().getColor(R.color.dark_grey));
            tv2.setTextColor(getResources().getColor(R.color.dark_grey));
            tv3.setTextColor(getResources().getColor(R.color.dark_grey));
            tv4.setTextColor(getResources().getColor(R.color.dark_grey));
            tv5.setTextColor(getResources().getColor(R.color.dark_grey));
        }


        SharedPreferences settings = getSharedPreferences("feature_settings", 0);

        boolean laneGuide_value = settings.getBoolean("lane_guide", false);
        laneGuide.setChecked(laneGuide_value);

        boolean distanceCalculator_value = settings.getBoolean("distance_calculator", false);
        distanceCalculator.setChecked(distanceCalculator_value);

        boolean objectDetection_value = settings.getBoolean("object_detection", false);
        objectDetection.setChecked(objectDetection_value);

        boolean signDetection_value = settings.getBoolean("sign_detection", false);
        signDetection.setChecked(signDetection_value);

        boolean muteWarnings_value = settings.getBoolean("mute_warnings", false);
        muteWarnings.setChecked(muteWarnings_value);


        laneGuide.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {

                if (isChecked) {
                    Toast.makeText(getApplicationContext(), "Lane Guide Enabled", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Lane Guide Disabled", Toast.LENGTH_SHORT).show();
                }
                SharedPreferences settings = getSharedPreferences("feature_settings", 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("lane_guide", isChecked);
                editor.commit();
            }
        });

        distanceCalculator.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {

                if (isChecked) {
                    Toast.makeText(getApplicationContext(), "Distance Calculator Enabled", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Distance Calculator Disabled", Toast.LENGTH_SHORT).show();
                }
                SharedPreferences settings = getSharedPreferences("feature_settings", 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("distance_calculator", isChecked);
                editor.commit();
            }
        });

        objectDetection.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {

                if (isChecked) {
                    Toast.makeText(getApplicationContext(), "Object Detector Enabled", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Object Detector Disabled", Toast.LENGTH_SHORT).show();
                }
                SharedPreferences settings = getSharedPreferences("feature_settings", 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("object_detection", isChecked);
                editor.commit();
            }
        });

        signDetection.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {

                if (isChecked) {
                    Toast.makeText(getApplicationContext(), "Sign Detector Enabled", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Sign Detector Disabled", Toast.LENGTH_SHORT).show();
                }
                SharedPreferences settings = getSharedPreferences("feature_settings", 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("sign_detection", isChecked);
                editor.commit();
            }
        });

        muteWarnings.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {

                if (isChecked) {
                    Toast.makeText(getApplicationContext(), "Mute Warnings Enabled", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Mute Warning Disabled", Toast.LENGTH_SHORT).show();
                }
                SharedPreferences settings = getSharedPreferences("feature_settings", 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("mute_warnings", isChecked);
                editor.commit();
            }
        });



        backButton.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        if (v.getId() == backButton.getId()) {
            finish();
        }
        else if(v.getId()==testbtn.getId()){
            Intent intent = new Intent(this, MicTest.class);
            startActivity(intent);
        }
    }


}
