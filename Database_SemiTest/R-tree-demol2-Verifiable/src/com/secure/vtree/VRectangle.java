package com.secure.vtree;


/**
 * 外包矩形
 * @ClassName Rectangle
 * @Description
 */
public class VRectangle implements Cloneable
{
    private VPoint low;
    private VPoint high;
    /**
     * 孩子结点
     */
    protected VRTNode child;

    public VPoint getHigh() {
        return high;
    }

    public void setHigh(VPoint high) {
        this.high = high;
    }

    public VRectangle(VPoint low, VPoint high) {
        this.low = low;
        this.high = high;
    }

    public VRectangle(VPoint low, VRTNode child) {
        this.low = low;
        this.child = child;
    }

    public VRectangle(VPoint low) {
        this.low = low;
    }

    public VRectangle() {
    }

    public VPoint getLow() {
        return low;
    }

    public void setLow(VPoint low) {
        this.low = low;
    }

    public VRTNode getChild() {
        return child;
    }

    public void setChild(VRTNode child) {
        this.child = child;
    }

    @Override
    public String toString()
    {
        return "VRectangle Low:" + low +";VRectangle high:"+high;
    }
}