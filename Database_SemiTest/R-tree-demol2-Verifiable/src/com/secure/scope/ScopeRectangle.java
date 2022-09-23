package com.secure.scope;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScopeRectangle {
    private List<BigInteger> cell;
    private List<ArrayList<Double>> scope;


    public Map<List<ArrayList<BigInteger>>,List<BigInteger>> genernateRectangle(Map<List<BigInteger>,List<ArrayList<Double>>> scopeWithCell){

        Map<List<ArrayList<BigInteger>>,List<BigInteger>> rectMaP
                = new HashMap<>();

        for (Map.Entry entry:scopeWithCell.entrySet()) {

            List<ArrayList<BigInteger>> rect = new ArrayList<ArrayList<BigInteger>>();

            List<ArrayList<Double>> pointList = (List<ArrayList<Double>>) entry.getValue();
            List<BigInteger> key = (List<BigInteger>) entry.getKey();

            //矩形四个点
            ArrayList<BigInteger> p1 = new ArrayList<>();
            ArrayList<BigInteger> p2 = new ArrayList<>();
            ArrayList<BigInteger> p3 = new ArrayList<>();
            ArrayList<BigInteger> p4 = new ArrayList<>();

            double x = pointList.get(0).get(0);
            if (x<0){
                p1.add(new BigInteger("0"));
                p2.add(new BigInteger("0"));
                p3.add(new BigInteger("0"));
                p4.add(new BigInteger("0"));
            }else{
                p1.add(new BigDecimal(Math.floor(pointList.get(0).get(0))).toBigIntegerExact());
                p2.add(new BigDecimal(Math.floor(pointList.get(0).get(0))).toBigIntegerExact());
                p3.add(new BigDecimal(Math.floor(pointList.get(0).get(0))).toBigIntegerExact());
                p4.add(new BigDecimal(Math.floor(pointList.get(0).get(0))).toBigIntegerExact());
            }

            p1.add(new BigDecimal(Math.floor(pointList.get(0).get(1))).toBigIntegerExact());
            p2.add(new BigDecimal(Math.floor(pointList.get(0).get(1))).toBigIntegerExact());
            p3.add(new BigDecimal(Math.floor(pointList.get(0).get(1))).toBigIntegerExact());
            p4.add(new BigDecimal(Math.floor(pointList.get(0).get(1))).toBigIntegerExact());

            for (int i = 0; i < pointList.size(); i++){
                ArrayList<Double> point = pointList.get(i);
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
            rect.add(p1);
            rect.add(p2);
            rect.add(p3);
            rect.add(p4);
            rectMaP.put(rect,key);
        }
        return rectMaP;
    }

    public static void main(String[] args) {

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
        a2.add(-7.0);
        a2.add(9.0);
        a2.add(6.0);
        a2.add(7.0);

        ArrayList<Double> a3 = new ArrayList<>();
        a3.add(1.0);
        a3.add(3.0);
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
        list.add(a4);


        List<BigInteger> c2 = new ArrayList<>();
        c2.add(new BigInteger("2"));
        c2.add(new BigInteger("3"));
        c2.add(new BigInteger("2"));
        c2.add(new BigInteger("2"));


        ArrayList<Double> aa1 = new ArrayList<>();
        aa1.add(7.0);
        aa1.add(5.0);
        aa1.add(6.0);
        aa1.add(7.0);

        ArrayList<Double> aa2 = new ArrayList<>();
        aa2.add(2.0);
        aa2.add(9.0);
        aa2.add(6.0);
        aa2.add(7.0);

        ArrayList<Double> aa3 = new ArrayList<>();
        aa3.add(1.0);
        aa3.add(3.0);
        aa3.add(6.0);
        aa3.add(7.0);

        ArrayList<Double> aa4 = new ArrayList<>();
        aa4.add(5.0);
        aa4.add(5.0);
        aa4.add(6.0);
        aa4.add(7.0);


        List<ArrayList<Double>> list2 = new ArrayList<>();
        list2.add(aa1);
        list2.add(aa2);
        list2.add(aa3);
        list2.add(aa4);

        Map<List<BigInteger>,List<ArrayList<Double>>> map =
                new HashMap<List<BigInteger>,List<ArrayList<Double>>>();
        map.put(c,list);
        map.put(c2,list2);

//        ScopeRectangle SR = new ScopeRectangle(map);

    }
}
