package com.natifick.geonotes;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import android.content.Intent;
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

import static com.natifick.geonotes.CreatePlaceActivity.MARKER;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback{
    // Сама карта
    private GoogleMap map;

    // Клиент для карты и локации
    FusedLocationProviderClient fusedLocationClient;

    // Настройки положения и зума камеры
    private static final int DEFAULT_ZOOM = 15;
    private static final String KEY_CAMERA_POSITION = "camera_position";

    // Локация по умолчанию, конечно, наш любимый ВУЗ)
    private final LatLng defaultLocation = new LatLng(55.669949, 37.481132);

    // Запоминаем маркер пользователя
    LatLng MarkerPoint = defaultLocation;



    // Для логов
    private static final String TAG = MapsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        if (savedInstanceState != null){
            MarkerPoint = savedInstanceState.getParcelable(MARKER);
        }

        // Запрашиваем SupportMapFragment и ждём, пока карта не будет готова
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        // Клиент локации
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

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
     * Возвращает позицию, выбранную пользователем (Marker)
     * @param view - кнопка "ок"
     */
    public void returnMarker(View view){
        Intent intent = new Intent();
        intent.putExtra(MARKER, MarkerPoint);
        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * Обновляем пользовательский интерфейс, отображаемый поверх карты
     */
    private void updateLocationUI(){
        if (map == null){
            return;
        }
        try{
            map.setMyLocationEnabled(true);
            map.getUiSettings().setMyLocationButtonEnabled(true);
        }
        catch (SecurityException exc){
            Log.e("Exception: %s", exc.getMessage());
        }
    }

    /**
     * Когда находим позицию пользователя, мы перемещаем камеру на него
     */
    private void getDeviceLocation(){
        try {
            // Task - это класс для асинхронных операций
            // В него мы кладём наш запрос
            Task<Location> locationResult = fusedLocationClient.getLastLocation();
            // Лямбда выражение, 'task' это имя функции (или интерфейса, это же лямбда)
            locationResult.addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    // Устанавливаем камеру в позицию пользователя
                    Location lastKnownLocation = task.getResult();
                    if (lastKnownLocation != null) {
                        LatLng temp = new LatLng(lastKnownLocation.getLatitude(),
                                lastKnownLocation.getLongitude());
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(temp, DEFAULT_ZOOM));
                        // Очищаем все предыдущие маркеры и устанавливаем в координату пользователя
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
                        // Очиащем все маркеры и устанавливаем в позицию по умолчанию
                        map.clear();
                        map.addMarker(new MarkerOptions().position(defaultLocation));
                    }
                }
            });
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
     *
     * Меняем карту сразу как она готова
     * Этот callback вызывается, когда карта загружена
     * и готова к отрисовке дополнительных элементов управления.
     * Если сервис Google Play не установлен, то пользователь будет перенаправлен
     * на страницу установки.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        // Каждый раз пользователь кликает куда-то, мы переставляем маркер
        map.setOnMapClickListener(point -> {
            MarkerPoint = point;
            map.clear();
            map.addMarker(new MarkerOptions().position(point));
            Toast.makeText(getApplicationContext(), MarkerPoint.toString(), Toast.LENGTH_SHORT).show();
        });
        // Слой элементов управления
        updateLocationUI();

        // Получаем локацию девайса и показываем на карте
        getDeviceLocation();
    }
}