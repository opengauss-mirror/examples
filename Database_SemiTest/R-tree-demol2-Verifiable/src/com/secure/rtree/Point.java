package com.secure.rtree;

import java.io.Serializable;

/**
 * @ClassName Point
 * @Description n维空间中的点，所有的维度被存储在一个float数组中
 */
public class Point implements Cloneable, Serializable
{
    private double[] data;
    private double[] location;

    //距离
    private double dist=-1;

    public double getDist() {
        return dist;
    }

    public void setDist(double dist) {
        this.dist = dist;
    }

    public Point(double[] data)
    {
        if(data == null)
        {
            throw new IllegalArgumentException("Coordinates cannot be null.");
        }
        if(data.length < 2)
        {
            throw new IllegalArgumentException("Point dimension should be greater than 1.");
        }

        this.data = new double[data.length];
        System.arraycopy(data, 0, this.data, 0, data.length);
    }

    public Point(double[] data, double[] location) {
        this.data = data;
        this.location = location;
    }

    public Point(int[] data)
    {
        if(data == null)
        {
            throw new IllegalArgumentException("Coordinates cannot be null.");
        }
        if(data.length < 2)
        {
            throw new IllegalArgumentException("Point dimension should be greater than 1.");
        }

        this.data = new double[data.length];
        for(int i = 0 ; i < data.length ; i ++)
        {
            this.data[i] = data[i];
        }
    }

    @Override
    protected Object clone()
    {
        double[] copy_data = new double[data.length];
        if (location!=null){
            double[] copy_location = new double[2];
            System.arraycopy(location, 0, copy_location, 0, 2);
            System.arraycopy(data, 0, copy_data, 0, data.length);
            return new Point(copy_data,copy_location);
        }else{
            System.arraycopy(data, 0, copy_data, 0, data.length);
            return new Point(copy_data);
        }

    }

    @Override
    public String toString()
    {
        StringBuffer sBuffer = new StringBuffer("(");

        if (location!=null){
            sBuffer.append(location[0]).append(",");
            sBuffer.append(location[1]).append(",");
        }

        for(int i = 0 ; i < data.length - 1 ; i ++)
        {
            sBuffer.append(data[i]).append(",");
        }

        sBuffer.append(data[data.length - 1]).append(")");

        return sBuffer.toString();
    }

    public void setData(double[] data) {
        this.data = data;
    }

    public double[] getLocation() {
        return location;
    }

    public void setLocation(double[] location) {
        this.location = location;
    }
    public void setLocation(double x,double y) {
        this.location = new double[2];
        this.location[0] = x;
        this.location[1] = y;
    }

    public static void main(String[] args)
    {
        double[] test = {1.2f,2f,34f};
        Point point1 = new Point(test);
        System.out.println(point1);

        int[] test2 = {1,2,3,4};
        point1 = new Point(test2);
        System.out.println(point1);
    }

    /**
     * @return 返回Point的维度
     */
    public int getDimension()
    {
        return data.length;
    }

    public double[] getData() {
        return data;
    }

    /**
     * @param index
     * @return 返回Point坐标第i位的double值
     */
    public double getFloatCoordinate(int index)
    {
        return data[index];
    }

    /**
     * @param index
     * @return 返回Point坐标第i位的int值
     */
    public int getIntCoordinate(int index)
    {
        return (int) data[index];
    }

    @Override
    public boolean equals(Object obj)
    {
        if(obj instanceof Point)
        {
            Point point = (Point) obj;

            if(point.getDimension() != getDimension())
                throw new IllegalArgumentException("Points must be of equal dimensions to be compared.");

            for(int i = 0; i < getDimension(); i ++)
            {
                if(getFloatCoordinate(i) != point.getFloatCoordinate(i))
                    return false;
            }
        }

        if(! (obj instanceof Point))
            return false;

        return true;
    }
}