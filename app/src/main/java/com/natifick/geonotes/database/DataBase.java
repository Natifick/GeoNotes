package com.natifick.geonotes.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class DataBase {
    //Объект дает доступ к чтению и записи базы данных
    private final SQLiteOpenHelper helper;
    //Список заметок, чтобы не обращаться к базе данных
    private final Set<Note> listOfNote = new TreeSet<>();

    /**
     * Создание объекта helper и копирование базы данных в listOfNote, а также инициализация
     * надсмоторщика за старыми записями
     * @param context да кто бы знал зачем это нужно, передавать null
     * @param name да кто бы знал зачем это нужно, передавать null
     * @param factory да кто бы знал зачем это нужно, передавать null
     * @param version догадываюсь зачем это нужно, передавать 1
     */
    public DataBase(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {

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
                    Thread.sleep(60000);
                    deleteOldNotes();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        oldNotesDeleter.setDaemon(true);
    }

    /**
     * Удаление записей из базы данных
     * @param name имя заметки для удаления
     */
    public void deleteNote(String name) {
        try (SQLiteDatabase databaseWriter = helper.getWritableDatabase()) {
            databaseWriter.delete(DataBaseHelper.TABLE_NAME,
                    DataBaseHelper.ColumnsNames.NOTE_NAME + "=" + name, null);
        }
        listOfNote.remove(new Note(name, null, 0, 0, 0));
    }

    /**
     * Добавляет новую запись в базу данных
     * @param note заметка для добавления
     */
    public void addNewNote(Note note) {
        //Не может быть записей с одинаковым именем
        if (nameIsExist(note.getName()))
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

    /**
     *Не может быть записей с одинаковым именем, поэтому этот метод проверяет существует ли уже
     * запись с данным именем в базе данных, использовать перед вызовом метода addNewNote(Note note)
     * @param name имя на проверку
     * @return Результат проверки, true - имя уже есть в базе, false - имени в базе нет, можно
     * использовать
     */
    public boolean nameIsExist(String name) {
        return listOfNote.contains(new Note(name, null, 0, 0, 0));
    }

    /**
     * Обновление какого-то пункта в записи, использовать при обновлении существующей записи
     * @param name название записи
     * @param columnToUpdate колонка для изменения
     * @param value значение для изменения
     */
    public void updateNote(String name, DataBaseHelper.ColumnsNames columnToUpdate, String value) {
        try (SQLiteDatabase databaseWriter = helper.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(columnToUpdate.getName(), value);
            databaseWriter.update(DataBaseHelper.TABLE_NAME, values,
                    DataBaseHelper.ColumnsNames.NOTE_NAME.getName() + "=" + name, null);
        }
        for (Note note : listOfNote)
            if (note.getName().equals(name))
                note.update(columnToUpdate, value);
    }

    /**
     * Обновление какого-то пункта в записи, использовать при обновлении существующей записи
     * @param name название записи
     * @param columnToUpdate колонка для изменения
     * @param value значение для изменения
     */
    public void updateNote(String name, DataBaseHelper.ColumnsNames columnToUpdate, long value) {
        try (SQLiteDatabase databaseWriter = helper.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(columnToUpdate.getName(), value);
            databaseWriter.update(DataBaseHelper.TABLE_NAME, values,
                    DataBaseHelper.ColumnsNames.NOTE_NAME.getName() + "=" + name, null);
        }
        for (Note note : listOfNote)
            if (note.getName().equals(name))
                note.update(columnToUpdate, value);
    }

    /**
     * Возвращает список всех заметок, использовать для вывода заметок на экран
     * @return Возвращает множество заметок, уникальных по названию
     */
    public Set<Note> getListOfNotes() {
        return listOfNote;
    }

    /**
     * Производит удаление устаревших записей, работает в отдельном потоке. На данный момент метод
     * является бесполезным, но может пригодиться при добавлении возможности указывать время жизни
     * заметок
     */
    private void deleteOldNotes()  {
        SQLiteDatabase databaseWriter = null;
        Set<Note> setNoteToDelete = new HashSet<>();
        for (Note note : listOfNote) {
            if (note.getTimeToDie() < System.currentTimeMillis()) {
                if (databaseWriter == null)
                    databaseWriter = helper.getWritableDatabase();
                databaseWriter.delete(DataBaseHelper.TABLE_NAME, DataBaseHelper.ColumnsNames.NOTE_NAME + "=" + note.getName(), null);
                setNoteToDelete.add(note);
            }
            //Прекращение выполнения, т. к. записи отсортированы по времени удаления
            else
                break;
        }
        if (databaseWriter != null)
            databaseWriter.close();
        for (Note note : setNoteToDelete)
            listOfNote.remove(note);
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

        /**
         * Первичная инициализация базы данных
         * @param db база данных для инициализации
         */
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + ColumnsNames.NOTE_NAME + " ( " + ColumnsNames.NOTE_NAME + " TEXT, "
                    + ColumnsNames.NOTE_TEXT + " TEXT, " + ColumnsNames.COORDINATE_X + " INTEGER, "
                    + ColumnsNames.COORDINATE_Y + " INTEGER, " + ColumnsNames.TIME_TO_DELETE +
                    " INTEGER " + " );");
        }

        /**
         * Когда-нибудь я пойму для чего нужен этот метод, но не сегодня
         * @param db - база данных
         * @param oldVersion - старая версия базы данных
         * @param newVersion - новая версия базы данных
         */
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }
}
