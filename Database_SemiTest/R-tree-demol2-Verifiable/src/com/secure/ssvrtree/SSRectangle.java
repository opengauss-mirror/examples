package com.secure.ssvrtree;


import com.secure.rtree.Point;
import com.secure.svrtree.SPoint;

import com.secure.util.Digest;
import com.secure.util.Paillier;
import com.secure.util.Signat;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * 外包矩形
 * @ClassName Rectangle
 * @Description
 */
public class SSRectangle implements Cloneable, Serializable
{
    /**
     * 孩子结点
     */
    protected SSVRTNode child;

    private List<BigInteger> scell;
    private List<ArrayList<BigInteger>> scope;

    //hash
    private String hashScope;
    //签名
    private byte[] sigScopeWithCell;


    public List<BigInteger> getScell() {
        return scell;
    }

    public void setScell(List<BigInteger> cell) {
        List<BigInteger> spoint = new ArrayList<>();
        for (BigInteger c:cell){
            spoint.add(Paillier.Encryption(c));
        }
        this.scell = spoint;
    }



    public List<ArrayList<BigInteger>> getScope() {
        return scope;
    }

    public String getHashScope() {
        return hashScope;
    }

    public byte[] getSigScopeWithCell() {
        return sigScopeWithCell;
    }

    public void setScopeAndHash(Point lowPoint, Point highPoint){
        ArrayList<BigInteger> low = new ArrayList<>();
        low.add(Paillier.Encryption(new BigDecimal(lowPoint.getData()[0]).toBigIntegerExact()));
        low.add(Paillier.Encryption(new BigDecimal(lowPoint.getData()[1]).toBigIntegerExact()));

        ArrayList<BigInteger> high = new ArrayList<>();
        high.add(Paillier.Encryption(new BigDecimal(highPoint.getData()[0]).toBigIntegerExact()));
        high.add(Paillier.Encryption(new BigDecimal(highPoint.getData()[1]).toBigIntegerExact()));

        List<ArrayList<BigInteger>> sp = new ArrayList<>();

        sp.add(low);
        sp.add(high);
        this.scope = sp;

        String s = lowPoint.getData()[0]+lowPoint.getData()[1]+
                  highPoint.getData()[0]+highPoint.getData()[1]+"";
        try {
            this.hashScope = Digest.afterMD5(s);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void setScopeAndScell(List<ArrayList<Double>> scope,List<BigInteger> cell) {

        List<ArrayList<BigInteger>> SP = new ArrayList<>();
        String s = "";
        for (int i= 0;i<scope.size();i++){
            ArrayList<BigInteger> sP = new ArrayList<BigInteger>();
            ArrayList<BigInteger> cP = new ArrayList<BigInteger>();
//            String str_scope="(";
            for (int j=0;j<scope.get(i).size();j++){
//                str_scope = str_scope+","+scope.get(i).get(j);
               sP.add(Paillier.Encryption(new BigDecimal(scope.get(i).get(j)).toBigIntegerExact()));
               s = s+new BigDecimal(scope.get(i).get(j)).toBigIntegerExact().toString(10);
            }
//            str_scope = str_scope+")";
//            System.out.print(str_scope+">>>>");
            SP.add(sP);
        }
//        System.out.println("\n");
        this.scope = SP;


        List<BigInteger> spoint = new ArrayList<>();
        String string_cell = "";
        for (BigInteger c:cell){
            spoint.add(Paillier.Encryption(c));
            string_cell = string_cell+c.toString(10);
        }
        this.scell = spoint;

        try {
            this.hashScope = Digest.afterMD5(s);
//            System.out.println(string_cell);
//            System.out.println(this.hashScope);
            this.sigScopeWithCell = Signat.excuteSig(Digest.afterMD5(string_cell)+this.hashScope);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }


    public SSRectangle() {
    }

    public SSVRTNode getChild() {
        return child;
    }

    public void setChild(SSVRTNode child) {
        this.child = child;
    }


}