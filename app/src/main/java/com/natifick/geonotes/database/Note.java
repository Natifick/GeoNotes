package com.natifick.geonotes.database;

import java.util.Objects;

public class Note implements Comparable<Note>{
    private String name;
    private String text;
    private long Y;
    private long X;
    private long timeToDie;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Note note = (Note) o;
        return Objects.equals(name, note.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    /**
     * Обновление заметки через параметры базы данных
     * @param param параметр для обновления
     * @param value значение обновления
     */
    void update(DataBase.DataBaseHelper.ColumnsNames param, String value) {
        switch (param) {
            case NOTE_NAME:
                name = value;
                break;
            case NOTE_TEXT:
                text = value;
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * Обновление заметки через параметры базы данных
     * @param param параметр для обновления
     * @param value значение обновления
     */
    void update(DataBase.DataBaseHelper.ColumnsNames param, long value) {
        /*
        Обновление параметров через название столбцов в базе данных
         */
        switch (param) {
            case COORDINATE_X:
                X = value;
                break;
            case COORDINATE_Y:
                Y = value;
                break;
            case TIME_TO_DELETE:
                timeToDie = value;
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    public Note(String name, String text, long x, long y, long timeToDie) {
        this.name = name;
        this.text = text;
        Y = y;
        X = x;
        this.timeToDie = timeToDie;
    }

    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    public String getText() {
        return text;
    }

    void setText(String text) {
        this.text = text;
    }

    public long getY() {
        return Y;
    }

   void setY(long y) {
        Y = y;
    }

    public long getX() {
        return X;
    }

    void setX(long x) {
        X = x;
    }

    public long getTimeToDie() {
        return timeToDie;
    }

    void setTimeToDie(long timeToDie) {
        this.timeToDie = timeToDie;
    }

    @Override
    public int compareTo(Note o) {
        if (timeToDie > o.timeToDie)
            return 1;
        else if (timeToDie < o.timeToDie)
            return -1;
        return 0;
    }
}