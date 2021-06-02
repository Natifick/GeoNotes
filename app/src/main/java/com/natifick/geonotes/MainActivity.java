package com.natifick.geonotes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
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
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.google.android.gms.maps.model.LatLng;
import com.natifick.geonotes.database.Address;
import com.natifick.geonotes.database.DataBase;

import java.util.Set;

public class MainActivity extends AppCompatActivity {

    // Чтобы решать, удаляем мы сейчас кнопки, или нет
    boolean pos_delete = false;

    // для intent'ов
    public static final int requestCode_makeAddress = 1;

    // Код запроса, когда спрашиваем разрешение для локации
    public static final int PERMISSIONS_ACCESS_BACKGROUND_LOCATION = 1;
    boolean locationPermissionGranted = false;

    // Для уведомлений нужно создавать канал
    public static final String CHANNEL_ID = "GeoNotes";

    // Обращаемся к базе данных
    public static DataBase db;

    // Массив адресов
    Address[] addresses = null;

    // Просим пользователя дать разрешение
    AlertDialog alert = null;

    // Куда складывать кнопки
    LinearLayout buttonContainer = null;

    // Чтобы штамповать кнопки будем хранить 2 разных margin'а
    LinearLayout.LayoutParams small_margin = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

    LinearLayout.LayoutParams big_margin = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        small_margin.setMargins(0, 0, 0, 20);
        big_margin.setMargins(0, 20, 0, 0);

        // Получим список всех адресов
        db = new DataBase(getApplicationContext(), "database", null, 1);
        // Держатель для всех кнопок
        buttonContainer = findViewById(R.id.ButtonKeeper);
        // Заносим все кнопки на экран
        addAllButtons();

        // Создаём канал уведомлений
        createNotificationChannel();
    }

    private void addAllButtons(){
        Set <Address> temp = db.getSetOfAddresses();
        buttonContainer.removeAllViews();
        Button butt; // Чтобы штамповать кнопки
        // Если у нас ещё нет адресов, то нам нечего создавать
        if (temp.size() != 0){
            addresses = new Address[temp.size()];
            temp.toArray(addresses);
            // Создадим кучу кнопок у которых id - индекс в массиве адресов
            for (int i = 0; i < addresses.length; i++){
                butt = new Button(this);
                // Даём id и текст состоящий из адреса
                // Этот id потом будет использоваться (на 7 строчек ниже)
                butt.setId(i);
                butt.setText(addresses[i].getAddress());
                butt.setSingleLine(false); // перенос текста
                butt.setBackground(ContextCompat.getDrawable(this, R.drawable.button_shape));
                butt.setOnClickListener(pos_delete?this::drop:this::openOld);
                buttonContainer.addView(butt, small_margin);
            }
        }

        // И кнопка добавления нового адреса, она чуть ниже всех остальных
        // Её остальные параметры почти такие же
        butt = new Button(this);
        butt.setId(temp.size()==0 ? 0 : addresses.length);
        butt.setBackground(ContextCompat.getDrawable(this, R.drawable.button_shape));
        butt.setText("Создать новый адрес");
        butt.setSingleLine(false);
        butt.setOnClickListener(this::createNew);
        buttonContainer.addView(butt, big_margin);
        // Добавляем кнопку удаления только в том случае, если есть что удалять
        if (temp.size()!=0){
            // И кнопка-переключатель
            butt = new Button(this);
            butt.setId(addresses.length+1);
            butt.setBackground(ContextCompat.getDrawable(this, R.drawable.button_shape));
            butt.setText(pos_delete?"Удалить выбранные адреса":"Перейти в режим удаления адресов");
            butt.setSingleLine(false);
            butt.setOnClickListener(this::switcher);
            buttonContainer.addView(butt, big_margin);
        }
    }

    /**
     * Переключает кнопки с режима "работает" в режим "удалить" и наоборот
     * @param v - сама кнопка, которую и подвязываем
     */
    public void switcher(View v){
        pos_delete = !pos_delete;
        Button butt;
        if (pos_delete){
            ((Button)v).setText("Удалить выбранные адреса");
            for (int i=0;i<addresses.length;i++){
                butt = findViewById(i);
                butt.setOnClickListener(this::drop);
            }
        }
        else{
            ((Button)v).setText("Перейти в режим удаления адресов");
            for (int i=0;i<addresses.length;i++){
                butt = findViewById(i);
                // Если пользователь переключил его в режим удаления - удаляем
                if (butt.getAlpha() == 0.7f){
                    db.deleteAddress(addresses[i].getAddress());
                }
                butt.setOnClickListener(this::openOld);
            }
            addAllButtons();
        }
    }

    public void createNew(View v){
        pos_delete = false;
        Intent intent = new Intent(this, CreatePlaceActivity.class);
        startActivityForResult(intent, requestCode_makeAddress);
    }

    public void openOld(View v){
        Intent intent = new Intent(this, ViewNotesActivity.class);
        intent.putExtra("addrX", addresses[v.getId()].getX());
        intent.putExtra("addrY", addresses[v.getId()].getY());
        startActivity(intent);
    }

    /**
     * на самом деле эта кнопка лишь помечает её как ту, что нужно удалить
     * @param v - подлежащая удалению кнопка
     */
    public void drop(View v){
        if (v.getAlpha() == 0.7f){
            v.setAlpha(1);
            v.setBackgroundColor(getResources().getColor(R.color.teal_200, null));
        }
        else{
            v.setAlpha(0.7f);
            v.setBackgroundColor(getResources().getColor(R.color.purple_mine, null));
        }
    }

    /**
     * Сразу после завершения вызванной активности создания адреса
     * @param requestCode - код, чтобы различать активности
     * @param resultCode - код результата
     * @param data - вся информация
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == requestCode_makeAddress && resultCode == RESULT_OK){
            // Нам передали "name" и "marker"
            LatLng coords = data.getParcelableExtra("marker");
            String name = data.getStringExtra("name");

            // Создаём на их основе новый адрес
            Address address = new Address(coords.latitude, coords.longitude, name);
            db.addNewAddress(address);
            // Теперь у нас гарантированно есть адрес, можем не проверять на null
            Set <Address> temp = db.getSetOfAddresses();
            addresses = new Address[temp.size()];
            temp.toArray(addresses);
            addAllButtons();
        }
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
        builder.setPositiveButton("Перейти в настройки", (dialog, which) -> {
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

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }
}



