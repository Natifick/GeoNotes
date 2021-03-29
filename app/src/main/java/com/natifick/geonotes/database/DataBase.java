package com.natifick.geonotes.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DataBase {
    //Объект дает доступ к чтению и записи базы данных
    private final SQLiteOpenHelper helper;
    //Список заметок, чтобы не обращаться к базе данных
    private final List<Note> listOfNote = new ArrayList<>();

    public DataBase(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        /*
        Создание объекта helper и копирование базы данных в listOfNote, а также инициализация
        надсмоторщика за старыми записями
         */
        helper = new DataBaseHelper(context, name, factory, version);
        try (SQLiteDatabase databaseReader = helper.getReadableDatabase()) {
            try (Cursor cursorToColums = databaseReader.query(DataBaseHelper.TABLE_NAME,
                    null, null, null, null, null,
                    DataBaseHelper.ColumnsNames.TIME_TO_DELETE.getName())) {
                while (cursorToColums.moveToNext()) {
                    //Создание Note по значениям столбцов
                    listOfNote.add(new Note(
                            cursorToColums.getString(cursorToColums.getColumnIndex(DataBaseHelper.ColumnsNames.NOTE_NAME.getName())),
                            cursorToColums.getString(cursorToColums.getColumnIndex(DataBaseHelper.ColumnsNames.NOTE_TEXT.getName())),
                            cursorToColums.getInt(cursorToColums.getColumnIndex(DataBaseHelper.ColumnsNames.COORDINATE_X.getName())),
                            cursorToColums.getInt(cursorToColums.getColumnIndex(DataBaseHelper.ColumnsNames.COORDINATE_Y.getName())),
                            cursorToColums.getInt(cursorToColums.getColumnIndex(DataBaseHelper.ColumnsNames.TIME_TO_DELETE.getName()))));
                }
            }
        }
        // Установка надсмоторщика за записями, требующими удаления
        Thread oldNotesDeleter = new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(100000);
                    deleteOldNotes();
                }
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        });
        oldNotesDeleter.setDaemon(true);
    }

    public void deleteNote(Note note) {
        /*
        Удаление заметки
         */
        try (SQLiteDatabase databaseWriter = helper.getWritableDatabase()) {
            databaseWriter.delete(DataBaseHelper.TABLE_NAME,
                    DataBaseHelper.ColumnsNames.NOTE_NAME + "=" + note.getName(), null);
            listOfNote.remove(note);
        }
    }

    public void addNewNote(Note note) {
        /*
        Добавление новой метки
         */
        //Не может быть записей с одинаковым именем
        if (nameIsExist(note))
            throw new IllegalArgumentException("Запись с таким именем уже существует");
        try (SQLiteDatabase databaseWriter = helper.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(DataBaseHelper.ColumnsNames.NOTE_NAME.getName(), note.getName());
            values.put(DataBaseHelper.ColumnsNames.NOTE_TEXT.getName(), note.getText());
            values.put(DataBaseHelper.ColumnsNames.COORDINATE_X.getName(), note.getX());
            values.put(DataBaseHelper.ColumnsNames.COORDINATE_Y.getName(), note.getY());
            values.put(DataBaseHelper.ColumnsNames.TIME_TO_DELETE.getName(), note.getTimeToDie());
            databaseWriter.insert(DataBaseHelper.TABLE_NAME, null, values);
        }
        listOfNote.add(note);
    }

    public boolean nameIsExist(Note note) {
        /*
        Не может быть записей с одинаковым именем
         */
        return listOfNote.contains(note);
    }

    public void updateNote(Note note, DataBaseHelper.ColumnsNames columnToUpdate, String value) {
        /*
        Обновление какого-то пункта в записи
        nameNote - название записи
        columnToUpdate - колонка для изменения
        value - значение для изменения
         */
        try (SQLiteDatabase databaseWriter = helper.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(columnToUpdate.getName(), value);
            databaseWriter.update(DataBaseHelper.TABLE_NAME, values,
                    DataBaseHelper.ColumnsNames.NOTE_NAME.getName() + "=" + note.getName(), null);
        }
        note.update(columnToUpdate, value);
    }

    public void updateNote(Note note, DataBaseHelper.ColumnsNames columnToUpdate, long value) {
        /*
        Обновление какого-то пункта в записи
        nameNote - название записи
        columnToUpdate - колонка для изменения
        value - значение для изменения
         */
        try (SQLiteDatabase databaseWriter = helper.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(columnToUpdate.getName(), value);
            databaseWriter.update(DataBaseHelper.TABLE_NAME, values,
                    DataBaseHelper.ColumnsNames.NOTE_NAME.getName() + "=" + note, null);
        }
        note.update(columnToUpdate, value);
    }

    public List<Note> getListOfNotes() {
        /*
        Возвращение всех записей
         */
        return listOfNote;
    }

    private void deleteOldNotes() throws IOException {
        /*
        Удаление старых записей
         */
        SQLiteDatabase databaseWriter = null;
        Collections.sort(listOfNote);
        for (int i = 0; i < listOfNote.size(); i++) {
            Note note = listOfNote.get(i);
            if (note.getTimeToDie() < System.currentTimeMillis()) {
                if (databaseWriter == null)
                    databaseWriter = helper.getWritableDatabase();
                databaseWriter.delete(DataBaseHelper.TABLE_NAME, DataBaseHelper.ColumnsNames.NOTE_NAME + "=" + note.getName(), null);
                listOfNote.remove(note);
            }
            //Прекращение выполнения, т. к. записи отсортированы по времени удаления
            else
                break;
        }
        if (databaseWriter != null)
            databaseWriter.close();
    }

    public static class DataBaseHelper extends SQLiteOpenHelper {
        //Используется где-то в onUpgrade
        public static int DATABASE_VERSION = 1;
        //Константы для объявления столбцов базы данных
        public enum ColumnsNames {
            NOTE_NAME("NAME"),
            NOTE_TEXT("TEXT"),
            COORDINATE_X("X"),
            COORDINATE_Y("Y"),
            TIME_TO_DELETE("TIME_TO_DELETE");

            private final String name;
            ColumnsNames(String name) {
                this.name = name;
            }

            public String getName() {
                return name;
            }
        }

        public final static String TABLE_NAME = "NOTE_TABLE";


        public DataBaseHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
        /*
        Создание базы данных через SQL команду
         */
            db.execSQL("CREATE TABLE " + ColumnsNames.NOTE_NAME + " ( " + ColumnsNames.NOTE_NAME + " TEXT, "
                    + ColumnsNames.NOTE_TEXT + " TEXT, " + ColumnsNames.COORDINATE_X + " INTEGER, "
                    + ColumnsNames.COORDINATE_Y + " INTEGER, " + ColumnsNames.TIME_TO_DELETE +
                    " INTEGER " + " );");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        /*
        Когда-нибудь я пойму для чего нужен этот метод, но не сегодня
         */
        }
    }
}
