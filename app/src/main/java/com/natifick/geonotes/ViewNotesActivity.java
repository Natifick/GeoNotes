package com.natifick.geonotes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.natifick.geonotes.database.Address;
import com.natifick.geonotes.database.DataBase;
import com.natifick.geonotes.database.Note;

import java.util.Set;

public class ViewNotesActivity extends AppCompatActivity {

    // Чтобы решать, удаляем мы сейчас кнопки, или нет
    boolean pos_delete = false;

    // для intent'ов
    public static final int requestCode_makeNote = 1;

    // Для intent'ов к уведомлениям
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_TITLE = "title";
    public static final String KEY_ADDRESS = "address";

    public static final long MINIMUM_TIME_BETWEEN_UPDATE = 1; // every millisecond
    public static final float MINIMUM_DISTANCECHANGE_FOR_UPDATE = 1; // meter

    // Обращаемся к базе данных
    DataBase db = MainActivity.db;

    // Храним заметки текущего адреса
    Note[] notes = null;

    // Адрес, переданный нам
    Address address = null;

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
        setContentView(R.layout.activity_view_notes);

        small_margin.setMargins(0, 0, 0, 20);
        big_margin.setMargins(0, 20, 0, 0);

        double x = getIntent().getDoubleExtra("addrX", 0);
        double y = getIntent().getDoubleExtra("addrY", 0);
        address = db.getAddressByCoordinates(x, y);
        setTitle(address.getAddress());

        // Куда будем складывать кнопки
        buttonContainer = findViewById(R.id.ButtonKeeper);

        // Выводим все кнопки на экран
        addAllButtons();
    }

    /**
     * Сразу после завершения вызванной активности создания заметки
     * @param requestCode - код, чтобы различать активности
     * @param resultCode - код результата
     * @param data - вся информация
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == requestCode_makeNote && resultCode == RESULT_OK){

            String name = data.getStringExtra("label");
            String text = data.getStringExtra("text");
            int radius = data.getIntExtra("radius", 100);
            long expire = data.getLongExtra("expire", -1);

            setProximityAlert(radius, expire, name, text);

            // Создаём на их основе новую заметку и заносим в базу данных
            // Если она не должна исчезать по времени, то ставим MAXVAL
            Note note = new Note(name, text, address, expire>0?expire:Long.MAX_VALUE);
            db.addNewNote(note, address);
            // Теперь у нас гарантированно есть адрес, можем не проверять на null
            // И можно добавить кнопку
            Set <Note> temp = db.getSetOfNoteByAddress(address.getAddress());
            notes = new Note[temp.size()];
            temp.toArray(notes);
            addAllButtons();
        }
    }

    private void addAllButtons(){
        Set<Note> temp = db.getSetOfNoteByAddress(address.getAddress());
        buttonContainer.removeAllViews();
        Button butt; // Чтобы штамповать кнопки
        // Если у нас ещё нет адресов, то нам нечего создавать
        if (temp.size() != 0){
            notes = new Note[temp.size()];
            temp.toArray(notes);
            // Создадим кучу кнопок у которых id - индекс в массиве адресов
            for (int i = 0; i < notes.length; i++){
                butt = new Button(this);
                // Даём id и текст состоящий из адреса
                // Этот id потом будет использоваться (на 7 строчек ниже)
                butt.setId(i);
                butt.setText(notes[i].getName());
                butt.setSingleLine(false); // перенос текста
                butt.setBackground(ContextCompat.getDrawable(this, R.drawable.button_shape));
                butt.setOnClickListener(pos_delete?this::drop:this::showNote);
                buttonContainer.addView(butt, small_margin);
            }
        }

        // И кнопка добавления нового адреса, она чуть ниже всех остальных
        // Её остальные параметры почти такие же
        butt = new Button(this);
        butt.setId(temp.size()==0 ? 0 : notes.length);
        butt.setBackground(ContextCompat.getDrawable(this, R.drawable.button_shape));
        butt.setText("Создать новую заметку");
        butt.setSingleLine(false);
        butt.setOnClickListener(this::createNew);
        buttonContainer.addView(butt, big_margin);
        // Добавляем кнопку удаления только в том случае, если есть что удалять
        if (temp.size()!=0){
            // И кнопка-переключатель
            butt = new Button(this);
            butt.setId(notes.length+1);
            butt.setBackground(ContextCompat.getDrawable(this, R.drawable.button_shape));
            butt.setText(pos_delete?"Удалить выбранные заметки":"Перейти в режим удаления заметок");
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
            ((Button)v).setText("Удалить выбранные заметки");
            for (int i=0;i<notes.length;i++){
                butt = findViewById(i);
                butt.setOnClickListener(this::drop);
            }
        }
        else{
            ((Button)v).setText("Перейти в режим удаления заметок");
            for (int i=0;i<notes.length;i++){
                butt = findViewById(i);
                // Если пользователь переключил его в режим удаления - удаляем
                if (butt.getAlpha() == 0.7f){
                    db.deleteNote(address.getAddress(), notes[i].getName());
                }
                butt.setOnClickListener(this::showNote);
            }
            addAllButtons();
        }
    }

    public void createNew(View v){
        Intent intent = new Intent(this, CreateNoteActivity.class);
        startActivityForResult(intent, requestCode_makeNote);
    }

    public void showNote(View v){
        // Intent intent = new Intent(this, CreateNoteActivity.class);
        // intent.putExtra("address", notes[v.getId()].getName());
        // startActivity(intent);
        Note note = notes[v.getId()];
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(note.getName());
        builder.setMessage(note.getText());
        AlertDialog alert = builder.create();
        // Проверка того, что активность ещё не закрыта
        if (!(this).isFinishing()) {
            alert.show();
        }
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
     * Устанавливаем новое уведомление на местоположение
     *
     * @param radius - радиус в метрах
     * @param duration - длительность в миллисекундах
     */
    private void setProximityAlert(float radius, long duration,
                                   String title, String message){
        Context context = getApplicationContext();
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        Intent intent = new Intent(context, IntentReceiver.class);
        // Будет проще искать потом для удаления
        intent.setAction(address.getX()+" "+address.getY());

        intent.putExtra(KEY_MESSAGE, message);
        intent.putExtra(KEY_TITLE, title);
        intent.putExtra(KEY_ADDRESS, address.getAddress());

        // Добавляем флаги к Intent'у
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        // Добавляем флаг к PendingIntent
        PendingIntent proximityIntent = PendingIntent.getBroadcast(context, -1,
                intent, PendingIntent.FLAG_CANCEL_CURRENT);

        // Вновь проверим, что у пользователя есть нужное разрешение
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.addProximityAlert(address.getX(), address.getY(), radius, duration,
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