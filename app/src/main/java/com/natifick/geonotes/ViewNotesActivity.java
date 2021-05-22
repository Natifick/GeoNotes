package com.natifick.geonotes;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

import com.natifick.geonotes.database.Address;
import com.natifick.geonotes.database.DataBase;
import com.natifick.geonotes.database.Note;

import java.util.Set;

public class ViewNotesActivity extends AppCompatActivity {

    public static final int requestCode_makeNote = 1;

    // Обращаемся к базе данных
    DataBase db;

    // Храним заметки текущего адреса
    Note[] notes = null;

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

        db = savedInstanceState.getParcelable("database");
        Address address = savedInstanceState.getParcelable("address");

        // Вроде, как-то так получаем список всех заметок адреса
        Set<Note> temp = db.getSetOfNoteByAddress(address.getAddress());


        small_margin.setMargins(0, 0, 0, 20);
        big_margin.setMargins(0, 20, 0, 0);

        Button butt; // Чтобы штамповать кнопки
        // Держатель для всех этих кнопок
        buttonContainer = findViewById(R.id.ButtonKeeper);
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
                butt.setText(notes[i].getText());
                butt.setSingleLine(false); // перенос текста
                butt.setBackground(ContextCompat.getDrawable(this, R.drawable.button_shape));
                butt.setOnClickListener(v -> {
                    Intent intent = new Intent(this, CreateNoteActivity.class);
                    intent.putExtra("address", notes[v.getId()].getText());
                    startActivity(intent);
                });
                buttonContainer.addView(butt, small_margin);
            }
        }

        // И кнопка добавления нового адреса, она чуть ниже всех остальных
        // Её остальные параметры почти такие же
        butt = new Button(this);
        butt.setId(temp.size() == 0 ? 0 : notes.length);
        butt.setBackground(ContextCompat.getDrawable(this, R.drawable.button_shape));
        butt.setText("Создать новый адрес");
        butt.setSingleLine(false);
        butt.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateNoteActivity.class);
            startActivityForResult(intent, requestCode_makeNote);
        });
        buttonContainer.addView(butt, big_margin);

    }
}