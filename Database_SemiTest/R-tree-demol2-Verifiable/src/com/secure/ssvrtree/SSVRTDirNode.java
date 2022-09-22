package com.secure.ssvrtree;



import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName RTDirNode
 * @Description 非叶节点
 */
public class SSVRTDirNode extends SSVRTNode implements Serializable
{
    /**
     * 孩子结点
     */
    protected List<SSVRTNode> children;

    public SSVRTDirNode(SSVRTree svrtree, int level, SSRectangle[] datas, int usedSpace)
    {
        super(svrtree,level,datas, usedSpace);
        children = new ArrayList<SSVRTNode>();
    }

    public SSVRTDirNode()
    {
    }

    /**
     * @param index
     * @return 对应索引下的孩子结点
     */
    public SSVRTNode getChild(int index)
    {
        return children.get(index);
    }

    public List<SSVRTNode> getChildren(){ return children; }

    public void setChildren(List<SSVRTNode> children) {
        this.children = children;
    }
}