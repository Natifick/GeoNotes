package com.natifick.geonotes.database;

import java.util.Objects;

public class Note implements Comparable<Note>{
    private String name;
    private String text;
    private long timeToDie;
    private Address address;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Note note = (Note) o;
        return Objects.equals(name, note.name) &&
                Objects.equals(address, note.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, address);
    }

    /**
     * Обновление заметки через параметры базы данных
     * @param param параметр для обновления
     * @param value значение обновления
     */
    void update(DataBase.DataBaseHelper.ColumnsNamesNote param, String value) {
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

    public Note(String name, String text, Address address, long timeToDie) {
        this.name = name;
        this.text = text;
        this.address = address;
        this.timeToDie = timeToDie;
    }

    Note(String name, Address address) {
        this.name = name;
        this.address = address;
        text = "";
        timeToDie = 0;
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

    public Address getAddress() {
        return address;
    }

    void setAddress(Address address) {
        this.address = address;
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