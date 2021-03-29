package com.natifick.geonotes;

import android.app.PendingIntent;
import android.content.Intent;
import android.location.LocationManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import static com.natifick.geonotes.MainActivity.CHANNEL_ID;
import static com.natifick.geonotes.MainActivity.KEY_MESSAGE;
import static com.natifick.geonotes.MainActivity.KEY_TITLE;
import static java.lang.Math.random;

/**
 * To receive intents and show messages to the user
 */
public class IntentReciever extends JobIntentService {
    public static final String TAG = "IntentReciever";
    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        if (intent.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING, false)){
            String message = intent.getStringExtra(KEY_MESSAGE);
            String title = intent.getStringExtra(KEY_TITLE);
            Log.d(TAG, message);
            sendNotification(title, message);
        }
    }

    void sendNotification(String title, String message){
        // When user taps on notification he will get to our activity
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.common_full_open_on_phone)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true); // setAutoCancel - removes notif when user taps on it

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify((int)Math.round(random()*100), builder.build());

    }


}
