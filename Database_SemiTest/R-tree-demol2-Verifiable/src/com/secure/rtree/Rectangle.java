package com.secure.rtree;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 外包矩形
 * @ClassName Rectangle
 * @Description
 */
public class Rectangle implements Cloneable, Serializable
{
    private Point low;
    private Point high;
    private List<BigInteger> cell;
    private List<ArrayList<Double>> scope;

    //补
    private double dis=-1;

    public double getDis() {
        return dis;
    }

    public void setDis(double dis) {
        this.dis = dis;
    }

    public List<BigInteger> getCell() {
        return cell;
    }

    public void setCell(List<BigInteger> cell) {
        this.cell = cell;
    }

    public List<ArrayList<Double>> getScope() {
        return scope;
    }

    public void setScope(List<ArrayList<Double>> scope) {
        this.scope = scope;
    }

    public Rectangle(List<BigInteger> cell, List<ArrayList<Double>> scope){
        this.cell =cell;
        this.scope = scope;

        //矩形四个点
        ArrayList<BigInteger> p1 = new ArrayList<>();
        ArrayList<BigInteger> p2 = new ArrayList<>();
        ArrayList<BigInteger> p3 = new ArrayList<>();
        ArrayList<BigInteger> p4 = new ArrayList<>();

        double x = scope.get(0).get(0);
        if (x<0){
            p1.add(new BigInteger("0"));
            p2.add(new BigInteger("0"));
            p3.add(new BigInteger("0"));
            p4.add(new BigInteger("0"));
        }else{
            p1.add(new BigDecimal(Math.floor(scope.get(0).get(0))).toBigIntegerExact());
            p2.add(new BigDecimal(Math.floor(scope.get(0).get(0))).toBigIntegerExact());
            p3.add(new BigDecimal(Math.floor(scope.get(0).get(0))).toBigIntegerExact());
            p4.add(new BigDecimal(Math.floor(scope.get(0).get(0))).toBigIntegerExact());
        }

        p1.add(new BigDecimal(Math.floor(scope.get(0).get(1))).toBigIntegerExact());
        p2.add(new BigDecimal(Math.floor(scope.get(0).get(1))).toBigIntegerExact());
        p3.add(new BigDecimal(Math.floor(scope.get(0).get(1))).toBigIntegerExact());
        p4.add(new BigDecimal(Math.floor(scope.get(0).get(1))).toBigIntegerExact());

        for (int i = 0; i < scope.size(); i++){
            ArrayList<Double> point = scope.get(i);
            BigInteger d1 = new BigDecimal(Math.floor(point.get(0))).toBigIntegerExact();
            BigInteger d2 = new BigDecimal(Math.floor(point.get(1))).toBigIntegerExact();

            if (d1.compareTo(new BigInteger("0"))<0){
                p1.set(0,new BigInteger("0"));
                p4.set(0,new BigInteger("0"));
            }else{
                if (d1.compareTo(p1.get(0))<0){
                    p1.set(0,d1);
                    p4.set(0,d1);
                }
            }


            if (d1.compareTo(p2.get(0))>0){
                p2.set(0,d1);
                p3.set(0,d1);
            }

            if (d2.compareTo(p1.get(1))>0){
                p1.set(1,d2);
                p2.set(1,d2);
            }
            if (d2.compareTo(p3.get(1))<0){
                p3.set(1,d2);
                p4.set(1,d2);
            }
        }

        double[] lowAray = {p4.get(0).doubleValue(),p4.get(1).doubleValue()};
        this.low = new Point(lowAray);

        double[] highAray = {p2.get(0).doubleValue(),p2.get(1).doubleValue()};
        this.high = new Point(highAray);
    }

    public Rectangle(Point p1, Point p2)
    {
        if(p1 == null || p2 == null)
        {
            throw new IllegalArgumentException("Points cannot be null.");
        }
        if(p1.getDimension() != p2.getDimension())
        {
            throw new IllegalArgumentException("Points must be of same dimension.");
        }
        //先左下角后右上角
        for(int i = 0; i < p1.getDimension(); i ++)
        {
            if(p1.getFloatCoordinate(i) > p2.getFloatCoordinate(i))
            {
//                throw new IllegalArgumentException("坐标点为先左下角后右上角");
                //交换p1和p2
                Point tmp = (Point) p1.clone();
                p1 = (Point) p2.clone();
                p2 = tmp;
            }
        }
        low = (Point) p1.clone();
        high = (Point) p2.clone();
    }

    /**
     * 返回Rectangle左下角的Point
     * @return Point
     */
    public Point getLow()
    {
        return (Point) low.clone();
    }

    public Point getLowWithoutClone()
    {
        return low;
    }

    /**
     * 返回Rectangle右上角的Point
     * @return Point
     */
    public Point getHigh()
    {
        return high;
    }

    /**
     * @param rectangle
     * @return 包围两个Rectangle的最小Rectangle
     */
    public Rectangle getUnionRectangle(Rectangle rectangle)
    {
        if(rectangle == null)
            throw new IllegalArgumentException("Rectangle cannot be null.");

        if(rectangle.getDimension() != getDimension())
        {
            throw new IllegalArgumentException("Rectangle must be of same dimension.");
        }

        double[] min = new double[getDimension()];
        double[] max = new double[getDimension()];

        for(int i = 0; i < getDimension(); i ++)
        {
            min[i] = Math.min(low.getFloatCoordinate(i), rectangle.low.getFloatCoordinate(i));
            max[i] = Math.max(high.getFloatCoordinate(i), rectangle.high.getFloatCoordinate(i));
        }

        return new Rectangle(new Point(min), new Point(max));
    }

    /**
     * @return 返回Rectangle的面积
     */
    public double getArea()
    {
        double area = 1;
        for(int i = 0; i < getDimension(); i ++)
        {
            area *= high.getFloatCoordinate(i) - low.getFloatCoordinate(i);
        }

        return area;
    }

    /**
     * @param rectangles
     * @return 包围一系列Rectangle的最小Rectangle
     */
    public static Rectangle getUnionRectangle(Rectangle[] rectangles)
    {
        if(rectangles == null || rectangles.length == 0)
            throw new IllegalArgumentException("Rectangle array is empty.");

        Rectangle r0 = (Rectangle) rectangles[0].clone();
        for(int i = 1; i < rectangles.length; i ++)
        {
            r0 = r0.getUnionRectangle(rectangles[i]);
        }

        return r0;
    }

    @Override
    protected Object clone()
    {
        Point p1 = (Point) low.clone();
        Point p2 = (Point) high.clone();
        return new Rectangle(p1, p2);
    }

    @Override
    public String toString()
    {
        return "Rectangle Low:" + low + " High:" + high;
    }

    public static void main(String[] args)
    {
        double[] f1 = {1.3f,2.4f};
        double[] f2 = {3.4f,4.5f};
        Point p1 = new Point(f1);
        Point p2 = new Point(f2);
        Rectangle rectangle = new Rectangle(p1, p2);
        System.out.println(rectangle);
//		Point point = rectangle.getHigh();
//		point = p1;
//		System.out.println(rectangle);

        double[] f_1 = {0f,0f};
        double[] f_2 = {-2f,2f};
        double[] f_3 = {3f,3f};
        double[] f_4 = {2.5f,2.5f};
        double[] f_5 = {1.5f,1.5f};
        p1 = new Point(f_1);
        p2 = new Point(f_2);
        Point p3 = new Point(f_3);
        Point p4 = new Point(f_4);
        Point p5 = new Point(f_5);
        Rectangle re1 = new Rectangle(p1, p2);
        Rectangle re2 = new Rectangle(p2, p3);
        Rectangle re3 = new Rectangle(p4, p3);
//		Rectangle re4 = new Rectangle(p3, p4);
        Rectangle re5 = new Rectangle(p5, p4);
        System.out.println(re1.isIntersection(re2));
        System.out.println(re1.isIntersection(re3));
        System.out.println(re1.intersectingArea(re2));
        System.out.println(re1.intersectingArea(re5));

        /**
         * 测试新的构造函数
         */
        List<BigInteger> c = new ArrayList<>();
        c.add(new BigInteger("2"));
        c.add(new BigInteger("2"));
        c.add(new BigInteger("2"));
        c.add(new BigInteger("2"));


        ArrayList<Double> a1 = new ArrayList<>();
        a1.add(7.0);
        a1.add(5.0);
        a1.add(6.0);
        a1.add(7.0);

        ArrayList<Double> a2 = new ArrayList<>();
        a2.add(-1.0);
        a2.add(9.0);
        a2.add(6.0);
        a2.add(7.0);

        ArrayList<Double> a3 = new ArrayList<>();
        a3.add(1.0);
        a3.add(1.0);
        a3.add(6.0);
        a3.add(7.0);

        ArrayList<Double> a4 = new ArrayList<>();
        a4.add(5.0);
        a4.add(5.0);
        a4.add(6.0);
        a4.add(7.0);


        List<ArrayList<Double>> list = new ArrayList<>();
        list.add(a1);
        list.add(a2);
        list.add(a3);
//        list.add(a4);
        Rectangle rect2 = new Rectangle(c,list);
        System.out.println(rect2);
    }

    /**
     * 两个Rectangle相交的面积
     * @param rectangle Rectangle
     * @return double
     */
    public double intersectingArea(Rectangle rectangle)
    {
        if(! isIntersection(rectangle))
        {
            return 0;
        }

        double ret = 1;
        for(int i = 0; i < rectangle.getDimension(); i ++)
        {
            double l1 = this.low.getFloatCoordinate(i);
            double h1 = this.high.getFloatCoordinate(i);
            double l2 = rectangle.low.getFloatCoordinate(i);
            double h2 = rectangle.high.getFloatCoordinate(i);

            //rectangle1在rectangle2的左边
            if(l1 <= l2 && h1 <= h2)
            {
                ret *= (h1 - l1) - (l2 - l1);
            }else if(l1 >= l2 && h1 >= h2)
            //rectangle1在rectangle2的右边
            {
                ret *= (h2 - l2) - (l1 - l2);
            }else if(l1 >= l2 && h1 <= h2)
            //rectangle1在rectangle2里面
            {
                ret *= h1 - l1;
            }else if(l1 <= l2 && h1 >= h2)
            //rectangle1包含rectangle2
            {
                ret *= h2 - l2;
            }
        }
        return ret;
    }

    /**
     * @param rectangle
     * @return 判断两个Rectangle是否相交
     */
    public boolean isIntersection(Rectangle rectangle)
    {
        if(rectangle == null)
            throw new IllegalArgumentException("Rectangle cannot be null.");

        if(rectangle.getDimension() != getDimension())
        {
            throw new IllegalArgumentException("Rectangle cannot be null.");
        }


        for(int i = 0; i < getDimension(); i ++)
        {
            if(low.getFloatCoordinate(i) > rectangle.high.getFloatCoordinate(i) ||
                    high.getFloatCoordinate(i) < rectangle.low.getFloatCoordinate(i))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * @return 返回Rectangle的维度
     */
    private int getDimension()
    {
        return low.getDimension();
    }

    /**
     * 判断rectangle是否被包围
     * @param rectangle
     * @return
     */
    public boolean enclosure(Rectangle rectangle)
    {
        if(rectangle == null)
            throw new IllegalArgumentException("Rectangle cannot be null.");

        if(rectangle.getDimension() != getDimension())
            throw new IllegalArgumentException("Rectangle dimension is different from current dimension.");

        for(int i = 0; i < getDimension(); i ++)
        {
            if(rectangle.low.getFloatCoordinate(i) < low.getFloatCoordinate(i) ||
                    rectangle.high.getFloatCoordinate(i) > high.getFloatCoordinate(i))
                return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object obj)
    {
        if(obj instanceof Rectangle)
        {
            Rectangle rectangle = (Rectangle) obj;
            if(low.equals(rectangle.getLow()) && high.equals(rectangle.getHigh()))
                return true;
        }
        return false;
    }


}