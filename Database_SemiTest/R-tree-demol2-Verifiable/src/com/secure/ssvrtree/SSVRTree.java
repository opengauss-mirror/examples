package com.secure.ssvrtree;

import com.secure.rtree.Constants;
import com.secure.rtree.RTNode;
import com.secure.rtree.Rectangle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * @ClassName RTree
 * @Description
 */
public class SSVRTree implements Serializable
{
    /**
     * 根节点
     */
    public SSVRTNode root;

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

    public SSVRTree(int capacity, float fillFactor, int type, int dimension)
    {
        this.fillFactor = fillFactor;
        tree_type = type;
        nodeCapacity = capacity;
        this.dimension = dimension;
        root = new SSVRTDataNode(this, Constants.SPNULL);
    }

    /**
     * @return RTree的维度
     */
    public int getDimension()
    {
        return dimension;
    }

    public SSVRTNode getRoot() {
        return root;
    }

    public void setRoot(SSVRTNode root)
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


//    private static List<SSVRTNode> cToS(List<RTNode> children, SSVRTree svrTree) {
//        List<SSVRTNode> schildren = new ArrayList<SSVRTNode>();
//
//
//        for (int i =0; i<children.size();i++){
//            RTNode node  = children.get(i);
//
//            Rectangle[] ds= node.getDatas();
//            int level = node.getLevel();
//            int usedSpace = node.getUsedSpace();
//            SSRectangle[] sRectangles = new SSRectangle[usedSpace];
//            for (int j = 0;j<usedSpace;j++){
//                sRectangles[j] = new SSRectangle();
//                sRectangles[j].set
//            }
//            SVRTNode svrtNode = new SVRTDirNode(svrTree,level, sRectangles, usedSpace);
//
//            schildren.add(svrtNode);
//        }
//
//        return schildren;
//    }

//    private static List<SVRTNode> cToS(List<RTNode> children, SRectangle[] sRts, SSVRTree svrTree) {
//        List<SVRTNode> schildren = new ArrayList<SVRTNode>();
//
//        for (int i =0; i<children.size();i++){
//            RTNode node  = children.get(i);
//
//            Rectangle[] ds= node.getDatas();
//            int level = node.getLevel();
//            int usedSpace = node.getUsedSpace();
//            SRectangle[] sRectangles = new SRectangle[usedSpace];
//            for (int j = 0;j<usedSpace;j++){
//                sRectangles[j] = new SRectangle(new SPoint(ds[j].getLow()));
//            }
//            SVRTNode svrtNode = new SVRTDirNode(svrTree,level, sRectangles, usedSpace);
//            sRts[i].setChild(svrtNode);
//
//            schildren.add(svrtNode);
//        }
//
//        return schildren;
//    }


//    public static SSVRTree createSVRTree(float[] data, int capacity, int dim){
//        RTree tree = new RTree(capacity, 0.4f, Constants.RTREE_QUADRATIC, dim);
//        SSVRTree svrtree = new SSVRTree(capacity, 0.4f, Constants.RTREE_QUADRATIC, dim);
//
//        for(int i = 0; i < data.length;)
//        {
//            double[] dt = new double[dim];
//            for (int j =0;j<dim;j++){
//               dt[j] =data[i++];
//            }
//
//            Point p1 = new Point(dt);
//            final Rectangle rectangle = new Rectangle(p1, p1);
//            tree.insert(rectangle);
//        }
//
//        Queue<RTNode> queue = new LinkedList<>();
//        Queue<SVRTNode> svqueue = new LinkedList<>();
//
//        queue.offer(tree.getRoot());
//
//        Rectangle[] ds = tree.getRoot().getDatas();
//        int level = tree.getRoot().getLevel();
//        int usedSpace = tree.getRoot().getUsedSpace();
//        SRectangle[] sRectangles = new SRectangle[usedSpace];
//        for (int i=0;i<usedSpace;i++){
//            sRectangles[i] = new SRectangle(new SPoint(ds[i].getLow()));
//        }
//        SVRTNode svrtNode = new SVRTDirNode(svrtree,level, sRectangles, usedSpace);
//        svrtree.setRoot(svrtNode);
//
//        svqueue.offer(svrtree.root);
//
//        while (!queue.isEmpty()) {
//            RTNode node = queue.poll();
//            SVRTNode snode = svqueue.poll();
//
//            if(node.getClass().equals(RTDirNode.class)) {
//                RTDirNode dirNode = (RTDirNode) node;
//                List<RTNode> children = dirNode.getChildren();
//                List<SVRTNode> schildren = cToS(children,snode.getDatas(),svrtree);
//
//                ((SVRTDirNode)snode).setChildren(schildren);
//
//                svqueue.addAll(schildren);
//                queue.addAll(children);
//            }
//        }
//        return svrtree;
//    }

}