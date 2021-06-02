package com.natifick.geonotes;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import static java.lang.Integer.parseInt;

public class CreateNoteActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_note);
    }

    /**
     * Вернуть полученную заметку
     *
     * @param view - кнопка "Готово"
     */
    public void returnNote(View view) {
        Intent data = new Intent();
        // Кладём имя, текст и радиус, всё, что дал нам пользователь
        String label = ((EditText) findViewById(R.id.NoteName)).getText().toString();
        String text = ((EditText) findViewById(R.id.Note)).getText().toString();
        int radius = parseInt(((EditText) findViewById(R.id.Radius)).getText().toString());

        data.putExtra("label", label);
        data.putExtra("text", text);
        data.putExtra("radius", radius);
        // to extend our app
        data.putExtra("expire", Long.MAX_VALUE);
        setResult(RESULT_OK, data);
        finish();
    }
}