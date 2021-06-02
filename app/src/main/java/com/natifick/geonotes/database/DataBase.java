package com.natifick.geonotes.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.io.Closeable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class DataBase implements Closeable {
    //Объект дает доступ к чтению и записи базы данных
    private final SQLiteOpenHelper helper;
    //Список отсортированных замток для удаления устаревших
    private final Set<Note> listOfNotes = new TreeSet<>();
    // Каждому адресу соответствует набор заметок
    private final Map<Address, Set<Note>> addressNoteMap = new HashMap<>();

    /**
     * Создание объекта helper и копирование базы данных в listOfNote, а также инициализация
     * надсмоторщика за старыми записями
     *
     * @param context да кто бы знал зачем это нужно, передавать null
     * @param name    да кто бы знал зачем это нужно, передавать null
     * @param factory да кто бы знал зачем это нужно, передавать null
     * @param version догадываюсь зачем это нужно, передавать 1
     */
    public DataBase(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        helper = new DataBaseHelper(context, name, factory, version);
        try (SQLiteDatabase databaseReader = helper.getReadableDatabase()) {
            try (Cursor cursorToAddress = databaseReader.query(DataBaseHelper.TABLE_ADDRESS_NAME,
                    null, null, null, null, null,
                    null)) {
                while (cursorToAddress.moveToNext()) {
                    //Создание адресов по значениям столбцов
                    addressNoteMap.put(new Address(
                                    cursorToAddress.getDouble(cursorToAddress.getColumnIndex(DataBaseHelper.ColumnsNamesAddress.COORDINATE_X.getName())),
                                    cursorToAddress.getDouble(cursorToAddress.getColumnIndex(DataBaseHelper.ColumnsNamesAddress.COORDINATE_Y.getName())),
                                    cursorToAddress.getString(cursorToAddress.getColumnIndex(DataBaseHelper.ColumnsNamesAddress.ADDRESS.getName()))),
                            new HashSet<>());
                }
            }
        }
        try (SQLiteDatabase databaseReader = helper.getReadableDatabase()) {
            try (Cursor cursorToNotes = databaseReader.query(DataBaseHelper.TABLE_NOTE_NAME,
                    null, null, null, null, null,
                    DataBaseHelper.ColumnsNamesNote.TIME_TO_DELETE.getName())) {
                while (cursorToNotes.moveToNext()) {
                    //Создание Note по значениям столбцов
                    Address address = new Address(cursorToNotes.getDouble(cursorToNotes.getColumnIndex(DataBaseHelper.ColumnsNamesNote.COORDINATE_X.getName())),
                            cursorToNotes.getDouble(cursorToNotes.getColumnIndex(DataBaseHelper.ColumnsNamesNote.COORDINATE_Y.getName())),
                            cursorToNotes.getString(cursorToNotes.getColumnIndex(DataBaseHelper.ColumnsNamesNote.ADDRESS.getName())));
                    Note note = new Note(
                            cursorToNotes.getString(cursorToNotes.getColumnIndex(DataBaseHelper.ColumnsNamesNote.NOTE_NAME.getName())),
                            cursorToNotes.getString(cursorToNotes.getColumnIndex(DataBaseHelper.ColumnsNamesNote.NOTE_TEXT.getName())),
                            address,
                            cursorToNotes.getInt(cursorToNotes.getColumnIndex(DataBaseHelper.ColumnsNamesNote.TIME_TO_DELETE.getName())));
                    listOfNotes.add(note);
                    addressNoteMap.get(address).add(note);
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
     *
     * @param name    имя заметки для удаления
     * @param address адрес для удаления
     */
    public void deleteNote(String address, String name) {
        try (SQLiteDatabase databaseWriter = helper.getWritableDatabase()) {
            databaseWriter.delete(DataBaseHelper.TABLE_NOTE_NAME,
                    DataBaseHelper.ColumnsNamesNote.NOTE_NAME.getName() + "='" + name + "'", null);
        }
        Address addressObj = new Address(address);
        Note note = new Note(name, addressObj);
        listOfNotes.remove(note);
        addressNoteMap.get(addressObj).remove(note);
    }

    /**
     * Добавление нового адреса в базу данных
     *
     * @param address - новый адрес
     */
    public void addNewAddress(Address address) {
        if (addressIsExist(address.getAddress()))
            throw new IllegalArgumentException("Такой адрес уже существует");
        try (SQLiteDatabase databaseWriter = helper.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(DataBaseHelper.ColumnsNamesAddress.ADDRESS.getName(), address.getAddress());
            values.put(DataBaseHelper.ColumnsNamesAddress.COORDINATE_X.getName(), address.getX());
            values.put(DataBaseHelper.ColumnsNamesAddress.COORDINATE_Y.getName(), address.getY());
            databaseWriter.insert(DataBaseHelper.TABLE_ADDRESS_NAME, null, values);
        }
        addressNoteMap.put(address, new HashSet<>());
    }

    /**
     * Добавляет новую запись в базу данных
     *
     * @param address адрес добавления заметки
     * @param note    заметка для добавления
     */
    public void addNewNote(Note note, Address address) {
        //Не может быть записей с одинаковым именем
        if (nameIsExist(note.getName(), address.getAddress()))
            throw new IllegalArgumentException("Запись с таким именем уже существует");
        addressNoteMap.get(address).add(note);
        try (SQLiteDatabase databaseWriter = helper.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(DataBaseHelper.ColumnsNamesNote.NOTE_NAME.getName(), note.getName());
            values.put(DataBaseHelper.ColumnsNamesNote.NOTE_TEXT.getName(), note.getText());
            values.put(DataBaseHelper.ColumnsNamesNote.COORDINATE_X.getName(), address.getX());
            values.put(DataBaseHelper.ColumnsNamesNote.COORDINATE_Y.getName(), address.getY());
            values.put(DataBaseHelper.ColumnsNamesNote.ADDRESS.getName(), address.getAddress());
            values.put(DataBaseHelper.ColumnsNamesNote.TIME_TO_DELETE.getName(), note.getTimeToDie());
            databaseWriter.insert(DataBaseHelper.TABLE_NOTE_NAME, null, values);

        }
        listOfNotes.add(note);
    }

    /**
     * Удаление адреса и всех связанных с ним заметок из базы данных
     *
     * @param address адрес для удаления
     */
    public void deleteAddress(String address) {
        deleteAllNote(address);
        try (SQLiteDatabase databaseWriter = helper.getWritableDatabase()) {
            databaseWriter.delete(DataBaseHelper.TABLE_ADDRESS_NAME,
                    DataBaseHelper.ColumnsNamesAddress.ADDRESS.getName() + "='" + address + "'", null);
        }
        addressNoteMap.remove(new Address(address));
    }

    /**
     * Удаление всех заметок по данному адресу из базы данных
     *
     * @param address адрес, по которому удаляются заметки
     */
    private void deleteAllNote(String address) {
        for (Note note : addressNoteMap.get(new Address(address)))
            deleteNote(address, note.getName());
    }

    /**
     * Не может быть записей с одинаковым именем под одним адресом, поэтому этот метод проверяет существует ли уже
     * запись с данным именем под данным адресом в базе данных, использовать перед вызовом метода addNewNote(Note note,
     * Address address)
     *
     * @param name    имя на проверку
     * @param address адрес по которому заводится заметка
     * @return Результат проверки, true - имя уже есть в базе, false - имени в базе нет, можно
     * использовать
     */
    public boolean nameIsExist(String name, String address) {
        Address addressObj = new Address(address);
        return addressNoteMap.get(addressObj).contains(new Note(name, addressObj));
    }

    /**
     * Не может быть адресов с одинаковым именем, поэтому этот метод проверяет существует ли уже адрес
     * с данным именем в базе данных, использовать перед вызовом метод addNewAddress(Address address)
     *
     * @param address адрес для проверки
     * @return Результат проверки, true - адрес уже есть в базе, false - адреса в базе нет, можно
     * * использовать
     */
    public boolean addressIsExist(String address) {
        return addressNoteMap.containsKey(new Address(address));
    }

    /**
     * Обновление какого-то пункта в записи, использовать при обновлении существующей записи
     *
     * @param name           название записи
     * @param columnToUpdate колонка для изменения
     * @param value          значение для изменения
     */
    public void updateNote(String name, DataBaseHelper.ColumnsNamesNote columnToUpdate, String value) {
        try (SQLiteDatabase databaseWriter = helper.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(columnToUpdate.getName(), value);
            databaseWriter.update(DataBaseHelper.TABLE_NOTE_NAME, values,
                    DataBaseHelper.ColumnsNamesNote.NOTE_NAME.getName() + "=" + name, null);

        }
        for (Note note : listOfNotes)
            if (note.getName().equals(name))
                note.update(columnToUpdate, value);
    }

    /**
     * Возвращает список всех заметок, использовать для вывода заметок на экран
     *
     * @return Возвращает множество заметок, уникальных по названию
     */
    public Set<Note> getSetOfNotes() {
        return listOfNotes;
    }

    /**
     * Возвращает множество заметок по адресу
     *
     * @param address адрес заметок
     * @return множество заметок
     */
    public Set<Note> getSetOfNoteByAddress(String address) {
        return addressNoteMap.get(new Address(address));
    }

    /**
     * Возвращает список всех адресов
     *
     * @return список адресов
     */
    public Set<Address> getSetOfAddresses() {
        return addressNoteMap.keySet();
    }

    /**
     * Возвращет адрес по координатам. Использовать для проверки на то, привязан ли уже какой-либо адрес
     * к выбранным координатам
     * @param x координата
     * @param y координата
     * @return адрес, если он существует, иначе null
     */
    public Address getAddressByCoordinates(double x, double y) {
        for (Address address : addressNoteMap.keySet())
            if (address.getX() == x && address.getY() == y)
                return address;
        return null;
    }

    /**
     * Закрывает базу данных
     */
    public void close(){
        helper.close();
    }

    /**
     * Производит удаление устаревших записей, работает в отдельном потоке. На данный момент метод
     * является бесполезным, но может пригодиться при добавлении возможности указывать время жизни
     * заметок
     */
    private void deleteOldNotes() {
        SQLiteDatabase databaseWriter = null;
        Set<Note> setNoteToDelete = new HashSet<>();
        for (Note note : listOfNotes) {
            if (note.getTimeToDie() < System.currentTimeMillis()) {
                if (databaseWriter == null)
                    databaseWriter = helper.getWritableDatabase();
                databaseWriter.delete(DataBaseHelper.TABLE_NOTE_NAME, DataBaseHelper.ColumnsNamesNote.NOTE_NAME.getName() + "=" + note.getName(), null);
                setNoteToDelete.add(note);
            }
            //Прекращение выполнения, т. к. записи отсортированы по времени удаления
            else
                break;
        }
        if (databaseWriter != null)
            databaseWriter.close();
        for (Note note : setNoteToDelete) {
            listOfNotes.remove(note);
            addressNoteMap.get(note.getAddress()).remove(note);
        }
    }

    public static class DataBaseHelper extends SQLiteOpenHelper {
        //Используется где-то в onUpgrade
        public static int DATABASE_VERSION = 1;

        //Константы для объявления столбцов базы данных
        public enum ColumnsNamesNote {
            NOTE_NAME("NAME"),
            NOTE_TEXT("TEXT"),
            COORDINATE_X("X"),
            COORDINATE_Y("Y"),
            ADDRESS("ADDRESS"),
            TIME_TO_DELETE("TIME_TO_DELETE");

            private final String name;

            ColumnsNamesNote(String name) {
                this.name = name;
            }

            public String getName() {
                return name;
            }
        }

        public enum ColumnsNamesAddress {
            ADDRESS("ADDRESS"),
            COORDINATE_X("X"),
            COORDINATE_Y("Y");

            private final String name;

            ColumnsNamesAddress(String name) {
                this.name = name;
            }

            public String getName() {
                return name;
            }
        }

        public final static String TABLE_NOTE_NAME = "NOTE_TABLE";
        public final static String TABLE_ADDRESS_NAME = "ADDRESS_TABLE";


        public DataBaseHelper(@Nullable Context context, @Nullable String name,
                              @Nullable SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        /**
         * Первичная инициализация базы данных
         *
         * @param db база данных для инициализации
         */
        @Override
        public void onCreate(SQLiteDatabase db) {
            //Создание таблицы для заметок
            db.execSQL("CREATE TABLE " + TABLE_NOTE_NAME + " ( " + ColumnsNamesNote.NOTE_NAME.getName() + " TEXT, "
                    + ColumnsNamesNote.NOTE_TEXT.getName() + " TEXT, " + ColumnsNamesNote.COORDINATE_X.getName() + " DOUBLE, "
                    + ColumnsNamesNote.COORDINATE_Y.getName() + " DOUBLE, " + ColumnsNamesNote.ADDRESS.getName() + " TEXT, " +
                    ColumnsNamesNote.TIME_TO_DELETE.getName() + " INTEGER " + " );");
            //Создание таблицы для адресов
            db.execSQL("CREATE TABLE " + TABLE_ADDRESS_NAME + " ( " + ColumnsNamesAddress.ADDRESS.getName() +
                    " TEXT, " + ColumnsNamesAddress.COORDINATE_X.getName() + " DOUBLE, "
                    + ColumnsNamesAddress.COORDINATE_Y.getName() + " DOUBLE " + " );");
        }

        /**
         * Когда-нибудь я пойму для чего нужен этот метод, но не сегодня
         *
         * @param db         - база данных
         * @param oldVersion - старая версия базы данных
         * @param newVersion - новая версия базы данных
         */
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }
}
