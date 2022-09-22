package com.secure.vsptree;




import com.secure.svrtree.SVRTNode;

import java.util.ArrayList;
import java.util.List;

public abstract class VSPRTNode {
    /**
     * 结点所在的树
     */
    private VSPRTree vrtree;

    /**
     * 结点所在的层
     */
    private int level;

    /**
     * 相当于条目
     */
    private VSPRectangle[] datas;

    /**
     * 父节点
     */
    private VSPRTNode parent;
    /**
     * 结点已用的空间
     */
    private int usedSpace;

    public VSPRTNode(VSPRTree vrtree, VSPRTNode parent, int level)
    {
        this.vrtree = vrtree;
        this.parent = parent;
        this.level = level;
//		this.capacity = capacity;
        datas = new VSPRectangle[vrtree.getNodeCapacity() + 1];//多出的一个用于结点分裂
        usedSpace = 0;
    }

    public VSPRTNode(VSPRTree vrtree, int level, VSPRectangle[] datas, int usedSpace) {
        this.vrtree = vrtree;
        this.level = level;
        this.datas = datas;
        this.usedSpace = usedSpace;
    }

    public VSPRTNode(VSPRTree vrtree, int level, int usedSpace) {
        this.vrtree = vrtree;
        this.level = level;
        this.usedSpace = usedSpace;
    }

    public VSPRTNode(VSPRTree vrtree, SVRTNode svrtNode) {
        this.vrtree = vrtree;
        this.level = svrtNode.getLevel();
        this.usedSpace = svrtNode.getUsedSpace();
    }

    public VSPRTNode() {
    }


    public VSPRTree getVrtree() {
        return vrtree;
    }

    public void setVrtree(VSPRTree vrtree) {
        this.vrtree = vrtree;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public VSPRectangle[] getDatas() {
        return datas;
    }

    public List<VSPRectangle> getObjects() {
        List<VSPRectangle> d = new ArrayList<>();
        VSPRectangle[] datas = getDatas();
        for (VSPRectangle vRectangle:datas)
            d.add(vRectangle);

        return d;
    }

    public void setDatas(VSPRectangle[] datas) {
        this.datas = datas;
    }

    public VSPRTNode getParent() {
        return parent;
    }

    public void setParent(VSPRTNode parent) {
        this.parent = parent;
    }

    public int getUsedSpace() {
        return usedSpace;
    }

    public void setUsedSpace(int usedSpace) {
        this.usedSpace = usedSpace;
    }
}
