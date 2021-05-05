package com.natifick.geonotes;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.natifick.geonotes.database.Address;
import com.natifick.geonotes.database.DataBase;

public class MainActivity extends AppCompatActivity {


    // Код запроса, когда спрашиваем разрешение для локации
    public static final int PERMISSIONS_ACCESS_BACKGROUND_LOCATION = 1;
    boolean locationPermissionGranted = false;

    // Для уведомлений нужно создавать канал
    public static final String CHANNEL_ID = "GeoNotes";

    // Обращаемся к базе данных
    DataBase db;

    // Массив адресов
    Address[] addresses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = new DataBase(null, null, null, 1);
        addresses = (Address[])db.getSetOfAddresses().toArray();

        // Держатель для всех этих кнопок
        LinearLayout buttonContainer = findViewById(R.id.ButtonKeeper);
        // Создадим кучу кнопок у которых id - индекс в массиве адресов
        Button butt; // Чтобы штамповать кнопки
        for (int i = 0; i < addresses.length; i++){
            butt = new Button(this);

            // Даём id и текст состоящий из адреса
            butt.setId(i);
            butt.setText(addresses[i].getAddress());
            butt.setBackground(ContextCompat.getDrawable(this, R.drawable.button_shape));
            butt.setOnClickListener(v -> {
                Toast.makeText(this, "А это я ещё не сделал", Toast.LENGTH_SHORT).show();
            });

            buttonContainer.addView(butt);
        }

        butt = new Button(this);
        butt.setId(addresses.length);
        butt.setBackground(ContextCompat.getDrawable(this, R.drawable.button_shape));
        butt.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreatePlaceActivity.class);
            startActivity(intent);
        });

        createNotificationChannel();

        setContentView(R.layout.activity_main);

        getLocationPermission();
    }

    /**
     * Запрашиваем разрешение на геолокацию, если нам его ещё не дали
     */
    private void getLocationPermission(){
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                Toast.makeText(this, "Нам нужно ваше местоположение, чтобы присылать заметки. " +
                        "Пожалуйста, разрешите делать это в фоне", Toast.LENGTH_LONG).show();
            }
        }

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
            Toast.makeText(this, "It is somehow granted", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                    PERMISSIONS_ACCESS_BACKGROUND_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults){
        // Если разрешение было отменено, массив будет пустым
        if (requestCode == PERMISSIONS_ACCESS_BACKGROUND_LOCATION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
            }
        }
    }

    /**
     * Создаём канал уведомлений
     */
    private void createNotificationChannel() {
        CharSequence name = getString(R.string.channel_name);
        String description = getString(R.string.channel_description);
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);
        // Регистрируем канал в системе; Нельзя поменять важность
        // или что-то другое из настроек уведомлений после этого
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }



}



