package com.example.fyp;


import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class StepsParser extends AsyncTask<String, Integer, ArrayList<String>>{
    TaskLoaded2Callback taskCallback;
    String directionMode = "driving";

    public StepsParser(Context mContext, String directionMode) {
        this.taskCallback = (TaskLoaded2Callback) mContext;
        this.directionMode = directionMode;
    }

    // Parsing the data in non-ui thread
    @Override
    protected ArrayList<String> doInBackground(String... jsonData) {

        JSONObject jObject;
      //  List<List<HashMap<String, String>>> routes = null;
        ArrayList<String> stepsInfo = new ArrayList<String>();

        try {
            jObject = new JSONObject(jsonData[0]);
            Log.d("mylog", jsonData[0].toString());
            DataParser parser = new DataParser();
            Log.d("mylog", parser.toString());

            // Starts parsing data
     //       routes = parser.parse(jObject);
            stepsInfo = parser.parseSteps(jObject);

            Log.d("mylog", "Executing routes");
      //      Log.d("mylog", routes.toString());

        } catch (Exception e) {
            Log.d("mylog", e.toString());
            e.printStackTrace();
        }
        return stepsInfo;
    }

    // Executes in UI thread, after the parsing process
    @Override
    protected void onPostExecute(ArrayList<String> result) {
        Log.d("stepsinfo", ": "+result);
        if (result != null) {
            //mMap.addPolyline(lineOptions);
            taskCallback.onTaskDone2(result);

        } else {
            Log.d("stepsinfo", "No steps found");
        }
    }
}