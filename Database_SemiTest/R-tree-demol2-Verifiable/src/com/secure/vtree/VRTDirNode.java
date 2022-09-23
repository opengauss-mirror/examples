package com.secure.vtree;

import com.secure.svrtree.SVRTNode;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName RTDirNode
 * @Description 非叶节点
 */
public class VRTDirNode extends VRTNode
{
    /**
     * 孩子结点
     */
    protected List<VRTNode> children;

    public VRTDirNode(VRTree vrtree, int level, VRectangle[] datas, int usedSpace)
    {
        super(vrtree,level,datas, usedSpace);
        children = new ArrayList<VRTNode>();
    }
    public VRTDirNode(VRTree vrtree, int level, int usedSpace)
    {
        super(vrtree,level, usedSpace);
    }

    public VRTDirNode(VRTree vrtree, SVRTNode svrtNode)
    {
        super(vrtree,svrtNode);
    }


    public VRTDirNode()
    {
    }

    /**
     * @param index
     * @return 对应索引下的孩子结点
     */
    public VRTNode getChild(int index)
    {
        return children.get(index);
    }

    public List<VRTNode> getChildren(){ return children; }

    public void setChildren(List<VRTNode> children) {
        this.children = children;
    }
}