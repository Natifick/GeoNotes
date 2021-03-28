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

    public void update(DataBase.DataBaseHelper.ColumnsNames param, String value) {
        /*
        Обновление параметров через название столбцов в базе данных
         */
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

    public void update(DataBase.DataBaseHelper.ColumnsNames param, long value) {
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

    public void setName(String name) {
        this.name = name;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getY() {
        return Y;
    }

    public void setY(long y) {
        Y = y;
    }

    public long getX() {
        return X;
    }

    public void setX(long x) {
        X = x;
    }

    public long getTimeToDie() {
        return timeToDie;
    }

    public void setTimeToDie(long timeToDie) {
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