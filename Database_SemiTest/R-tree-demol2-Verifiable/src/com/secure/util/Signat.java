package com.secure.util;

import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import org.apache.commons.codec.binary.Hex;


public class Signat {
    private static RSAPublicKey rsaPublicKey;
    private static RSAPrivateKey rsaPrivateKey;
    private static KeyFactory keyFactory;
    private static Signature signature;


    static {
        //1.初始化密钥
        KeyPairGenerator keyPairGenerator = null;
        try {
            keyPairGenerator = KeyPairGenerator.getInstance("RSA");
//
        keyPairGenerator.initialize(512);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
//        rsaPublicKey = (RSAPublicKey)keyPair.getPublic();
//        ObjectWithFile.writeObjectToFile(rsaPublicKey,"D:\\idea\\SVLSQ\\sig_key\\rsaPublicKey");
          //linux lab_ubuntu
          rsaPublicKey=(RSAPublicKey)ObjectWithFile.readObjectFromFile("//home//lujf//svlsq//sig_key//rsaPublicKey");
          //rsaPublicKey=(RSAPublicKey)ObjectWithFile.readObjectFromFile("//root//svlsq//sig_key//rsaPublicKey");
          //rsaPublicKey=(RSAPublicKey)ObjectWithFile.readObjectFromFile("D:\\idea\\svlsq\\sig_key\\rsaPublicKey");

//        rsaPrivateKey = (RSAPrivateKey)keyPair.getPrivate();
//        ObjectWithFile.writeObjectToFile(rsaPrivateKey,"D:\\idea\\SVLSQ\\sig_key\\rsaPrivateKey");
         //linux lab_ubuntu
            rsaPrivateKey = (RSAPrivateKey)ObjectWithFile.readObjectFromFile("//home//lujf//svlsq//sig_key//rsaPrivateKey");

        //linux openEuler
            //rsaPrivateKey = (RSAPrivateKey)ObjectWithFile.readObjectFromFile("//root//svlsq//sig_key//rsaPrivateKey");
        //rsaPrivateKey = (RSAPrivateKey)ObjectWithFile.readObjectFromFile("D:\\idea\\svlsq\\sig_key\\rsaPrivateKey");//windows


        keyFactory = KeyFactory.getInstance("RSA");
//        ObjectWithFile.writeObjectToFile(keyFactory,"D:\\idea\\SVLSQ\\sig_key\\keyFactory");
//        keyFactory = (KeyFactory)ObjectWithFile.readObjectFromFile("D:\\idea\\SVLSQ\\sig_key\\keyFactory");

        signature = Signature.getInstance("MD5withRSA");
//        ObjectWithFile.writeObjectToFile(signature,"D:\\idea\\SVLSQ\\sig_key\\signature");
//            signature=(Signature) ObjectWithFile.readObjectFromFile("D:\\idea\\SVLSQ\\sig_key\\signature");
     } catch (Exception e) {
        e.printStackTrace();
    }
    }


    public static byte[] excuteSig(String a) {
        byte[] result = null;
        try {
            //2.执行签名
            PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(rsaPrivateKey.getEncoded());

            PrivateKey privateKey = keyFactory.generatePrivate(pkcs8EncodedKeySpec);

            signature.initSign(privateKey);
            signature.update(a.getBytes());
            result = signature.sign();
//            System.out.println("jdk rsa sign : " + Hex.encodeHexString(result));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
    public static boolean excuteUnSig(String src,byte[] result) {
        boolean bool = false;
        try {
            //3.验证签名
            X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(rsaPublicKey.getEncoded());
//            keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(x509EncodedKeySpec);
//            signature = Signature.getInstance("MD5withRSA");
            signature.initVerify(publicKey);
            signature.update(src.getBytes());
            bool = signature.verify(result);
//            System.out.println("jdk rsa verify : " + bool);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bool;
    }

    public static void main(String[] args) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        String src = Digest.afterMD5("121314")+Digest.afterMD5("121314");


        byte[] res = Signat.excuteSig(src);

        System.out.println(Signat.excuteUnSig(src,res));

       return ;
    }
}

