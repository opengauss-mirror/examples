package com.secure.svrtree;

import com.secure.OPE.OPE;
import com.secure.rtree.Point;
import com.secure.util.ORE;
import com.secure.util.Paillier;
import com.secure.util.Digest;
import com.secure.util.Signat;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLOutput;

/**
 * @ClassName Point
 * @Description n维空间中的点，所有的维度被存储在一个BigInteger数组中
 */
public class SPoint implements Cloneable, Serializable
{
    //paillier加密
    private BigInteger[] sdata;
    private BigInteger[] slocation;
    private BigInteger[] clocation;
    //
    private BigInteger eHSdata;
    //ORE加密
    private BigInteger[] cdata;
    private byte[] eHCdata;
    //距离
    private double sDist=-1;

    public double getsDist() {
        return sDist;
    }

    public void setsDist(double sDist) {
        this.sDist = sDist;
    }

    public BigInteger geteHSdata() {
        return eHSdata;
    }

    public byte[] geteHCdata() {
        return eHCdata;
    }

    public SPoint(BigInteger[] sdata, BigInteger eHSdata, BigInteger[] cdata, byte[] eHCdata) {
        this.sdata = sdata;
        this.eHSdata = eHSdata;
        this.cdata = cdata;
        this.eHCdata = eHCdata;
    }

    public SPoint(Point point) {
            double[] data = point.getData();

            this.sdata = new BigInteger[data.length];
            this.cdata = new BigInteger[data.length];

            OPE o = new OPE();
            String sdatastr = "";
            String cdatastr = "";
            BigInteger tmp ;
            String hCdata = "";
            String hSdata = "";

        if (point.getLocation()!=null){
            this.slocation = new BigInteger[2];
            this.clocation = new BigInteger[2];
            double[] loc =  point.getLocation();
            for (int i = 0;i<2;i++) {
                this.slocation[i] = Paillier.Encryption(new BigDecimal(loc[i]).toBigIntegerExact());

                this.clocation[i] = new BigDecimal(loc[i]).toBigIntegerExact();
            }
        }

            if (point.getLocation()!=null) {
//                if (getClocation()[0].compareTo(new BigInteger("16"))==0){
//                    System.out.println("16！！！！！！！！！！！！！");
//                }
                sdatastr = sdatastr + getSlocation()[0] + getSlocation()[1];
                cdatastr = cdatastr + getClocation()[0] + getClocation()[1];
            }
            for (int i = 0; i < data.length;i++){

                sdata[i] = Paillier.Encryption(BigInteger.valueOf((int)data[i]));
                sdatastr = sdatastr + (int)data[i];
//                tmp = ORE.Enc((int) data[i]);
                cdata[i] = new BigInteger(Math.round(data[i])+"");
                cdatastr = cdatastr + new BigInteger(Math.round(data[i])+"");
            }
            try {
//                hSdata = Digest.afterMD5(sdatastr);

                hCdata = Digest.afterMD5(cdatastr);
                eHSdata = new BigInteger(hCdata,16);

                byte[] sigValue = Signat.excuteSig(hCdata);
                eHCdata = sigValue;

            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
    }

//    public SPoint(Point point, int type) {
//        double[] data = point.getData();
//
//        this.sdata = new BigInteger[data.length];
//        this.cdata = new BigInteger[data.length];
//
//        String sdatastr = "";
//        String cdatastr = "";
//        BigInteger tmp ;
//        String hCdata = "";
//        String hSdata = "";
//        for (int i = 0; i < data.length;i++){
//
//            sdata[i] = Paillier.Encryption(BigInteger.valueOf((int)data[i]));
//            sdatastr = sdatastr + (int)data[i];
//            tmp = ORE.Enc((int) data[i]);
//            if (type==0){
//                cdata[i] = BigInteger.valueOf((int)data[i]);
//            }else{
//                cdata[i] = Paillier.Encryption(BigInteger.valueOf((int)data[i]));
//            }
//            cdatastr = cdatastr + BigInteger.valueOf((int)data[i]);
//        }
//        try {
//            hSdata = Digest.afterMD5(sdatastr);
//            eHSdata = Paillier.Encryption(new BigInteger(hSdata,16));
//            hCdata = Digest.afterMD5(cdatastr);
//            eHCdata = Paillier.Encryption(new BigInteger(Signat.excuteSig(hSdata+hCdata)));
//        } catch (NoSuchAlgorithmException e) {
//            e.printStackTrace();
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
//    }

    public SPoint() {
    }

    public BigInteger[] getSdata() {
        return sdata;
    }

    public void setSdata(BigInteger[] sdata) {
        this.sdata = sdata;
    }

    public void seteHSdata(BigInteger eHSdata) {
        this.eHSdata = eHSdata;
    }

    public void seteHCdata(byte[] eHCdata) {
        this.eHCdata = eHCdata;
    }

    public BigInteger[] getCdata() {
        return cdata;
    }

    public void setCdata(BigInteger[] cdata) {
        this.cdata = cdata;
    }

    public BigInteger[] getSlocation() {
        return slocation;
    }

    public void setSlocation(BigInteger[] slocation) {
        this.slocation = slocation;
    }

    public BigInteger[] getClocation() {
        return clocation;
    }

    public void setClocation(BigInteger[] clocation) {
        this.clocation = clocation;
    }

    @Override
    public String toString()
    {
        StringBuffer sBuffer = new StringBuffer("(");
        sBuffer.append(clocation[0]).append(",").append(clocation[1]).append(",");

        for(int i = 0 ; i < sdata.length - 1 ; i ++)
        {
            sBuffer.append(Paillier.Decryption(sdata[i])).append(",");
        }

        sBuffer.append(Paillier.Decryption(sdata[sdata.length - 1])).append(") ");

//        sBuffer.append(eHSdata).append("; (");
//
//        for(int i = 0 ; i < cdata.length - 1 ; i ++)
//        {
//            sBuffer.append(cdata[i]).append(",");
//        }
//        sBuffer.append(cdata[cdata.length - 1]).append("),Gigest is ");
//
//        sBuffer.append(eHCdata).append(";");

        return sBuffer.toString();
    }

    public static void main(String[] args) throws UnsupportedEncodingException, NoSuchAlgorithmException, DecoderException {
        double[] data ={12,13,14};
        Point p = new Point(data);
        SPoint SPoint1 = new SPoint(p);
//        System.out.println(SPoint1);

        String dig_str = "";
        for(BigInteger s: SPoint1.getSdata()){
//            System.out.println(Paillier.Decryption(s));
            dig_str = dig_str+Paillier.Decryption(s);
        }

        String sig_str = "";
        for(BigInteger s: SPoint1.getCdata()){
            OPE o = new OPE();
//            System.out.println(s);
            sig_str =  sig_str + s;
        }

//        System.out.println("摘要明文："+dig_str);
//        System.out.println("摘要："+Paillier.Decryption(SPoint1.geteHSdata()));
//        System.out.println("签名索引："+Paillier.Decryption(SPoint1.geteHCdata()));
//
//        System.out.println(new BigInteger(Digest.afterMD5(dig_str),16));
//        System.out.println(">>MD5>>>"+Digest.afterMD5(dig_str)+Digest.afterMD5(sig_str));

        System.out.println("验证签名"+Signat.excuteUnSig(Digest.afterMD5(dig_str)+Digest.afterMD5(sig_str),
                SPoint1.geteHCdata()
              ));
    }

}