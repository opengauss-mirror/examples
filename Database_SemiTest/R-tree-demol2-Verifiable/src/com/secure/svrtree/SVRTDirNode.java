package com.secure.svrtree;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName RTDirNode
 * @Description 非叶节点
 */
public class SVRTDirNode extends SVRTNode implements Serializable
{
    /**
     * 孩子结点
     */
    protected List<SVRTNode> children;

    public SVRTDirNode(SVRTree svrtree, int level, SRectangle[] datas, int usedSpace)
    {
        super(svrtree,level,datas, usedSpace);
        children = new ArrayList<SVRTNode>();
    }

    public SVRTDirNode()
    {
    }

    /**
     * @param index
     * @return 对应索引下的孩子结点
     */
    public SVRTNode getChild(int index)
    {
        return children.get(index);
    }

    public List<SVRTNode> getChildren(){ return children; }

    public void setChildren(List<SVRTNode> children) {
        this.children = children;
    }
}