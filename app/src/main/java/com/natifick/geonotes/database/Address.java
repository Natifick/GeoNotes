package com.natifick.geonotes.database;


import java.util.Objects;

public class Address {
    private double X;
    private double Y;
    private String address;

    public Address(double x,double y, String address) {
        X = x;
        Y = y;
        this.address = address;
    }

    Address(String address) {
        X = 0;
        Y = 0;
        this.address = address;
    }

    public double getX() {
        return X;
    }

    void setX(double x) {
        X = x;
    }

    public double getY() {
        return Y;
    }

   void setY(double y) {
        Y = y;
    }

    public String getAddress() {
        return address;
    }

    void setAddress(String address) {
        this.address = address;
    }

    @Override
    public boolean equals(Object o) {
        // Если сами ссылки равны
        if (this == o) return true;
        // Если классы не равны, или объект равен null
        if (o == null || getClass() != o.getClass()) return false;
        // Проверим, равны ли имена
        Address address1 = (Address) o;
        return Objects.equals(address, address1.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }
}