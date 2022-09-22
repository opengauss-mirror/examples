package com.secure.vtree;

import com.secure.rtree.Constants;

/**
 * @ClassName RTree
 * @Description
 */
public class VRTree
{
    /**
     * 根节点
     */
    private VRTNode root;

    /**
     * 树类型
     */
    private int tree_type;

    /**
     * 结点容量
     */
    private int nodeCapacity = -1;

    /**
     * 结点填充因子
     */
    private float fillFactor = -1;

    private int dimension ;

    public VRTree(int capacity, float fillFactor, int type, int dimension)
    {
        this.fillFactor = fillFactor;
        tree_type = type;
        nodeCapacity = capacity;
        this.dimension = dimension;
        root = new VRTDataNode(this, Constants.VNULL);
    }

    /**
     * @return RTree的维度
     */
    public int getDimension()
    {
        return dimension;
    }

    public VRTNode getRoot() {
        return root;
    }

    public void setRoot(VRTNode root)
    {
        this.root = root;
    }


    public float getFillFactor()
    {
        return fillFactor;
    }

    /**
     * @return 返回结点容量
     */
    public int getNodeCapacity()
    {
        return nodeCapacity;
    }



}