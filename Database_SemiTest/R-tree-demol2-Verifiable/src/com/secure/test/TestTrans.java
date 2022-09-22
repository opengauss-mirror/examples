package com.secure.test;

import java.math.BigDecimal;
import java.math.BigInteger;

public class TestTrans {
    public static void main(String[] args) {
//        testit(Double.MIN_VALUE);
//        testit(Double.MAX_VALUE);
//        testit(0);
//        testit(1.0);
//        testit(Math.E);
//        testit(Math.PI);

        double x = 3.0;
       BigInteger y =  doubleToScaledInteger(x);
    }

    private static void testit(double d) {
        double roundTrip = scaledIntegerToDouble(doubleToScaledInteger(d));
        if (d != roundTrip) {
            System.out.println("ERROR: " + d + " " + roundTrip);
        }
    }

    public static final BigDecimal scale = new BigDecimal("1e1074");

    public static BigInteger doubleToScaledInteger(double d) {
        BigDecimal bd = new BigDecimal(d);
        return bd.toBigIntegerExact();
    }

    public static double scaledIntegerToDouble(BigInteger bi) {
        BigDecimal bd = new BigDecimal(bi);
        return bd.doubleValue();
    }
}
