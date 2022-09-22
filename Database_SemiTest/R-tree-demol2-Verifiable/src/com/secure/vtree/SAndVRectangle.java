package com.secure.vtree;

public class SAndVRectangle <S, V>{
    private S ob1;
    private V ob2;
    private double mindist=-1;

    public double getMindist() {
        return mindist;
    }

    public void setMindist(double mindist) {
        this.mindist = mindist;
    }

    public SAndVRectangle(S ob1, V ob2) {
        this.ob1 = ob1;
        this.ob2 = ob2;
    }

    public S getOb1() {
        return ob1;
    }

    public void setOb1(S ob1) {
        this.ob1 = ob1;
    }

    public V getOb2() {
        return ob2;
    }

    public void setOb2(V ob2) {
        this.ob2 = ob2;
    }
}
