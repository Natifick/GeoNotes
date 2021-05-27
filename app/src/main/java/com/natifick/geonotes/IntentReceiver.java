package com.natifick.geonotes;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import static com.natifick.geonotes.CreatePlaceActivity.KEY_MESSAGE;
import static com.natifick.geonotes.CreatePlaceActivity.KEY_TITLE;
import static com.natifick.geonotes.MainActivity.CHANNEL_ID;
import static java.lang.Math.random;

/**
 * To receive intents and show messages to the user
 */
public class IntentReceiver extends BroadcastReceiver {
    public static final String TAG = "IntentReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING, false)){
            String message = intent.getStringExtra(KEY_MESSAGE);
            String title = intent.getStringExtra(KEY_TITLE);
            Log.d(TAG, message);
            sendNotification(context, title, message);
            // теперь нужно удалить alert, иначе он будет каждый раз присылаться
            LocationManager locationManager = (LocationManager) context.
                    getSystemService(Context.LOCATION_SERVICE);
            PendingIntent proximityIntent = PendingIntent.getBroadcast(context, -1,
                    intent, PendingIntent.FLAG_CANCEL_CURRENT);
            locationManager.removeProximityAlert(proximityIntent);

        }
    }

    void sendNotification(Context context, String title, String message){
        // When user taps on notification he will get to our activity
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true); // setAutoCancel - removes notif when user taps on it

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify((int)Math.round(random()*100), builder.build());

    }


}
