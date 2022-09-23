package com.secure.svrtree;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public abstract class SVRTNode implements Serializable {
    /**
     * 结点所在的树
     */
    private SVRTree svrtree;

    /**
     * 结点所在的层
     */
    private int level;

    /**
     * 相当于条目
     */
    private SRectangle[] datas;

    /**
     * 父节点
     */
    private SVRTNode parent;
    /**
     * 结点已用的空间
     */
    private int usedSpace;

    public SVRTNode(SVRTree svrtree, SVRTNode parent, int level)
    {
        this.svrtree = svrtree;
        this.parent = parent;
        this.level = level;
//		this.capacity = capacity;
        datas = new SRectangle[svrtree.getNodeCapacity() + 1];//多出的一个用于结点分裂
        usedSpace = 0;
    }

    public SVRTNode(SVRTree svrtree, int level, SRectangle[] datas, int usedSpace) {
        this.svrtree = svrtree;
        this.level = level;
        this.datas = datas;
        this.usedSpace = usedSpace;
    }

    public SVRTNode() {
    }
    //    public SVRTNode(SVRTree svrtree,RTNode rtNode)
//    {
//        this.svrtree = svrtree;
//        this.parent = parent;
//        this.level = level;
////		this.capacity = capacity;
//        datas = new Rectangle[svrtree.getNodeCapacity() + 1];//多出的一个用于结点分裂
//        usedSpace = 0;
//    }


    public SVRTree getSvrtree() {
        return svrtree;
    }

    public void setSvrtree(SVRTree svrtree) {
        this.svrtree = svrtree;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public SRectangle[] getDatas() {
        return datas;
    }

    public List<SRectangle> getObjects() {
        List<SRectangle> d = new ArrayList<>();
        SRectangle[] datas = getDatas();
        for (SRectangle sRectangle:datas)
            d.add(sRectangle);

        return d;
    }

    public void setDatas(SRectangle[] datas) {
        this.datas = datas;
    }

    public SVRTNode getParent() {
        return parent;
    }

    public void setParent(SVRTNode parent) {
        this.parent = parent;
    }

    public int getUsedSpace() {
        return usedSpace;
    }

    public void setUsedSpace(int usedSpace) {
        this.usedSpace = usedSpace;
    }
}
