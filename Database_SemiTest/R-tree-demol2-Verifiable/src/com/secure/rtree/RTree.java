package com.secure.rtree;

import com.secure.alg.BVLSQ;
import com.secure.svrtree.SVRTree;
import org.apache.commons.collections.CollectionUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

/**
 * @ClassName RTree
 * @Description
 */
public class RTree implements Serializable
{
    /**
     * 根节点
     */
    private RTNode root;

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

    public RTree(int capacity, float fillFactor, int type, int dimension)
    {
        this.fillFactor = fillFactor;
        tree_type = type;
        nodeCapacity = capacity;
        this.dimension = dimension;
        root = new RTDataNode(this, Constants.NULL);
    }

    /**
     * @return RTree的维度
     */
    public int getDimension()
    {
        return dimension;
    }

    public void setRoot(RTNode root)
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

    /**
     * @return 返回树的类型
     */
    public int getTreeType()
    {
        return tree_type;
    }

    public RTNode getRoot() {
        return root;
    }

    public int getTree_type() {
        return tree_type;
    }

    public void setTree_type(int tree_type) {
        this.tree_type = tree_type;
    }

    public void setNodeCapacity(int nodeCapacity) {
        this.nodeCapacity = nodeCapacity;
    }

    public void setFillFactor(float fillFactor) {
        this.fillFactor = fillFactor;
    }

    public void setDimension(int dimension) {
        this.dimension = dimension;
    }

    /**
     * 向Rtree中插入Rectangle<p>
     * 1、先找到合适的叶节点 <br>
     * 2、再向此叶节点中插入<br>
     * @param rectangle
     */
    public boolean insert(Rectangle rectangle)
    {
        if(rectangle == null)
            throw new IllegalArgumentException("Rectangle cannot be null.");

        if(rectangle.getHigh().getDimension() != getDimension())
        {
            throw new IllegalArgumentException("Rectangle dimension different than RTree dimension.");
        }

        RTDataNode leaf = root.chooseLeaf(rectangle);

        return leaf.insert(rectangle);
    }

    /**
     * 从R树中删除Rectangle <p>
     * 1、寻找包含记录的结点--调用算法findLeaf()来定位包含此记录的叶子结点L，如果没有找到则算法终止。<br>
     * 2、删除记录--将找到的叶子结点L中的此记录删除<br>
     * 3、调用算法condenseTree<br>
     * @param rectangle
     * @return
     */
    public int delete(Rectangle rectangle)
    {
        if(rectangle == null)
        {
            throw new IllegalArgumentException("Rectangle cannot be null.");
        }

        if(rectangle.getHigh().getDimension() != getDimension())
        {
            throw new IllegalArgumentException("Rectangle dimension different than RTree dimension.");
        }

        RTDataNode leaf = root.findLeaf(rectangle);

        if(leaf != null)
        {
            return leaf.delete(rectangle);
        }

        return -1;
    }

    /**
     * 从给定的结点root开始遍历所有的结点
     * @param
     * @return 所有遍历的结点集合
     */
    public List<RTNode> traversePostOrder(RTNode root)
    {
        if(root == null)
            throw new IllegalArgumentException("Node cannot be null.");

        List<RTNode> list = new ArrayList<RTNode>();
        list.add(root);

        if(! root.isLeaf())
        {
            for(int i = 0; i < root.usedSpace; i ++)
            {
                List<RTNode> a = traversePostOrder(((RTDirNode)root).getChild(i));
                for(int j = 0; j < a.size(); j ++)
                {
                    list.add(a.get(j));
                }
            }
        }

        return list;
    }

    public static RTree createRTree(double[] data, int capacity, int dim) {
        RTree tree = new RTree(capacity, 0.4f, Constants.RTREE_QUADRATIC, dim);

        for (int i = 0; i < data.length; ) {
            double[] dt = new double[dim];
            double[] sp = new double[2];

            for (int j = 0; j < dim + 2; j++) {
                if (j < 2) {
                    sp[j] = data[i++];
                } else {
                    dt[j - 2] = data[i++];
                }
            }

            Point p1 = new Point(dt, sp);
            final Rectangle rectangle = new Rectangle(p1, p1);
            tree.insert(rectangle);
        }
        //补充location
        List<RTNode> list = tree.traversePostOrder(tree.getRoot());
        int highLevel = tree.getRoot().getLevel();
        List<ArrayList<RTNode>> listWithLevel = new ArrayList<>();
        for (int h = 0; h < highLevel + 1; h++) {
            listWithLevel.add(new ArrayList<RTNode>());
        }

        for (RTNode node : list) {
            for (int h = 0; h < highLevel + 1; h++) {
                if (node.getLevel() == h)
                    listWithLevel.get(h).add(node);
            }
        }
        for (int L = 1; L < highLevel + 1; L++) {
            for (int i = 0; i < listWithLevel.get(L).size(); i++) {
                RTDirNode parent_node = (RTDirNode) listWithLevel.get(L).get(i);
                List<RTNode> children_node = parent_node.getChildren();
                Rectangle[] children_rect = parent_node.getDatas();
                for (int j = 0; j < children_node.size(); j++) {
                    Rectangle[] set = children_node.get(j).getDatas();
                    double low_x = Double.MAX_VALUE;
                    double low_y = Double.MAX_VALUE;
                    double high_x = Double.MIN_VALUE;
                    double high_y = Double.MIN_VALUE;
                    for (int n = 0; n < set.length; n++) {
                        if (set[n] != null) {
                            if (set[n].getLow().getLocation()[0] < low_x)
                                low_x = set[n].getLow().getLocation()[0];
                            if (set[n].getLow().getLocation()[1] < low_y)
                                low_y = set[n].getLow().getLocation()[1];

                            if (set[n].getHigh().getLocation()[0] > high_x)
                                high_x = set[n].getHigh().getLocation()[0];
                            if (set[n].getHigh().getLocation()[1] > high_y)
                                high_y = set[n].getHigh().getLocation()[1];
                        }
                    }
                    children_rect[j].getLowWithoutClone().setLocation(low_x, low_y);
                    children_rect[j].getHigh().setLocation(high_x, high_y);
                }
            }
        }
        return tree;
    }
        public static void main(String[] args) throws Exception
    {
		RTree tree = new RTree(3, 0.4f, Constants.RTREE_QUADRATIC, 2);

//		float[] f = {10, 20, 40, 70,	//1
//			     30, 10, 70, 15,
//			     100, 70, 110, 80,		//3
//			     0, 50, 30, 55,
//			     13, 21, 54, 78,		//5
//			     3, 8, 23, 34,
//			     200, 29, 202, 50,
//			     34, 1, 35, 1,			//8
//			     201, 200, 234, 203,
//			     56, 69, 58, 70,		//10
//			     2, 67, 270, 102,
//			     1, 10, 310, 20,		//12
//			     23, 12, 345, 120,
//			     5, 34, 100, 340,
//			     19,100,450,560,	//15
//			     12,340,560,450,
//			     34,45,190,590,
//			     24,47,770,450,	//18
//
//			     91,99,390,980,
//			     89,10,99,100,	//20
//			     10,29,400,990,
//			     110,220,220,330,
//			     123,24,234,999	//23
//		};

//        float[] f = {1,9,2,10,
//                4,8,6,7,
//                9,10,7,5,
//                5,6,4,3,
//                3,2,9,1,
//                10,4,6,2,
//                8,3,8,4
//        };
		//插入结点
//		for(int i = 0; i < f.length;)
//		{
//			Point p1 = new Point(new double[]{f[i++],f[i++]});
//			final Rectangle rectangle = new Rectangle(p1, p1);
//			tree.insert(rectangle);
//
//			Rectangle[] rectangles = tree.root.datas;
//			System.out.println(tree.root.level);
//			for(int j = 0; j < rectangles.length; j ++)
//				System.out.println(rectangles[j]);
//		}
        String path = "D:\\idea\\SVLSQ\\data_test\\test_2_12.txt";
        double[] f = new double[0];
        String filename = path.substring(0,path.lastIndexOf("."));
        String[] split = filename.split("\\\\");
        String [] p =split[split.length-1].split("_");
        String typeName = p[0];
        Integer num = Integer.valueOf(p[p.length - 1]);
        Integer dim = Integer.valueOf(p[p.length - 2]);

        try {
            f = BVLSQ.readFile(path);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for(int i = 0; i < f.length;)
        {
            double[] dt = new double[dim];
            double[] sp = new double[2];

            for (int j =0;j<dim+2;j++){
                if (j<2){
                    sp[j]=f[i++];
                }else{
                    dt[j-2]=f[i++];
                }
            }

            Point p1 = new Point(dt,sp);
            final Rectangle rectangle = new Rectangle(p1, p1);
            tree.insert(rectangle);
        }



        System.out.println(">>>>>>>>>>>>>>>>遍历R-Tree>>>>>>>>>>>>>>>>>>>>>>");
        Queue<RTNode> queue = new LinkedList<>();

        queue.offer(tree.root);

        while (!queue.isEmpty()) {
            RTNode node = queue.poll();
            Rectangle[] rectangles = node.datas;
            System.out.println(node.level);
            for (int j = 0; j < rectangles.length; j++)
                System.out.println(rectangles[j]);
            if(node.getClass().equals(RTDirNode.class)) {
                RTDirNode dirNode = (RTDirNode) node;
                List<RTNode> children = dirNode.getChildren();
                queue.addAll(children);
            }
        }

        System.out.println(">>>>>>>>>>>>>>>>遍历R-Tree2>>>>>>>>>>>>>>>>>>>>>>");
        List<RTNode> list = tree.traversePostOrder(tree.root);


//        List<RTNode> newList = new ArrayList<>();
//        CollectionUtils.addAll(newList, new Object[list.size()]);
//        Collections.copy(newList, list);

        int highLevel = tree.root.getLevel();
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
        //打印结点
        for (RTNode node:list){
            System.out.println(node.getLevel());

            Rectangle[] datas = node.getDatas();
            for(Rectangle data:datas){
                if (data!=null){
                    System.out.println(data.toString());
                }
            }
        }

		System.out.println("---------------------------------");
//		System.out.println("Insert finished.");
//
//		//删除结点
//		System.out.println("---------------------------------");
//		System.out.println("Begin delete.");
//
//		for(int i = 0; i < f.length;)
//		{
//			Point p1 = new Point(new float[]{f[i++],f[i++]});
//			Point p2 = new Point(new float[]{f[i++],f[i++]});
//			final Rectangle rectangle = new Rectangle(p1, p2);
//			tree.delete(rectangle);
//
//			Rectangle[] rectangles = tree.root.datas;
//			System.out.println(tree.root.level);
//			for(int j = 0; j < rectangles.length; j ++)
//				System.out.println(rectangles[j]);
//		}
//
//		System.out.println("---------------------------------");
//		System.out.println("Delete finished.");
//
//
//		Rectangle[] rectangles = tree.root.datas;
//		for(int i = 0; i < rectangles.length; i ++)
//			System.out.println(rectangles[i]);


//        System.out.println("---------------------------------");
//        RTree tree = new RTree(5, 0.4f, Constants.RTREE_QUADRATIC,2);
//        BufferedReader reader = new BufferedReader(new FileReader(new File("d:\\LB.txt")));
//        String line ;
//        while((line = reader.readLine()) != null)
//        {
//            String[] splits = line.split(" ");
//            float lx = Float.parseFloat(splits[1]);
//            float ly = Float.parseFloat(splits[2]);
//            float hx = Float.parseFloat(splits[3]);
//            float hy = Float.parseFloat(splits[4]);
//
//            Point p1 = new Point(new float[]{lx,ly});
//            Point p2 = new Point(new float[]{hx,hy});
//
//            final Rectangle rectangle = new Rectangle(p1, p2);
//            tree.insert(rectangle);
//
//            Rectangle[] rectangles = tree.root.datas;
//            System.out.println(tree.root.level);
//            for(int j = 0; j < rectangles.length; j ++)
//                System.out.println(rectangles[j]);
//        }
//
//
//        //删除结点
//        System.out.println("---------------------------------");
//        System.out.println("Begin delete.");
//
//        reader = new BufferedReader(new FileReader(new File("d:\\LB.txt")));
//        while((line = reader.readLine()) != null)
//        {
//            String[] splits = line.split(" ");
//            float lx = Float.parseFloat(splits[1]);
//            float ly = Float.parseFloat(splits[2]);
//            float hx = Float.parseFloat(splits[3]);
//            float hy = Float.parseFloat(splits[4]);
//
//            Point p1 = new Point(new float[]{lx,ly});
//            Point p2 = new Point(new float[]{hx,hy});
//
//            final Rectangle rectangle = new Rectangle(p1, p2);
//            tree.delete(rectangle);
//
//            Rectangle[] rectangles = tree.root.datas;
//            System.out.println(tree.root.level);
//            for(int j = 0; j < rectangles.length; j ++)
//                System.out.println(rectangles[j]);
//        }
//
//        System.out.println("---------------------------------");
//        System.out.println("Delete finished.");

    }

}