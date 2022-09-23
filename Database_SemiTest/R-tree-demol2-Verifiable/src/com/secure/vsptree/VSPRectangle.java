package com.secure.vsptree;


import com.secure.svrtree.SPoint;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * 外包矩形
 * @ClassName Rectangle
 * @Description
 */
public class VSPRectangle implements Cloneable
{


    private List<BigInteger> scell;
    private List<ArrayList<BigInteger>> scope;

    //hash
    private String hashScope;
    //签名
    private byte[] sigScopeWithCell;

    /**
     * 孩子结点
     */
    protected VSPRTNode child;





    public VSPRectangle() {
    }



    public VSPRTNode getChild() {
        return child;
    }

    public void setChild(VSPRTNode child) {
        this.child = child;
    }

    public List<BigInteger> getScell() {
        return scell;
    }

    public void setScell(List<BigInteger> scell) {
        this.scell = scell;
    }

    public List<ArrayList<BigInteger>> getScope() {
        return scope;
    }

    public void setScope(List<ArrayList<BigInteger>> scope) {
        this.scope = scope;
    }

    public String getHashScope() {
        return hashScope;
    }

    public void setHashScope(String hashScope) {
        this.hashScope = hashScope;
    }

    public byte[] getSigScopeWithCell() {
        return sigScopeWithCell;
    }

    public void setSigScopeWithCell(byte[] sigScopeWithCell) {
        this.sigScopeWithCell = sigScopeWithCell;
    }

}