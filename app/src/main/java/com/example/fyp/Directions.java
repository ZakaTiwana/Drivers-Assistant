package com.example.fyp;

import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;

import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.location.config.LocationAccuracy;
import io.nlopez.smartlocation.location.config.LocationParams;

public class Directions extends AsyncTask<Object, String, String> {
    private LatLng fromPosition = new LatLng(0.0, 0.0);
    private WeakReference<Context> contextRef;
    DestinationActivity da=new DestinationActivity();
    private TextToSpeech tts;
//    private ImageView maneuverIcon;

    public Directions(Context context) {
        contextRef = new WeakReference<>(context);


    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(Object... params) {
        //initializeTextToSpeech();
        Context context = contextRef.get();
        getDeviceLocation();
        ArrayList<String> arrayList = new ArrayList<String>();
        arrayList = (ArrayList<String>)params[0];
        tts=(TextToSpeech)params[1];
        String[] step = new String[5];
        String distance;
        String instructions;
        double lat;
        double lng;
        LatLng latlng;
        String maneuver;
//        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
//            @Override
//            public void onInit(int status) {
//                if (tts.getEngines().size() == 0) {
////                    Toast.makeText(MicTest.this, "No TTS engine on your device", Toast.LENGTH_LONG).show();
////                    finish();
//                } else {
//                    tts.setLanguage(Locale.getDefault());
//                    tts.setLanguage(Locale.US);
//                    //   Toast.makeText(getApplicationContext(),"check"+Locale.getDefault(),Toast.LENGTH_SHORT);
//                    Log.d("check", "language: " + Locale.getDefault());
//                    speak("Hello there!");
////                    speak("Oye bhai kesa hai");
//                }
//            }
//        });
        // maneuverIcon = (ImageView) getActivity.findViewById(R.id.ic_maneuver);
        for (int i = 0; i < arrayList.size(); i++) {
            step = arrayList.get(i).split("::");
            distance = step[0];
            instructions = step[1];
            instructions = instructions.replaceAll("\\<.*?\\>", "");
            String lat1 = step[2];
            lat = Double.parseDouble(step[2]);
            lng = Double.parseDouble(step[3]);
            maneuver = step[4];
            //       latlng=new LatLng(lat, lng);
            while (true) {
                Log.d("check", "instructions loop");
                if (Math.abs(lat - fromPosition.latitude) < 0.0001) {
                    if (Math.abs(lng - fromPosition.longitude) < 0.0001) {
                        tts.speak(instructions, TextToSpeech.QUEUE_FLUSH, null, null);
                        break;
                    }
                }
                if (Math.abs(lng - fromPosition.longitude) < 0.0001) {
                    if (Math.abs(lat - fromPosition.latitude) < 0.0001) {
                        tts.speak(instructions, TextToSpeech.QUEUE_FLUSH, null, null);
                        break;
//                        if(maneuver!=null){
//                            //icon source change krni hai
//
//                            //icon ko visible krna hai
//
//                        }
                    }
                }

            }

        }

        return "a";
    }


    private void getDeviceLocation() {
        Log.d("TAG", "getDeviceLocation: getting the devices current location");
        Context context = contextRef.get();
        try {


            SmartLocation smartLocation = null;
            LocationParams.Builder builder;
            smartLocation = new SmartLocation.Builder(context).logging(true).build();
            builder = new LocationParams.Builder()
                    .setAccuracy(LocationAccuracy.HIGH)
                    .setDistance(0)
                    .setInterval(5000);
            try {
                smartLocation.with(context)
                        .location()
                        .config(LocationParams.BEST_EFFORT)
                        .continuous()
                        .config(builder.build())
                        .start(new OnLocationUpdatedListener() {
                            @Override
                            public void onLocationUpdated(Location location) {
                                fromPosition = new LatLng(location.getLatitude(), location.getLongitude());

                            }
                        });
            } catch (SecurityException se) {
                se.printStackTrace();
            }

        } catch (SecurityException e) {
            Log.e("TAG", "getDeviceLocation: SecurityException: " + e.getMessage());
        }
    }


    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

    }

}


