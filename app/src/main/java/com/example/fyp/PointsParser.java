package com.example.fyp;


import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Vishal on 10/20/2018.
 */

public class PointsParser extends AsyncTask< String, Integer, List<List<LatLng>> > {
    TaskLoadedCallback taskCallback;
    String directionMode = "driving";

    public PointsParser(Context mContext, String directionMode) {
        this.taskCallback = (TaskLoadedCallback) mContext;
        this.directionMode = directionMode;
    }

    // Parsing the data in non-ui thread
    @Override
    protected List<List<LatLng>> doInBackground(String... jsonData) {

        JSONObject jObject;
        List<List<LatLng>> listOfPolyline = null;

        try {
            jObject = new JSONObject(jsonData[0]);
            Log.d("mylog", jsonData[0].toString());
            DataParser parser = new DataParser();
            Log.d("mylog", parser.toString());

            // Starts parsing data
            listOfPolyline = parser.parse(jObject);
            Log.d("mylog", "Executing routes");
//            Log.d("mylog", listOfPolyline.toString());

        } catch (Exception e) {
            Log.d("mylog", e.toString());
            e.printStackTrace();
        }
        return listOfPolyline;
    }

    // Executes in UI thread, after the parsing process
    @Override
    protected void onPostExecute(List<List<LatLng>> result) {
        List<PolylineOptions> lineOptions = new ArrayList<>();
        // Traversing through all the routes
        for (int i = 0; i < result.size(); i++) {
            PolylineOptions lineOption = new PolylineOptions();
            // Fetching i-th route
            List<LatLng> path = result.get(i);
            // Fetching all the points in i-th route
            for (int j = 0; j < path.size(); j++) {
                lineOption.add(path.get(j));
            }

            lineOption.width(20);
            if (i % 2 == 0) lineOption.color(Color.BLUE);
            else            lineOption.color(Color.RED);

            lineOptions.add(lineOption);
            Log.d("mylog", "onPostExecute lineoptions decoded");
        }

        // Drawing polyline in the Google Map for the i-th route
        if (lineOptions != null) {
            //mMap.addPolyline(lineOptions);
            taskCallback.onTaskDone(lineOptions);

        } else {
            Log.d("mylog", "without Polylines drawn");
        }
    }
}