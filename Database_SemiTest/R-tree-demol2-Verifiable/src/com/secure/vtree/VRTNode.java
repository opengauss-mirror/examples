package com.secure.vtree;

import com.secure.svrtree.SVRTNode;

import java.util.ArrayList;
import java.util.List;

public abstract class VRTNode {
    /**
     * 结点所在的树
     */
    private VRTree vrtree;

    /**
     * 结点所在的层
     */
    private int level;

    /**
     * 相当于条目
     */
    private VRectangle[] datas;

    /**
     * 父节点
     */
    private VRTNode parent;
    /**
     * 结点已用的空间
     */
    private int usedSpace;

    public VRTNode(VRTree vrtree, VRTNode parent, int level)
    {
        this.vrtree = vrtree;
        this.parent = parent;
        this.level = level;
//		this.capacity = capacity;
        datas = new VRectangle[vrtree.getNodeCapacity() + 1];//多出的一个用于结点分裂
        usedSpace = 0;
    }

    public VRTNode(VRTree vrtree, int level, VRectangle[] datas, int usedSpace) {
        this.vrtree = vrtree;
        this.level = level;
        this.datas = datas;
        this.usedSpace = usedSpace;
    }

    public VRTNode(VRTree vrtree, int level, int usedSpace) {
        this.vrtree = vrtree;
        this.level = level;
        this.usedSpace = usedSpace;
    }

    public VRTNode(VRTree vrtree, SVRTNode svrtNode) {
        this.vrtree = vrtree;
        this.level = svrtNode.getLevel();
        this.usedSpace = svrtNode.getUsedSpace();
    }

    public VRTNode() {
    }


    public VRTree getVrtree() {
        return vrtree;
    }

    public void setVrtree(VRTree vrtree) {
        this.vrtree = vrtree;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public VRectangle[] getDatas() {
        return datas;
    }

    public List<VRectangle> getObjects() {
        List<VRectangle> d = new ArrayList<>();
        VRectangle[] datas = getDatas();
        for (VRectangle vRectangle:datas)
            d.add(vRectangle);

        return d;
    }

    public void setDatas(VRectangle[] datas) {
        this.datas = datas;
    }

    public VRTNode getParent() {
        return parent;
    }

    public void setParent(VRTNode parent) {
        this.parent = parent;
    }

    public int getUsedSpace() {
        return usedSpace;
    }

    public void setUsedSpace(int usedSpace) {
        this.usedSpace = usedSpace;
    }
}
