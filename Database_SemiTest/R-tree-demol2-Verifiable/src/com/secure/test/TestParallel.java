package com.secure.test;

public class TestParallel {
    /*
     *函数名:if_not_parallel
     *功能:两条直线不垂直的情况下,判断是否相交
     *输入:(x1,y1),(x2,y2)是线段一的两个端点的坐标
     *     (x3,y3),(x4,y4)是线段二的两个端点的坐标
     *输出:返回整型值判断两条线段是否相交
     */
    int if_intersect_np(double x1,double y1,double x2,double y2, double x3,double y3,double x4,double y4)
    {
        double x;
        x=((x1*y2-x2*y1)/(x2-x1)+(x4*y3-x3*y4)/(x4-x3))/((y2-y1)/(x2-x1)-(y4-y3)/(x4-x3));
        if(((x1-x)*(x-x2)>=0)&&((x3-x)*(x-x4)>=0))
            return 1;
        else
            return 0;
    }

    public static void main(String[] args) {
        TestParallel TP=new TestParallel();
        int x = TP.if_intersect_np(12,-8,16,-18,
                10,-6,16,-18);
    }
}
