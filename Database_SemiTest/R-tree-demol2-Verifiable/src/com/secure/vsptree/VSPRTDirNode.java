package com.secure.vsptree;



import com.secure.svrtree.SVRTNode;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName RTDirNode
 * @Description 非叶节点
 */
public class VSPRTDirNode extends VSPRTNode
{
    /**
     * 孩子结点
     */
    protected List<VSPRTNode> children;

    public VSPRTDirNode(VSPRTree vrtree, int level, VSPRectangle[] datas, int usedSpace)
    {
        super(vrtree,level,datas, usedSpace);
        children = new ArrayList<VSPRTNode>();
    }
    public VSPRTDirNode(VSPRTree vrtree, int level, int usedSpace)
    {
        super(vrtree,level, usedSpace);
    }

    public VSPRTDirNode(VSPRTree vrtree, SVRTNode svrtNode)
    {
        super(vrtree,svrtNode);
    }


    public VSPRTDirNode()
    {
    }

    /**
     * @param index
     * @return 对应索引下的孩子结点
     */
    public VSPRTNode getChild(int index)
    {
        return children.get(index);
    }

    public List<VSPRTNode> getChildren(){ return children; }

    public void setChildren(List<VSPRTNode> children) {
        this.children = children;
    }
}