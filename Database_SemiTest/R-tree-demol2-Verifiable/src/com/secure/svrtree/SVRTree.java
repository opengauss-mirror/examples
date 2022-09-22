package com.secure.svrtree;

import com.secure.rtree.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * @ClassName RTree
 * @Description
 */
public class SVRTree implements Serializable
{
    /**
     * 根节点
     */
    public SVRTNode root;

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

    public SVRTree(int capacity, float fillFactor, int type, int dimension)
    {
        this.fillFactor = fillFactor;
        tree_type = type;
        nodeCapacity = capacity;
        this.dimension = dimension;
        root = new SVRTDataNode(this, Constants.SNULL);
    }

    /**
     * @return RTree的维度
     */
    public int getDimension()
    {
        return dimension;
    }

    public SVRTNode getRoot() {
        return root;
    }

    public void setRoot(SVRTNode root)
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


    private static List<SVRTNode> cToS(List<RTNode> children, SVRTree svrTree) {
        List<SVRTNode> schildren = new ArrayList<SVRTNode>();


        for (int i =0; i<children.size();i++){
            RTNode node  = children.get(i);

            Rectangle[] ds= node.getDatas();
            int level = node.getLevel();
            int usedSpace = node.getUsedSpace();
            SRectangle[] sRectangles = new SRectangle[usedSpace];
            for (int j = 0;j<usedSpace;j++){
                sRectangles[j] = new SRectangle(new SPoint(ds[j].getLow()));
            }
            SVRTNode svrtNode = new SVRTDirNode(svrTree,level, sRectangles, usedSpace);

            schildren.add(svrtNode);
        }

        return schildren;
    }

    private static List<SVRTNode> cToS(List<RTNode> children, SRectangle[] sRts, SVRTree svrTree) {
        List<SVRTNode> schildren = new ArrayList<SVRTNode>();

        for (int i =0; i<children.size();i++){
            RTNode node  = children.get(i);

            Rectangle[] ds= node.getDatas();
            int level = node.getLevel();
            int usedSpace = node.getUsedSpace();
            SRectangle[] sRectangles = new SRectangle[usedSpace];
            for (int j = 0;j<usedSpace;j++){
                sRectangles[j] = new SRectangle(new SPoint(ds[j].getLow()),new SPoint(ds[j].getHigh()));
            }
            SVRTNode svrtNode = new SVRTDirNode(svrTree,level, sRectangles, usedSpace);
            sRts[i].setChild(svrtNode);

            schildren.add(svrtNode);
        }

        return schildren;
    }


    public static SVRTree createSVRTree(double[] data, int capacity, int dim){
        RTree tree = new RTree(capacity, 0.4f, Constants.RTREE_QUADRATIC, dim);
        SVRTree svrtree = new SVRTree(capacity, 0.4f, Constants.RTREE_QUADRATIC, dim);

        for(int i = 0; i < data.length;)
        {
            double[] dt = new double[dim];
            double[] sp = new double[2];

            for (int j =0;j<dim+2;j++){
                if (j<2){
                    sp[j]=data[i++];
                }else{
                    dt[j-2]=data[i++];
                }
            }

            Point p1 = new Point(dt,sp);
            final Rectangle rectangle = new Rectangle(p1, p1);
            tree.insert(rectangle);
        }
        //补充location
        List<RTNode> list = tree.traversePostOrder(tree.getRoot());
        int highLevel = tree.getRoot().getLevel();
        List<ArrayList<RTNode>> listWithLevel = new ArrayList<>();
        for (int h = 0; h <highLevel+1;h++){
            listWithLevel.add(new ArrayList<RTNode>());
        }

        for (RTNode node:list){
            for (int h = 0; h <highLevel+1;h++){
                if (node.getLevel()==h)
                    listWithLevel.get(h).add(node);
            }
        }
        for (int L = 1; L<highLevel+1;L++){
            for (int i = 0;i<listWithLevel.get(L).size();i++){
                RTDirNode parent_node = (RTDirNode)listWithLevel.get(L).get(i);
                List<RTNode> children_node = parent_node.getChildren();
                Rectangle[] children_rect = parent_node.getDatas();
                for (int j =0;j<children_node.size();j++){
                    Rectangle[] set = children_node.get(j).getDatas();
                    double low_x = Double.MAX_VALUE;
                    double low_y = Double.MAX_VALUE;
                    double high_x = Double.MIN_VALUE;
                    double high_y = Double.MIN_VALUE;
                    for (int n =0 ;n<set.length;n++){
                        if (set[n]!=null){
                            if (set[n].getLow().getLocation()[0]<low_x)
                                low_x=set[n].getLow().getLocation()[0];
                            if (set[n].getLow().getLocation()[1]<low_y)
                                low_y=set[n].getLow().getLocation()[1];

                            if (set[n].getHigh().getLocation()[0]>high_x)
                                high_x=set[n].getHigh().getLocation()[0];
                            if (set[n].getHigh().getLocation()[1]>high_y)
                                high_y=set[n].getHigh().getLocation()[1];
                        }
                    }
                    children_rect[j].getLowWithoutClone().setLocation(low_x,low_y);
                    children_rect[j].getHigh().setLocation(high_x,high_y);
                }
            }
        }

        //制作SVRtree
        Queue<RTNode> queue = new LinkedList<>();
        Queue<SVRTNode> svqueue = new LinkedList<>();

        queue.offer(tree.getRoot());

        Rectangle[] ds = tree.getRoot().getDatas();
        int level = tree.getRoot().getLevel();
        int usedSpace = tree.getRoot().getUsedSpace();
        SRectangle[] sRectangles = new SRectangle[usedSpace];
        for (int i=0;i<usedSpace;i++){
            sRectangles[i] = new SRectangle(new SPoint(ds[i].getLow()),new SPoint(ds[i].getHigh()));
        }
        SVRTNode svrtNode = new SVRTDirNode(svrtree, level, sRectangles, usedSpace);
        svrtree.setRoot(svrtNode);

        svqueue.offer(svrtree.root);

        while (!queue.isEmpty()) {
            RTNode node = queue.poll();
            SVRTNode snode = svqueue.poll();

            if(node.getClass().equals(RTDirNode.class)) {
                RTDirNode dirNode = (RTDirNode) node;
                List<RTNode> children = dirNode.getChildren();
                List<SVRTNode> schildren = cToS(children,snode.getDatas(),svrtree);

                ((SVRTDirNode)snode).setChildren(schildren);

                svqueue.addAll(schildren);
                queue.addAll(children);
            }
        }
        return svrtree;
    }


    public static void main(String[] args) throws Exception
    {
        double[] f = {1,9,2,10,
                4,8,6,7,
                9,10,7,5,
                5,6,4,3,
                3,2,9,1,
                10,4,6,2,
                8,3,8,4
        };
//        float[] f = {1,9,2,
//                10,4,8,
//                6,7,9,
//                10,7,5,
//                5,6,4,
//                3,3,2,
//                9,1,10,
//                4,6,2,
//                8,3,8
//        };
        SVRTree svrTree = SVRTree.createSVRTree(f,3, 2);

//        ObjectWithFile.writeObjectToFile(svrTree,"test.bak");
//        SVRTree svrTree2 = (SVRTree)ObjectWithFile.readObjectFromFile("test.bak");

        System.out.println(">>>>>>>>>>>>>>>>遍历SVR-Tree>>>>>>>>>>>>>>>>>>>>>>");
        Queue<SVRTNode> queue = new LinkedList<>();

        queue.offer(svrTree.root);

        while (!queue.isEmpty()) {
            SVRTNode node = queue.poll();
            SRectangle[] srectangles = node.getDatas();
            System.out.println(node.getLevel());
            for (int j = 0; j < srectangles.length; j++)
                System.out.println(srectangles[j]);
            if(node.getClass().equals(SVRTDirNode.class)) {
                SVRTDirNode dirNode = (SVRTDirNode) node;
                List<SVRTNode> children = dirNode.getChildren();
                queue.addAll(children);
            }
        }

//        System.out.println(">>>>>>>>>>>>>>>>遍历R-Tree2>>>>>>>>>>>>>>>>>>>>>>");
//        List<RTNode> list = tree.traversePostOrder(tree.getRoot());
//        for (RTNode node:list){
//            System.out.println(node.getLevel());
//
//            Rectangle[] datas = node.getDatas();
//            for(Rectangle data:datas){
//                if (data!=null){
//                    System.out.println(data.toString());
//                }
//
//            }
//        }



    }



}