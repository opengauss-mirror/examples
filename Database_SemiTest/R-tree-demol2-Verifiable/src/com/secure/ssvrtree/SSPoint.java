package com.secure.ssvrtree;






import com.secure.rtree.Point;
import com.secure.util.Paillier;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;

/**
 * @ClassName Point
 * @Description n维空间中的点，所有的维度被存储在一个BigInteger数组中
 */
public class SSPoint implements Cloneable, Serializable
{
    //paillier加密
    private ArrayList<BigInteger> sdata;

    public SSPoint(Point s) {
        for (Double b:s.getData()){
            this.sdata.add(Paillier.Encryption(new BigDecimal(b).toBigIntegerExact()));
        }
    }


    public ArrayList<BigInteger> getSdata() {
        return sdata;
    }

    public void setSdata(ArrayList<BigInteger> sdata) {
        this.sdata = sdata;
    }
}