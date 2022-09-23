package com.secure.vtree;

/**
 * @ClassName RTDataNode
 * @Description
 */
public class VRTDataNode extends VRTNode
{

    public VRTDataNode(VRTree vrtree, int level, VRectangle[] datas)
    {
        super(vrtree,level,datas, 0);
    }

    public VRTDataNode(VRTree vrtree, VRTNode parent)
    {
        super(vrtree, parent, 0);
    }



}