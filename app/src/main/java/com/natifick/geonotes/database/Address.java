package com.natifick.geonotes.database;

import java.util.Objects;

public class Address {
    private int X;
    private int Y;
    private String address;

    public Address(int x, int y, String address) {
        X = x;
        Y = y;
        this.address = address;
    }

    Address(String address) {
        X = 0;
        Y = 0;
        this.address = address;
    }

    public int getX() {
        return X;
    }

    void setX(int x) {
        X = x;
    }

    public int getY() {
        return Y;
    }

   void setY(int y) {
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Address address1 = (Address) o;
        return Objects.equals(address, address1.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }
}