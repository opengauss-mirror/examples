package com.secure.oneVoronoi;

public class PointIntersect {

    public static class Point{
        private double x;
        private double y;

        public Point(double x,double y){
            this.x=x;
            this.y=y;
        }

        public double getX() {
            return x;
        }

        public void setX(double x) {
            this.x = x;
        }

        public double getY() {
            return y;
        }

        public void setY(double y) {
            this.y = y;
        }

        @Override
        public String toString() {
            return "X="+this.x+"Y="+this.y;
        }
    }

    /**
     * 获取两条直线相交的点
     */
    public static Point getIntersectPoint(Point p1, Point p2, Point p3, Point p4){

        double A1=p1.getY()-p2.getY();
        double B1=p2.getX()-p1.getX();
        double C1=A1*p1.getX()+B1*p1.getY();

        double A2=p3.getY()-p4.getY();
        double B2=p4.getX()-p3.getX();
        double C2=A2*p3.getX()+B2*p3.getY();

        double det_k=A1*B2-A2*B1;

        if(Math.abs(det_k)<0.00001){
            return null;
        }

        double a=B2/det_k;
        double b=-1*B1/det_k;
        double c=-1*A2/det_k;
        double d=A1/det_k;

        double x=a*C1+b*C2;
        double y=c*C1+d*C2;

        return  new Point(x,y);
    }

    public static void main(String[] args) {
        Point p1=new Point(1,1);
        Point p2=new Point(5,5);
        Point p3=new Point(2,1);
        Point p4=new Point(2,5);

        Point point=getIntersectPoint(p1,p2,p3,p4);

        if(point==null){
            System.out.print("未相交");
        }else{
            System.out.print("相交于："+point.toString());
        }
    }
}

