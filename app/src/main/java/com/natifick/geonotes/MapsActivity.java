package com.natifick.geonotes;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;

import static com.natifick.geonotes.MainActivity.MARKER;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback{
    // the map itself
    private GoogleMap map;

    // client for map and location
    FusedLocationProviderClient fusedLocationClient;

    // to find users current location
    private static final int DEFAULT_ZOOM = 15;
    private static final String KEY_CAMERA_POSITION = "camera_position";

    // default location is, of course, our favorite university
    private final LatLng defaultLocation = new LatLng(55.669949, 37.481132);

    // remember the user's Marker
    LatLng MarkerPoint = defaultLocation;

    // request code when asking permission
    public static final int PERMISSIONS_ACCESS_FINE_LOCATION = 1;
    boolean locationPermissionGranted = false;

    // for logs
    private static final String TAG = MapsActivity.class.getSimpleName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        if (savedInstanceState != null){
            MarkerPoint = savedInstanceState.getParcelable(MARKER);
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        // location client to work with current location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        getLocationPermission();

        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        if (map != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, map.getCameraPosition());
            outState.putParcelable(MARKER, MarkerPoint);
        }
        super.onSaveInstanceState(outState);
    }

    /**
     * Возвращает выбранную геолокацию
     * @param view - наша кнопка для завершения активности
     */
    public void returnMarker(View view){
        Intent intent = new Intent();
        intent.putExtra(MARKER, MarkerPoint);
        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * get location permission if not granted already
     */
    private void getLocationPermission(){
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults){
        // If request is cancelled, the result arrays are empty.
        if (requestCode == PERMISSIONS_ACCESS_FINE_LOCATION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
                getDeviceLocation();
            }
        }
        updateLocationUI();
    }

    /**
     * updates user interface on the map
     */
    private void updateLocationUI(){
        if (map == null){
            return;
        }
        try{
            if (locationPermissionGranted){
                map.setMyLocationEnabled(true);
                map.getUiSettings().setMyLocationButtonEnabled(true);

            }
            else{
                map.setMyLocationEnabled(false);
                map.getUiSettings().setMyLocationButtonEnabled(false);
                getLocationPermission();
            }
        }
        catch (SecurityException exc){
            Log.e("Exception: %s", exc.getMessage());
        }
    }

    /**
     * when we find users current location, we set the camera to it and put marker
     */
    private void getDeviceLocation(){
        try {
            if (locationPermissionGranted) {
                // task is a class for asynchronous operations
                // we put there our demand
                Task<Location> locationResult = fusedLocationClient.getLastLocation();
                // lambda, 'task' is the name of the function (or an interface, it's lambda)
                locationResult.addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Set the map's camera position to the current location of the device
                        Location lastKnownLocation = task.getResult();
                        if (lastKnownLocation != null) {
                            LatLng temp = new LatLng(lastKnownLocation.getLatitude(),
                                    lastKnownLocation.getLongitude());
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(temp, DEFAULT_ZOOM));
                            // Clear all markers and put one to current position
                            map.clear();
                            map.addMarker(new MarkerOptions().position(temp));
                            MarkerPoint = new LatLng(lastKnownLocation.getLatitude(),
                                    lastKnownLocation.getLongitude());
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            map.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
                            map.getUiSettings().setMyLocationButtonEnabled(false);
                            // clear all markers and put one to default
                            map.clear();
                            map.addMarker(new MarkerOptions().position(defaultLocation));
                        }
                    }
                });
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage(), e);
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        // Every time user taps on the screen he will put the marker
        map.setOnMapClickListener(point -> {
            MarkerPoint = point;
            map.clear();
            map.addMarker(new MarkerOptions().position(point));
            Toast.makeText(getApplicationContext(), MarkerPoint.toString(), Toast.LENGTH_SHORT).show();
        });
        // turn on the location layer
        updateLocationUI();

        // get device location and show it on the map
        getDeviceLocation();
    }
}