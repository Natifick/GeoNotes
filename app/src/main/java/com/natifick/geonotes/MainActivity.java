package com.natifick.geonotes;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.natifick.geonotes.database.Address;
import com.natifick.geonotes.database.DataBase;

import java.util.Set;

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

    // Просим пользователя дать разрешение
    AlertDialog alert = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DataBase(null, null, null, 1);
        Set <Address> temp = db.getSetOfAddresses();

        Button butt; // Чтобы штамповать кнопки
        // Держатель для всех этих кнопок
        LinearLayout buttonContainer = findViewById(R.id.ButtonKeeper);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 0, 0, 20);
        // Если у нас ещё нет адресов, то нам нечего создавать
        if (temp.size() != 0){
            addresses = new Address[temp.size()];
            temp.toArray(addresses);
            // Создадим кучу кнопок у которых id - индекс в массиве адресов
            for (int i = 0; i < addresses.length; i++){
                butt = new Button(this);
                // Даём id и текст состоящий из адреса
                butt.setId(i);
                butt.setText(addresses[i].getAddress());
                butt.setSingleLine(false); // перенос текста
                butt.setBackground(ContextCompat.getDrawable(this, R.drawable.button_shape));
                butt.setOnClickListener(v -> {
                    Toast.makeText(this, "А это я ещё не сделал", Toast.LENGTH_SHORT).show();
                });
                buttonContainer.addView(butt, layoutParams);
            }
        }

        // И кнопка добавления нового адреса, она чуть ниже всех остальных
        layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 20, 0, 0);

        // Её остальные параметры почти такие же
        butt = new Button(this);
        butt.setId(temp.size() == 0 ? 0 : addresses.length);
        butt.setBackground(ContextCompat.getDrawable(this, R.drawable.button_shape));
        butt.setText("Создать новый адрес");
        butt.setSingleLine(false);
        butt.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreatePlaceActivity.class);
            startActivity(intent);
        });
        buttonContainer.addView(butt, layoutParams);

        // Создаём канал уведомлений
        createNotificationChannel();
    }

    /**
     * Запрашиваем разрешение на местоположение.
     * Пользователю предлагается перейти в настройки, или уйти из приложения
     */
    private void request_permission(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.app_name);
        builder.setMessage(R.string.permission_request);
        builder.setCancelable(false);

        // Если разрешение получено - продолжаем
        builder.setPositiveButton("перейти в настройки", (dialog, which) -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", getPackageName(), null));
            startActivity(intent);
        });
        // Если не хочет - закрываем приложение
        builder.setNegativeButton("Закрыть приложение", (dialog, which) -> this.finish());

        if (alert != null){
            alert.cancel();
        }
        alert = builder.create();
        // Проверка того, что активность ещё не закрыта
        if (!(this).isFinishing()) {
            alert.show();
        }
    }

    /**
     * При фокусировке на активности
     * Если у нас нет разрешения на геолокацию, воспользуемся случаем попросить
     */
    @Override
    protected void onResume() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            request_permission();
        }
        else{
            if (alert != null){
                alert.cancel();
                alert = null;
            }
        }
        super.onResume();
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



