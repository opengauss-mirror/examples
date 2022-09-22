package com.secure.svrtree;


import com.secure.rtree.Point;
import com.secure.util.Digest;
import com.secure.util.Paillier;
import com.secure.util.Signat;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * 外包矩形
 * @ClassName Rectangle
 * @Description
 */
public class SRectangle implements Cloneable, Serializable
{
    private SPoint low;
    private SPoint high;

    /**
     * 孩子结点
     */
    protected SVRTNode child;

    private double dis=-1;


    public SRectangle(SPoint low, SPoint high, SVRTNode child) {
        this.low = low;
        this.high = high;
        this.child = child;
    }



    public SRectangle(SPoint low, SVRTNode child) {
        this.low = low;
        this.child = child;
    }

    public SRectangle(SPoint low) {
        this.low = low;
    }

    public SRectangle(SPoint low, SPoint high) {
        this.low = low;
        this.high = high;
    }

    public SRectangle() {
    }

    public SPoint getLow() {
        return low;
    }

    public void setLow(SPoint low) {
        this.low = low;
    }

    public SPoint getHigh() {
        return high;
    }

    public void setHigh(SPoint high) {
        this.high = high;
    }

    public SVRTNode getChild() {
        return child;
    }

    public void setChild(SVRTNode child) {
        this.child = child;
    }

    public double getDis() {
        return dis;
    }

    public void setDis(double dis) {
        this.dis = dis;
    }

    @Override
    public String toString()
    {
        return "SRectangle Low:" + low ;
    }


}