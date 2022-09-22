package com.secure.test;

import java.math.BigInteger;

public class TestBiginteger {
    public static void main(String[] args) {
        String str1 = "12312";
        String str2 = "14502";
        String str3 = "87654321";

        BigInteger b_int1 = new BigInteger(str1);
        BigInteger b_int2 = new BigInteger(str2);
        BigInteger b_int3 = new BigInteger(str3);

        // Display b_int1 , b_int2 and str3
        System.out.println("b_int1: " + b_int1);
        System.out.println("b_int2: " + b_int2);
        System.out.println("b_int3: " + b_int3);

        double d1 = b_int3.doubleValue();
        System.out.println(d1);

        System.out.println(b_int1.equals(b_int3));
        System.out.println(b_int1.compareTo(b_int2));
    }
}
