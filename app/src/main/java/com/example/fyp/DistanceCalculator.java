package com.example.fyp;

import android.graphics.RectF;
import android.util.Log;


public class DistanceCalculator {
    private static final String TAG = "DistanceCalculator";
    private static final float KNOWN_DISTANCE = 1f;     // 1 meter for testing
//    private static final float KNOWN_DISTANCE = 0.5f;     // 1 meter for testing
    private static final float ACTUAL_WIDTH_BOTTLE = 0.127f;   // 0.127 meter
    private static final float ACTUAL_WIDTH_CAR = 1.5f;// avg width of car
    private static final float FOCAL_LENGTH = 950f;

    private RectF location;
//    private float focalLength;
    private float perceived_width;
    private String label;

    private float distance;

    public DistanceCalculator(RectF location,String label){
        this.location = location;
        this.label = label;
        perceived_width = location.width();
        Log.d(TAG, String.format("DistanceCalculator: precived_width = %f", perceived_width));
//        this.focalLength = calculateFocalLength();
//        Log.d(TAG, String.format("DistanceCalculator: focal length = %f",   this.focalLength ));
        distance = calculateDistance();
    }

//    private float calculateFocalLength(){
//        return (precived_width * KNOWN_DISTANCE)/ ACTUAL_WIDTH;
//    }

    private float calculateDistance(){

        float ACTUAL_WIDTH = 1f; // default for any other except car and bottle
        if (label.equalsIgnoreCase("car")){
            ACTUAL_WIDTH = ACTUAL_WIDTH_CAR;
        }else if(label.equalsIgnoreCase("bottle")){
            ACTUAL_WIDTH = ACTUAL_WIDTH_BOTTLE;
        }

        return  ( ACTUAL_WIDTH * FOCAL_LENGTH ) / this.perceived_width;
    }

    public float getDistance(){
        return distance;
    }
}