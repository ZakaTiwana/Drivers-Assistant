package com.example.fyp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class HomeSettings extends AppCompatActivity implements View.OnClickListener {

    Switch accidentDetector;

    Switch voiceCommands;

    ImageView backbutton;

    TextView featureSettings;

    TextView contactSettings;

    TextView featureSettingsText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_settings);


        accidentDetector = findViewById(R.id.switch1);

        voiceCommands = findViewById(R.id.switch2);

        SharedPreferences settings = getSharedPreferences("home_settings", 0);

        boolean accidentDetector_value = settings.getBoolean("accident_detector_settings", false);
        accidentDetector.setChecked(accidentDetector_value);

        boolean voice_commands_value = settings.getBoolean("voice_commands_settings", false);
        voiceCommands.setChecked(voice_commands_value);

       accidentDetector.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {

                if (isChecked) {
                    Toast.makeText(getApplicationContext(), "true", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "false", Toast.LENGTH_SHORT).show();
                }
                SharedPreferences settings = getSharedPreferences("home_settings", 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("accident_detector_settings", isChecked);
                editor.commit();
            }
        });


        voiceCommands.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {

                if (isChecked) {
                    Toast.makeText(getApplicationContext(), "true", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "false", Toast.LENGTH_SHORT).show();
                }
                SharedPreferences settings = getSharedPreferences("home_settings", 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("voice_commands_settings", isChecked);
                editor.commit();
            }
        });

        backbutton=(ImageView)findViewById(R.id.backbtn1);

        backbutton.setOnClickListener(this);

        featureSettings=(TextView)findViewById(R.id.tv);

        featureSettings.setOnClickListener(this);

        contactSettings=(TextView)findViewById(R.id.tv1);

        contactSettings.setOnClickListener(this);

        featureSettingsText=(TextView)findViewById(R.id.tv3);

        featureSettingsText.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        if(v.getId()==backbutton.getId()){
            finish();
        }
        else if(v.getId()==featureSettings.getId()){
            Intent intent = new Intent(this, FeatureSettings.class);
            startActivity(intent);
        }
        else if(v.getId()==featureSettingsText.getId()){
            Intent intent = new Intent(this, FeatureSettings.class);
            startActivity(intent);
        }
        else if(v.getId()==contactSettings.getId()){
            Intent intent = new Intent(this, ContactsSettings.class);
            startActivity(intent);
        }
    }
}
