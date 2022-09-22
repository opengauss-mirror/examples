package com.secure.ssvrtree;



import java.io.Serializable;

/**
 * @ClassName RTDataNode
 * @Description
 */
public class SSVRTDataNode extends SSVRTNode implements Serializable
{

    public SSVRTDataNode(SSVRTree ssvrtree, int level, SSRectangle[] datas)
    {
        super(ssvrtree,level,datas, 0);
    }

    public SSVRTDataNode(SSVRTree ssvrtree, SSVRTNode parent)
    {
        super(ssvrtree, parent, 0);
    }



}