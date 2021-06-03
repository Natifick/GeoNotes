package com.natifick.geonotes;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.google.android.gms.maps.model.LatLng;


public class CreatePlaceActivity extends AppCompatActivity {

    public static final String MARKER = "marker";


    // Чтобы расшифровывать позицию пользователя
    Geocoder geocoder = new Geocoder(this);

    LatLng coords;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_create_place);
    }

    /**
     * Вызываем активность карты
     *
     * @param view - кнопка "Выбрать место"
     */
    public void MakeNewPlace(View view) {

        Intent intent = new Intent(this, MapsActivity.class);
        startActivityForResult(intent, 1);

    }

    /**
     * Выходим из активности, возвращая выбранный адрес
     *
     * @param view - кнопка "Готово"
     */
    public void returnAddress(View view) {
        Intent data = new Intent();
        // Кладём координаты и имя, всё, что дал нам пользователь
        data.putExtra("marker", coords);
        data.putExtra("name", ((EditText) findViewById(R.id.LocationName)).getText().toString());
        setResult(RESULT_OK, data);
        finish();
    }

    /**
     * Забираем с активности взятия адреса
     *
     * @param requestCode - код запроса, содержит что именно мы хотели узнать
     * @param resultCode  - код результата, проверяем успешность активности
     * @param data        - все данные, которые нам вернула активность
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) return;

        if (resultCode == RESULT_OK) {
            // Запоминаем маркер пользователя
            coords = data.getParcelableExtra(MARKER);
        }
    }
}