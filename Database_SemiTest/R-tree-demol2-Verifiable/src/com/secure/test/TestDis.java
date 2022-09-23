package com.secure.test;

public class TestDis {
    public double getDistance(double x1, double y1, double x2, double y2){
        double _x = Math.abs(x1 - x2);
        double _y = Math.abs(y1 - y2);
        return Math.sqrt(_x*_x+_y*_y);
    }

    public static void main(String[] args) {
        double dsi = new TestDis().getDistance(3,0,0,4);
    }
}