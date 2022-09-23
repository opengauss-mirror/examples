package com.secure.svrtree;

import java.io.Serializable;

/**
 * @ClassName RTDataNode
 * @Description
 */
public class SVRTDataNode extends SVRTNode implements Serializable
{

    public SVRTDataNode(SVRTree svrtree, int level, SRectangle[] datas)
    {
        super(svrtree,level,datas, 0);
    }

    public SVRTDataNode(SVRTree svrtree, SVRTNode parent)
    {
        super(svrtree, parent, 0);
    }



}