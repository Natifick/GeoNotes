package com.natifick.geonotes;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;


public class MainActivity extends AppCompatActivity {

    public static final String MARKER = "marker";

    // for intents
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_TITLE = "title";

    // to decode geolocation of Marker
    Geocoder geocoder = new Geocoder(this);

    // for logs
    private static final String TAG = MainActivity.class.getSimpleName();

    // notification channel
    public static final String CHANNEL_ID = "GeoNotes";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createNotificationChannel();

        setContentView(R.layout.activity_main);
    }

    /**
     * Creates the notification channel for our application
     */
    private void createNotificationChannel() {
        CharSequence name = getString(R.string.channel_name);
        String description = getString(R.string.channel_description);
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    /**
     * call map activity
     * @param view - the only button here
     */
    public void MakeNewPlace(View view) {

        Intent intent = new Intent(this, MapsActivity.class);
        startActivityForResult(intent, 1);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) return;

        if (resultCode == RESULT_OK) {
            // remember the user's Marker
            LatLng MarkerPoint = data.getParcelableExtra(MARKER);
            try {
                ((TextView) findViewById(R.id.UsersPosition)).setText(
                        geocoder.getFromLocation(MarkerPoint.latitude,
                                MarkerPoint.longitude, 1).get(0).toString());
                Log.e(TAG, "Begin adding proximity alert");
                setProximityAlert(MarkerPoint, 1000, -1, "title", "You have arrived!");
                Log.e(TAG, "End adding proximity alert");
            } catch (IOException ex) {
                Log.e(TAG, ex.getMessage());
            }
        }
    }

    /**
     * Устанавливаем новое уведомление на местоположение
     *
     * @param point - точка заданная в формате LatLng (latitude, longitude)
     * @param radius - радиус в метрах
     * @param duration - длительность в миллисекундах
     */
    private void setProximityAlert(LatLng point, float radius, long duration, String title, String message){
        Context context = getApplicationContext();
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        Intent intent = new Intent(context, IntentReceiver.class);
        // It will be easier to find intents after we set their actions as an identifier
        intent.setAction(point.latitude+" "+point.longitude);

        intent.putExtra(KEY_MESSAGE, message);
        intent.putExtra(KEY_TITLE, title);

        //flagging intent
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        //flagging pendingIntent
        PendingIntent proximityIntent = PendingIntent.getBroadcast(context, -1,
                intent, PendingIntent.FLAG_CANCEL_CURRENT);

        // Checking if user has the right permission again
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.addProximityAlert(point.latitude, point.longitude, radius, duration,
                    proximityIntent);//setting proximity alert
        }
        else {
            Toast.makeText(this, "Can't set marker", Toast.LENGTH_LONG).show();
        }
    }

}