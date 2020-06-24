package com.example.fyp;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;


import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.location.config.LocationAccuracy;
import io.nlopez.smartlocation.location.config.LocationParams;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {


    private boolean gpscheck = false;

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "Map is Ready", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onMapReady: map is ready");
        mMap = googleMap;
        // statusCheck();
        if (mLocationPermissionsGranted) {
            LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//            if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            //       Toast.makeText(this, "gps check " + gpscheck, Toast.LENGTH_SHORT).show();
            //statusCheck();
//                LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                // Toast.makeText(this, "Enable gps", Toast.LENGTH_SHORT).show();
                buildAlertMessageNoGps();

            } else {
                getDeviceLocation();
            }


            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            init();
            // }
        }

    }

    LatLng fromPosition;
    private ImageView mGps;
    private ImageView direction;

    private static final String TAG = "MapActivity";

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;
    //vars
    private Boolean mLocationPermissionsGranted = false;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;

    Button btn1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        mGps = (ImageView) findViewById(R.id.ic_gps);

 //       direction = (ImageView) findViewById(R.id.ic_direction);
        getLocationPermission();
        btn1 = (Button) findViewById(R.id.btn);


    }


    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }


    private void init() {
        Log.d(TAG, "init: initializing");


        Places.initialize(this, "AIzaSyBDMnkecPu3LCgXZuPsLdkPsNtGbf1cr4U");
        // Create a new Places client instance.
        PlacesClient placesClient = Places.createClient(this);

        // Initialize the AutocompleteSupportFragment.
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        // Specify the types of place data to return.eq3E
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
                fromPosition = new LatLng(place.getLatLng().latitude, place.getLatLng().longitude);
                //do something
                moveCamera(new LatLng(lat, lng), DEFAULT_ZOOM,
                        "Start Location");
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
//                Intent intent = new Intent(MapsActivity.this, DestinationActivity.class);
//                //intent.putExtra("sum", sum + "");
//                Bundle args = new Bundle();
//                args.putParcelable("from_position", fromPosition);
//                intent.putExtra("bundle", args);
//
//                startActivity(intent);
//                Toast.makeText(MapsActivity.this, "Direction clicked", Toast.LENGTH_SHORT).show();
//            }
//        });

        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: Start Location confirmed!");
                Intent intent = new Intent(MapsActivity.this, DestinationActivity.class);
                //intent.putExtra("sum", sum + "");
                Bundle args = new Bundle();
                args.putParcelable("from_position", fromPosition);
                intent.putExtra("bundle", args);

                startActivity(intent);
                Toast.makeText(MapsActivity.this, "Start Location Confirmed!", Toast.LENGTH_SHORT).show();
            }
        });


    }


    private void getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: getting the devices current location");

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            if (mLocationPermissionsGranted) {


//                final Task location = mFusedLocationProviderClient.getLastLocation();
//                location.addOnCompleteListener(new OnCompleteListener() {
//                    @Override
//                    public void onComplete(@NonNull Task task) {
//                        if (task.isSuccessful()) {
//                            Log.d(TAG, "onComplete: found location!");
//                            Location currentLocation = (Location) task.getResult();
//                            if (currentLocation == null) {
//                                Toast.makeText(getApplicationContext(), "unable to get current location. wait for a few minutes and restart the app", Toast.LENGTH_SHORT).show();
//
//                            } else {
//                                fromPosition = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
//
//                                moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()),
//                                        DEFAULT_ZOOM, "My Location");
//                            }
//
//                        } else {
//                            Log.d(TAG, "onComplete: current location is null");
//                            Toast.makeText(MapsActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
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
        //   Toast.makeText(this, "check1", Toast.LENGTH_SHORT).show();
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionsGranted = true;
                //        Toast.makeText(this, "check2", Toast.LENGTH_SHORT).show();
                initMap();
            } else {
                ActivityCompat.requestPermissions(this,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
                //           Toast.makeText(this, "a", Toast.LENGTH_SHORT).show();
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
            //      Toast.makeText(this, "b", Toast.LENGTH_SHORT).show();
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


}

