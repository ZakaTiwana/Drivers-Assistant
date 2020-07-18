package com.example.fyp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;


import com.example.fyp.customutilities.SharedValues;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;


import java.util.ArrayList;

import java.util.Arrays;

import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.location.config.LocationAccuracy;
import io.nlopez.smartlocation.location.config.LocationParams;

public class DestinationActivity extends AppCompatActivity implements OnMapReadyCallback, TaskLoadedCallback, TaskLoaded2Callback {

    //new changes
    @Override
    public void onMapReady(GoogleMap googleMap) {
        //     Toast.makeText(this, "Map is Ready", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onMapReady: map is ready");
        mMap = googleMap;
        SharedPreferences settings = getSharedPreferences("home_settings", 0);
        boolean darkModeUi_value = settings.getBoolean("ui_settings", false);
        if (darkModeUi_value) {
//            RelativeLayout rel=(RelativeLayout)findViewById(R.id.relLayout1);
//            rel.setBackgroundResource(R.drawable.custom_border3);
            ImageView img =(ImageView)findViewById(R.id.ic_gps2);
            img.setImageResource(R.drawable.ic_gps_teal);
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_in_night));
        }
        if (mLocationPermissionsGranted) {
            getDeviceLocation();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            init();
            mMap.addMarker(place1);
            //mMap.addMarker(place2);

        }

    }

    //new changes
    private ImageView mGps, direction;

    private MarkerOptions place1, place2;
    private Polyline currentPolyline;
    private ArrayList<String> stepsInformation = new ArrayList<String>();


    private static final String TAG = "DestinationActivity";

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;
    //vars
    private Boolean mLocationPermissionsGranted = false;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;

    LatLng fromPosition;
    LatLng toPosition;

//    private TextToSpeech tts;

    Button btn1, btn2;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_destination);
        mGps = (ImageView) findViewById(R.id.ic_gps2);

        //  direction = (ImageView) findViewById(R.id.ic_direction2);
        btn1 = (Button) findViewById(R.id.btn);
        btn2 = (Button) findViewById(R.id.btn2);

        SharedPreferences settings = getSharedPreferences("home_settings", 0);
        boolean darkModeUi_value = settings.getBoolean("ui_settings", false);
        if (!darkModeUi_value) {
            btn1.setTextColor(getResources().getColor(R.color.light_grey));
            btn2.setTextColor(getResources().getColor(R.color.light_grey));
        }
        if(!checkInternetAccess()){
            Toast.makeText(this, "You need a working internet access to enjoy feature.", Toast.LENGTH_SHORT).show();
        }

        Bundle bundle = getIntent().getParcelableExtra("bundle");
        fromPosition = bundle.getParcelable("from_position");
        getLocationPermission();
    }

    private void init() {
        Log.d(TAG, "init: initializing");


        Places.initialize(this, "AIzaSyBDMnkecPu3LCgXZuPsLdkPsNtGbf1cr4U");
        // Create a new Places client instance.
        PlacesClient placesClient = Places.createClient(this);

        // Initialize the AutocompleteSupportFragment.
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(Arrays.asList(
                Place.Field.NAME,
                Place.Field.LAT_LNG
        ));

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                String name = place.getName();
                double lat, lng;
                lat = 0;
                lng = 0;
                if (place.getLatLng() != null) {
                    lat = place.getLatLng().latitude;
                    lng = place.getLatLng().longitude;
                }
                toPosition = new LatLng(place.getLatLng().latitude, place.getLatLng().longitude);
                place2 = new MarkerOptions().position(toPosition).title("Location 2");
                //do something
                moveCamera(new LatLng(lat, lng), DEFAULT_ZOOM,
                        "Destination");
                btn1.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i(TAG, "An error occurred: " + status);

            }
        });


        mGps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: clicked gps icon");
                getDeviceLocation();
            }
        });

//        direction.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Log.d(TAG, "onClick: clicked destination icon");
//                mMap.addMarker(place2);
//                new FetchURL(DestinationActivity.this).execute(getUrl(place1.getPosition(), place2.getPosition(),
//                        "driving"), "driving");
//            }
//        });
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: Destination confirmed.");
                mMap.addMarker(place2);
                new FetchURL(DestinationActivity.this).execute(getUrl(place1.getPosition(), place2.getPosition(),
                        "driving"), "driving");
                btn1.setVisibility(View.GONE);

            }
        });
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: Start Directing thread started!");
//                Directions direct = new Directions(getApplicationContext());
//                direct.execute(stepsInformation, tts);
                Toast.makeText(getApplicationContext(), "Directions started!", Toast.LENGTH_SHORT).show();

                // intent to image-processor
                Intent intent = new Intent(getApplicationContext(), LanePointsActivity.class);
                intent.putExtra(SharedValues.intent_to_assistant_mode,true);
                intent.putExtra(SharedValues.intent_to_nav_mode,true);
                intent.putExtra(SharedValues.intent_dest_latitude,place2.getPosition().latitude);
                intent.putExtra(SharedValues.intent_dest_latitude,place2.getPosition().longitude);
                intent.putStringArrayListExtra(SharedValues.intent_step_info,stepsInformation);
                startActivity(intent);
                finishAffinity();
            }
        });

        place1 = new MarkerOptions().position(fromPosition).title("Location 1");


    }

    private String getUrl(LatLng origin, LatLng dest, String directionMode) {
        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        //"origin=33.5678365, 73.1097087
        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        //destination=33.5938879, 73.0875839"

        // Mode
        String mode = "mode=" + directionMode;
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + mode;

        //"origin=33.5678365, 73.1097087 & destination=33.5938879, 73.0875839 & mode=driving"

        // Output format
        String output = "json";
        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key=AIzaSyBDMnkecPu3LCgXZuPsLdkPsNtGbf1cr4U";
//https://maps.googleapis.com/maps/api/directions/json?origin=33.5678365,73.1097087&destination=33.5938879,73.0875839&mode=driving&key=AIzaSyBDMnkecPu3LCgXZuPsLdkPsNtGbf1cr4U
        return url;
//
    }

    @Override
    public void onTaskDone(Object... values) {
        if (currentPolyline != null)
            currentPolyline.remove();
        currentPolyline = mMap.addPolyline((PolylineOptions) values[0]);
    }

    @Override
    public void onTaskDone2(Object... values) {
        stepsInformation = (ArrayList<String>) values[0];
//        Directions direct = new Directions(getApplicationContext());
//        speak("Hello there!");
        btn2.setVisibility(View.VISIBLE);
        // direct.execute(stepsInformation, tts);
    }


    private void getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: getting the devices current location");

//        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            if (mLocationPermissionsGranted) {

//                final Task location = mFusedLocationProviderClient.getLastLocation();
//                location.addOnCompleteListener(new OnCompleteListener() {
//                    @Override
//                    public void onComplete(@NonNull Task task) {
//                        if(task.isSuccessful()){
//                            Log.d(TAG, "onComplete: found location!");
//                            Location currentLocation = (Location) task.getResult();
//                            toPosition = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
//                            place2=new MarkerOptions().position(toPosition).title("Location 2");
//
//                            moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()),
//                                    DEFAULT_ZOOM,"My Location");
//
//                        }else{
//                            Log.d(TAG, "onComplete: current location is null");
//                            Toast.makeText(DestinationActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
//                        }
//                    }
//                });

                SmartLocation smartLocation = null;
                LocationParams.Builder builder;
                smartLocation = new SmartLocation.Builder(this).logging(true).build();
                builder = new LocationParams.Builder()
                        .setAccuracy(LocationAccuracy.HIGH)
                        .setDistance(0)
                        .setInterval(5000);
                try {
                    smartLocation.with(this)
                            .location()
                            .config(LocationParams.BEST_EFFORT)
                            .continuous()
                            .config(builder.build())
                            .start(new OnLocationUpdatedListener() {
                                @Override
                                public void onLocationUpdated(Location location) {
//                                    double lon = location.getLongitude();
//                                    double lat = location.getLatitude();
                                    fromPosition = new LatLng(location.getLatitude(), location.getLongitude());
                                    moveCamera(new LatLng(location.getLatitude(), location.getLongitude()),
                                            DEFAULT_ZOOM, "My Location");
                                }
                            });
                } catch (SecurityException se) {
                    se.printStackTrace();
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage());
        }
    }

    private void moveCamera(LatLng latLng, float zoom, String title) {
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        if (!title.equals("My Location")) {
            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .title(title);
            mMap.addMarker(options);
        }
        hideSoftKeyboard();
    }


    private void initMap() {
        Log.d(TAG, "initMap: initializing map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);


    }


    private void getLocationPermission() {
        Log.d(TAG, "getLocationPermission: getting location permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};
        //       Toast.makeText(DestinationActivity.this, "check1", Toast.LENGTH_SHORT).show();
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionsGranted = true;
                //         Toast.makeText(this, "check2", Toast.LENGTH_SHORT).show();
                initMap();
            } else {
                ActivityCompat.requestPermissions(this,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
                //              Toast.makeText(this, "a", Toast.LENGTH_SHORT).show();
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
            //           Toast.makeText(this, "b", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: called.");
        mLocationPermissionsGranted = false;

        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            mLocationPermissionsGranted = false;
                            Log.d(TAG, "onRequestPermissionsResult: permission failed");
                            return;
                        }
                    }
                    Log.d(TAG, "onRequestPermissionsResult: permission granted");
                    mLocationPermissionsGranted = true;
                    //initialize our map
                    initMap();
                }
            }
        }
    }

    private void hideSoftKeyboard() {
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

//    private void initializeTextToSpeech() {
//
//        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
//            @Override
//            public void onInit(int status) {
//                if (tts.getEngines().size() == 0) {
//                    Toast.makeText(DestinationActivity.this, "No TTS engine on your device", Toast.LENGTH_LONG).show();
//                    Log.d("check", "No TTS engine on your device");
//                } else {
//                    tts.setLanguage(Locale.getDefault());
//                    tts.setLanguage(Locale.US);
//                    //   Toast.makeText(getApplicationContext(),"check"+Locale.getDefault(),Toast.LENGTH_SHORT);
//                    Log.d("check", "language: " + Locale.getDefault());
//
////                    speak("Oye bhai kesa hai");
//                }
//            }
//        });
//    }

//    public void speak(String message) {
//        if (Build.VERSION.SDK_INT >= 21) {
//            tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
//        } else {
//            tts.speak(message, TextToSpeech.QUEUE_FLUSH, null);
//        }
//    }

        @Override
        protected void onPause() {
            super.onPause();
//            tts.shutdown();
        }

        @Override
        protected void onResume() {
            super.onResume();
    //        Reinitialize the tts engines upon resuming from background such as after opening the browser
//            initializeTextToSpeech();
        }

    public Boolean checkInternetAccess(){
        boolean connected = false;
        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
            //we are connected to a network
            connected = true;
        }
        else
            connected = false;

        return connected;
    }


}
