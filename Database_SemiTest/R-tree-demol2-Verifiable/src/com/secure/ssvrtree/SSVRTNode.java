package com.secure.ssvrtree;



import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public abstract class SSVRTNode implements Serializable {
    /**
     * 结点所在的树
     */
    private SSVRTree svrtree;

    /**
     * 结点所在的层
     */
    private int level;

    /**
     * 相当于条目
     */
    private SSRectangle[] datas;

    /**
     * 父节点
     */
    private SSVRTNode parent;
    /**
     * 结点已用的空间
     */
    private int usedSpace;

    public SSVRTNode(SSVRTree svrtree, SSVRTNode parent, int level)
    {
        this.svrtree = svrtree;
        this.parent = parent;
        this.level = level;
//		this.capacity = capacity;
        datas = new SSRectangle[svrtree.getNodeCapacity() + 1];//多出的一个用于结点分裂
        usedSpace = 0;
    }

    public SSVRTNode(SSVRTree svrtree, int level, SSRectangle[] datas, int usedSpace) {
        this.svrtree = svrtree;
        this.level = level;
        this.datas = datas;
        this.usedSpace = usedSpace;
    }

    public SSVRTNode() {
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


    public SSVRTree getSvrtree() {
        return svrtree;
    }

    public void setSvrtree(SSVRTree svrtree) {
        this.svrtree = svrtree;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public SSRectangle[] getDatas() {
        return datas;
    }

    public List<SSRectangle> getObjects() {
        List<SSRectangle> d = new ArrayList<>();
        SSRectangle[] datas = getDatas();
        for (SSRectangle sRectangle:datas)
            d.add(sRectangle);

        return d;
    }

    public void setDatas(SSRectangle[] datas) {
        this.datas = datas;
    }

    public SSVRTNode getParent() {
        return parent;
    }

    public void setParent(SSVRTNode parent) {
        this.parent = parent;
    }

    public int getUsedSpace() {
        return usedSpace;
    }

    public void setUsedSpace(int usedSpace) {
        this.usedSpace = usedSpace;
    }
}
