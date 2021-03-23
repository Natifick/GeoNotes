package com.natifick.geonotes;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    // remember the user's Marker
    LatLng MarkerPoint;
    private static final String MARKER = "marker";

    // to decode geolocation of Marker
    Geocoder geocoder=new Geocoder(this);

    // for logs
    private static final String TAG = MapsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void MakeNewPlace(View view) {
        Intent intent = new Intent(this, MapsActivity.class);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) return;
        if (resultCode == RESULT_OK) {
            MarkerPoint = data.getParcelableExtra(MARKER);
            try{
                ((TextView) findViewById(R.id.UsersPosition)).setText(
                        geocoder.getFromLocation(MarkerPoint.latitude,
                                MarkerPoint.longitude, 1).get(0).toString());
            }
            catch (IOException ex){
                Log.e("geocoder", ex.getMessage());
            }
        }
    }
}