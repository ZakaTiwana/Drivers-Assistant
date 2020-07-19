package com.example.fyp;

import android.graphics.RectF;
import android.util.Log;


public class DistanceCalculator {
    private static final String TAG = "DistanceCalculator";
    private static final float ACTUAL_WIDTH_BOTTLE = 0.127f;   // 0.127 meter
    private static final float ACTUAL_WIDTH_CAR = 1.5f;        // avg width of car in meter
    private static final float ACTUAL_WIDTH_PERSON = 0.6f;
    private static final float ACTUAL_WIDTH_TRUCK = 2.8f;
    private static final float ACTUAL_WIDTH_MOTORCYCLE = 0.8f;
    private static final float ACTUAL_WIDTH_BICYCLE = 0.6f;

    private static final float FOCAL_LENGTH = 950f;

    private float distance;
    public DistanceCalculator(){ }

    public float calculateDistance(RectF location, String label){
        float perceived_width = location.width();
        float ACTUAL_WIDTH = 1f;            // default for any other
        switch (label.toLowerCase()){
            case "bottle":
                ACTUAL_WIDTH = ACTUAL_WIDTH_BOTTLE;
                break;
            case "car":
                ACTUAL_WIDTH = ACTUAL_WIDTH_CAR;
                break;
            case "person":
                ACTUAL_WIDTH = ACTUAL_WIDTH_PERSON;
                break;
            case "bicycle":
                ACTUAL_WIDTH = ACTUAL_WIDTH_BICYCLE;
                break;
            case "motorcycle":
                ACTUAL_WIDTH = ACTUAL_WIDTH_MOTORCYCLE;
                break;
            case "truck":
                ACTUAL_WIDTH = ACTUAL_WIDTH_TRUCK;
                break;
        }
        distance =( ACTUAL_WIDTH * FOCAL_LENGTH ) / perceived_width;
        return  distance;
    }

    public float getDistance(){
        return distance;
    }
}