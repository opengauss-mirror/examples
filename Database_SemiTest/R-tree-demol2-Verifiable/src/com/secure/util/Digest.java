package com.secure.util;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Digest {

    // MD5 单向加密
    public static void main(String[] args) throws NoSuchAlgorithmException,
            UnsupportedEncodingException {
        BigInteger x = new BigInteger("ff",16);
        System.out.println(x);
    }
    public static String afterMD5(String str) throws NoSuchAlgorithmException,
            UnsupportedEncodingException {
        // 获取MD5 加密对象,还可以获取SHA加密对象
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        // 将输入的信息使用指定的编码方式获取字节
        byte[] bytes = str.getBytes("UTF-8");
        // 使用md5 类来获取摘要，也就是加密后的字节
        md5.update(bytes);
        byte[] md5encode = md5.digest();
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < md5encode.length; i++) {
            // 使用&0xff 不足24高位，因为只占了8低位
            int val = ((int) md5encode[i]) & 0xff;
            if (val < 16) {
                buffer.append("0");
            }
            // 以十六进制（基数 16）无符号整数形式返回一个整数参数的字符串表示形式。
            buffer.append(Integer.toHexString(val));
        }
        return buffer.toString();
    }

}
