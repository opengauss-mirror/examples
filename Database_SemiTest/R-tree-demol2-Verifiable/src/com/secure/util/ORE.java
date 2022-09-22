package com.secure.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;

public class ORE {

    public static BigInteger Enc(int c){
        String res = null;
        BigInteger enres = null;
        try {
            String[] ars = new String[] { "D:\\anaconda3\\python.exe", "ORE2.py", String.valueOf(c) };
            Process proc = Runtime.getRuntime().exec(ars);// 执行py文件

            BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line = null;
            while ((line = in.readLine()) != null) {
               res = line;
            }

           enres = new BigInteger("0");
            in.close();
            int re = proc.waitFor();
//            System.out.println(re==1?"----状态码1----运行失败":"----状态码0----运行成功");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return enres;
    }

    public static void main(String[] args) throws Exception {
//        BigInteger a = new BigInteger("48005400");
//        BigInteger b = new BigInteger("48005402");
        int a = 48005400;
        int b = 48005401;

//        try {
////            String[] ars = new String[] { "D:\\anaconda3\\python.exe", "D:\\ORE.py", String.valueOf(a) };
////            Process proc = Runtime.getRuntime().exec(ars);// 执行py文件
////
////            BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
////            String line = null;
////            while ((line = in.readLine()) != null) {
////                System.out.println(line);
////            }
////            in.close();
////            int re = proc.waitFor();
////            System.out.println(re==1?"----状态码1----运行失败":"----状态码0----运行成功");
////        } catch (IOException e) {
////            e.printStackTrace();
////        } catch (InterruptedException e) {
////            e.printStackTrace();
////        }


        System.out.println(a);
        System.out.println(b);
        System.out.println(ORE.Enc(a));
        System.out.println(ORE.Enc(b));
    }
}