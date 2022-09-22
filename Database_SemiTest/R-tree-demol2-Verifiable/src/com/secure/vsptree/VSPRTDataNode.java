package com.secure.vsptree;



/**
 * @ClassName RTDataNode
 * @Description
 */
public class VSPRTDataNode extends VSPRTNode
{

    public VSPRTDataNode(VSPRTree vrtree, int level, VSPRectangle[] datas)
    {
        super(vrtree,level,datas, 0);
    }

    public VSPRTDataNode(VSPRTree vrtree, VSPRTNode parent)
    {
        super(vrtree, parent, 0);
    }



}