package com.natifick.geonotes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;


public class CreatePlaceActivity extends AppCompatActivity {

    public static final String MARKER = "marker";

    // Для intent'ов к уведомлениям
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_TITLE = "title";

    // Чтобы расшифровывать позицию пользователя
    Geocoder geocoder = new Geocoder(this);

    LatLng coords;

    // Для логов
    private static final String TAG = MainActivity.class.getSimpleName();

    public static final long MINIMUM_TIME_BETWEEN_UPDATE = 1; // every millisecond
    public static final float MINIMUM_DISTANCECHANGE_FOR_UPDATE = 1; // meter

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_create_place);
    }

    /**
     * Вызываем активность карты
     * @param view - кнопка "Выбрать место"
     */
    public void MakeNewPlace(View view) {

        Intent intent = new Intent(this, MapsActivity.class);
        startActivityForResult(intent, 1);

    }

    /**
     * Выходим из активности, возвращая выбранный адрес
     * @param view - кнопка
     */
    public void return_address(View view){
        Intent data = new Intent();
        // Кладём координаты и имя, всё, что дал нам пользователь
        data.putExtra("marker", coords);
        data.putExtra("name", ((EditText)findViewById(R.id.LocationName)).getText().toString());
        setResult(RESULT_OK, data);
        finish();
    }

    /**
     * Забираем с активности взятия адреса
     * @param requestCode - код запроса, содержит что именно мы хотели узнать
     * @param resultCode - код результата, проверяем успешность активности
     * @param data - все данные, которые нам вернула активность
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) return;

        if (resultCode == RESULT_OK) {
            // Запоминаем маркер пользователя
            coords = data.getParcelableExtra(MARKER);
            try {
                android.location.Address location = geocoder.getFromLocation(coords.latitude,
                        coords.longitude, 1).get(0);
                // Получаем описание локации
                ((TextView) findViewById(R.id.Location)).setText(location.getAddressLine(0));

                Log.e(TAG, "Добавляем уведомление по приближении");
                setProximityAlert(coords, 300, -1,
                        "title", "You have arrived!");
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
    private void setProximityAlert(LatLng point, float radius, long duration,
                                   String title, String message){
        Context context = getApplicationContext();
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        Intent intent = new Intent(context, IntentReceiver.class);
        // Будет проще искать потом для удаления
        intent.setAction(point.latitude+" "+point.longitude);

        intent.putExtra(KEY_MESSAGE, message);
        intent.putExtra(KEY_TITLE, title);

        // Добавляем флаги к Intent'у
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        // Добавляем флаг к PendingIntent
        PendingIntent proximityIntent = PendingIntent.getBroadcast(context, -1,
                intent, PendingIntent.FLAG_CANCEL_CURRENT);

        // Вновь проверим, что у пользователя есть нужное разрешение
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.addProximityAlert(point.latitude, point.longitude, radius, duration,
                    proximityIntent);// Наконец сама установка уведомления на приближение

            // Также стоит заставить его проверять это самостоятельно
            // https://stackoverflow.com/questions/11979240/does-locationmanager-addproximityalert-require-a-location-listener
            Criteria crit = new Criteria();
            crit.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
            locationManager.requestLocationUpdates(
                    locationManager.getBestProvider(crit, true), MINIMUM_TIME_BETWEEN_UPDATE,
                    MINIMUM_DISTANCECHANGE_FOR_UPDATE, new MyLocListener());
        }
        else {
            Toast.makeText(this, "Невозможно настроить уведомление", Toast.LENGTH_LONG).show();
        }
    }
}

class MyLocListener implements LocationListener {

    @Override
    public void onLocationChanged(@NonNull Location location) {

    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {

    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {

    }
}