package com.secure.vtree;

import com.secure.OPE.OPE;
import com.secure.rtree.Point;
import com.secure.svrtree.SPoint;
import com.secure.util.Digest;
import com.secure.util.Paillier;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;

/**
 * @ClassName Point
 * @Description n维空间中的点，所有的维度被存储在一个BigInteger数组中
 */
public class VPoint implements Cloneable
{
    //paillier加密的hash值
    private BigInteger eHSdata;

    //ORE加密
    private BigInteger[] cdata;
    private BigInteger[] cLocation;
    private byte[] eHCdata;

    public VPoint(BigInteger eHSdata, BigInteger[] cdata, BigInteger[] eLocation, byte[] eHCdata) {
        this.eHSdata = eHSdata;
        this.cdata = cdata;
        this.eHCdata = eHCdata;
        this.cLocation = eLocation;
    }

    public BigInteger[] getcLocation() {
        return cLocation;
    }

    public void setcLocation(BigInteger[] cLocation) {
        this.cLocation = cLocation;
    }

    public BigInteger geteHSdata() {
        return eHSdata;
    }

    public void seteHSdata(BigInteger eHSdata) {
        this.eHSdata = eHSdata;
    }

    public byte[] geteHCdata() {
        return eHCdata;
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

    @Override
    public String toString()
    {
        StringBuffer sBuffer = new StringBuffer("(");
        sBuffer.append(cLocation[0]).append(",").append(cLocation[1]).append(",");

//        sBuffer.append(eHSdata).append("; (");
//
        for(int i = 0 ; i < cdata.length - 1 ; i ++)
        {
            sBuffer.append(cdata[i]).append(",");
        }
        sBuffer.append(cdata[cdata.length - 1]).append(") ");
//
//        sBuffer.append(eHCdata).append(";");
//        sBuffer.append("\n");
//        BigInteger dec = Paillier.Decryption(cdata[0]);
//        BigInteger decrypt = new OPE().decrypt(dec);
//       sBuffer.append("******************(").append(dec);
//       sBuffer.append("****").append(decrypt);
//       sBuffer.append(",");
//        BigInteger dec1 = Paillier.Decryption(cdata[1]);
//        BigInteger decrypt1 = new OPE().decrypt(dec1);
//       sBuffer.append(dec1);
//        sBuffer.append("****").append(decrypt1);
//        sBuffer.append("),");
//        sBuffer.append("\n");

        return sBuffer.toString();
    }

    public static void main(String[] args) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        double[] data ={12,13,14};
        Point p = new Point(data);
        SPoint SPoint1 = new SPoint(p);
        System.out.println(SPoint1);

        for(BigInteger s: SPoint1.getSdata()){
            System.out.println(Paillier.Decryption(s));
        }

        System.out.println("*****************VPoint*******************************************");
        VPoint vPoint = new VPoint(SPoint1.geteHSdata(),SPoint1.getCdata(),SPoint1.getClocation(),SPoint1.geteHCdata());
        System.out.println(vPoint);

        String str = "";
        for(BigInteger s: SPoint1.getCdata()){
            OPE o = new OPE();
            System.out.println(o.decrypt(Paillier.Decryption(s)));
            str =  str + Paillier.Decryption(s);
        }

        System.out.println(str);
        System.out.println(SPoint1.geteHCdata());
        System.out.println(new BigInteger(Digest.afterMD5(str),16));
    }

}