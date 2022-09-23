package com.secure.alg;

import com.secure.rtree.*;
import com.secure.util.Paillier;

import java.util.*;

public class LSQ {
    public LSQ() {
    }

    private double getNoSquareDist(Rectangle rectangle, Point query) {
        double [] sloc_low = rectangle.getLow().getLocation();
        double [] sloc_high = rectangle.getHigh().getLocation();

        double square_dis = 0;
        //x\<=y,返回[1];x>y, 返回[0]

        /**
         * 比较两个密文的大小
         * @param x
         * @param y
         * @return x\<=y,返回[1];x>y, 返回[0]
         */
        double low0_flag = 0;// Paillier.Decryption(Paillier.IntegerComp(sloc_low[0],sQuery.getSdata()[0])).doubleValue();
        double high0_flag = 0;// Paillier.Decryption(Paillier.IntegerComp(sQuery.getSdata()[0],sloc_high[0])).doubleValue();

        if (sloc_low[0]<=query.getData()[0]){
            low0_flag = 1;
        }

        if (query.getData()[0]<=sloc_high[0]){
            high0_flag = 1;
        }

        double low1_flag = 0;// Paillier.Decryption(Paillier.IntegerComp(sloc_low[1],sQuery.getSdata()[1])).doubleValue();
        double high1_flag = 0;// Paillier.Decryption(Paillier.IntegerComp(sQuery.getSdata()[1],sloc_high[1])).doubleValue();

        if (sloc_low[1]<=query.getData()[1]){
            low1_flag = 1;
        }

        if (query.getData()[1]<=sloc_high[1]){
            high1_flag = 1;
        }


        if (low0_flag==0&&low1_flag==0){
//            BigInteger df0= sloc_low[0].multiply(sQuery.getSdata()[0].modPow(Paillier.n.subtract(new BigInteger("1")),Paillier.nsquare));
//            BigInteger df1= sloc_low[1].multiply(sQuery.getSdata()[1].modPow(Paillier.n.subtract(new BigInteger("1")),Paillier.nsquare));
//            square_dis = Paillier.SM(df0,df0).multiply(Paillier.SM(df1,df1).mod(Paillier.nsquare));

            square_dis = Math.pow(query.getData()[0]-sloc_low[0],2)+
                    Math.pow(query.getData()[1]-sloc_low[1],2);

        }else if (low0_flag==0&&low1_flag==1&&high1_flag==1){
//            BigInteger df0= sloc_low[0].multiply(sQuery.getSdata()[0].modPow(Paillier.n.subtract(new BigInteger("1")),Paillier.nsquare));
//            square_dis = Paillier.SM(df0,df0).mod(Paillier.nsquare);

            square_dis = Math.pow(query.getData()[0]-sloc_low[0],2);

        }else if (low0_flag==0&&high1_flag==0){
//            BigInteger df0= sloc_low[0].multiply(sQuery.getSdata()[0].modPow(Paillier.n.subtract(new BigInteger("1")),Paillier.nsquare));
//            BigInteger df1= sloc_high[1].multiply(sQuery.getSdata()[1].modPow(Paillier.n.subtract(new BigInteger("1")),Paillier.nsquare));
//            square_dis = Paillier.SM(df0,df0).multiply(Paillier.SM(df1,df1).mod(Paillier.nsquare));

            square_dis = Math.pow(query.getData()[0]-sloc_low[0],2)+
                    Math.pow(query.getData()[1]-sloc_high[1],2);

        }else if (low0_flag==1&&high0_flag==1&&high1_flag==0){
//            BigInteger df0= sloc_high[1].multiply(sQuery.getSdata()[1].modPow(Paillier.n.subtract(new BigInteger("1")),Paillier.nsquare));
//            square_dis = Paillier.SM(df0,df0).mod(Paillier.nsquare);
            square_dis =
                    Math.pow(query.getData()[1]-sloc_high[1],2);

        }else if (high0_flag==0&&high1_flag==0){
//            BigInteger df0= sloc_high[0].multiply(sQuery.getSdata()[0].modPow(Paillier.n.subtract(new BigInteger("1")),Paillier.nsquare));
//            BigInteger df1= sloc_high[1].multiply(sQuery.getSdata()[1].modPow(Paillier.n.subtract(new BigInteger("1")),Paillier.nsquare));
//            square_dis = Paillier.SM(df0,df0).multiply(Paillier.SM(df1,df1).mod(Paillier.nsquare));
            square_dis = Math.pow(query.getData()[0]-sloc_high[0],2)+
                    Math.pow(query.getData()[1]-sloc_high[1],2);
        }else if (high0_flag==0&&low1_flag==1&&high1_flag==1){
//            BigInteger df0= sloc_high[0].multiply(sQuery.getSdata()[0].modPow(Paillier.n.subtract(new BigInteger("1")),Paillier.nsquare));
//            square_dis = Paillier.SM(df0,df0).mod(Paillier.nsquare);
            square_dis = Math.pow(query.getData()[0]-sloc_high[0],2);

        }else if (high0_flag==0&&low1_flag==0){
//            BigInteger df0= sloc_high[0].multiply(sQuery.getSdata()[0].modPow(Paillier.n.subtract(new BigInteger("1")),Paillier.nsquare));
//            BigInteger df1= sloc_low[1].multiply(sQuery.getSdata()[1].modPow(Paillier.n.subtract(new BigInteger("1")),Paillier.nsquare));
//            square_dis = Paillier.SM(df0,df0).multiply(Paillier.SM(df1,df1).mod(Paillier.nsquare));
            square_dis = Math.pow(query.getData()[0]-sloc_high[0],2)+
                    Math.pow(query.getData()[1]-sloc_low[1],2);

        }else if (low0_flag==1&&high0_flag==1&&low1_flag==0){
//            BigInteger df0= sloc_low[1].multiply(sQuery.getSdata()[1].modPow(Paillier.n.subtract(new BigInteger("1")),Paillier.nsquare));
//            square_dis = Paillier.SM(df0,df0).mod(Paillier.nsquare);
            square_dis =
                    Math.pow(query.getData()[1]-sloc_low[1],2);
        }else{
            square_dis = 0;
        }
        return Math.sqrt(square_dis);
    }

    private double distWithMiwen(Rectangle rectangle,Point query){
        double NoSquare_dis= 0;
        if (rectangle.getDis()==-1){
            NoSquare_dis = getNoSquareDist(rectangle,query);
            rectangle.setDis(NoSquare_dis);
        }else{
            NoSquare_dis = rectangle.getDis();
        }

        for (double sp:rectangle.getLow().getData()){
            NoSquare_dis = NoSquare_dis+sp;
        }
        return NoSquare_dis;
    }

    public RectAndNode sortSRectWithPlaintext(Stack<RTNode> node_queue, Stack<Rectangle> queue,Point query){
        Stack<Rectangle> headMinQueue = new Stack<>();
        Stack<RTNode> headMinNodeQueue = new Stack<>();

        Rectangle sminsle = queue.pop();
        RTNode sminNode = node_queue.pop();

        double mindist = distWithMiwen(sminsle,query);

        while (!queue.isEmpty()) {
            Rectangle sle = queue.pop();
            RTNode nde = node_queue.pop();

            double spdist = distWithMiwen(sle,query);

            if (mindist<spdist){
                headMinQueue.push(sle);
                headMinNodeQueue.push(nde);
            }else{
                headMinQueue.push(sminsle);
                headMinNodeQueue.push(sminNode);

                sminsle = sle;
                sminNode = nde;
                mindist = spdist;
            }
        }
        headMinQueue.push(sminsle);
        headMinNodeQueue.push(sminNode);
        return new RectAndNode(headMinQueue,headMinNodeQueue);
    }

    private double getNewDist(Rectangle rectangle, Point Q) {
        double dist = 0;

        double[] sloc_low = rectangle.getLow().getLocation();
        double[] sloc_high = rectangle.getHigh().getLocation();

//        double[] Q = new ArrayList<>();
//        Q.add(sQ.getData()[0]);
//        Q.add(sQ.getData()[1]);

        if (sloc_low[0]==(sloc_high[0])&&sloc_low[1]==(sloc_high[1])){
            double df0= sloc_low[0]-(Q.getData()[0]);
            double df1= sloc_low[1]-(Q.getData()[1]);
            dist = Math.sqrt(Math.pow(df0,2)+Math.pow(df1,2));
            return dist;
        }else{
            //x\<=y,返回[1];x>y, 返回[0]
            double low0_flag;
            double high0_flag;
            double low1_flag;
            double high1_flag;
            if (sloc_low[0]>(Q.getData()[0])){
                low0_flag = 0;
            }else{
                low0_flag = 1;
            }

            if (Q.getData()[0]>(sloc_high[0])){
                high0_flag = 0;
            }else{
                high0_flag = 1;
            }

            if (sloc_low[1]>(Q.getData()[1])){
                low1_flag = 0;
            }else{
                low1_flag = 1;
            }
            if (Q.getData()[1]>(sloc_high[1])){
                high1_flag = 0;
            }else{
                high1_flag = 1;
            }


            if (low0_flag==0&&low1_flag==0){
                double df0= sloc_low[0]-(Q.getData()[0]);
                double df1= sloc_low[1]-(Q.getData()[1]);
                dist = Math.sqrt(Math.pow(df0,2)+Math.pow(df1,2));
            }else if (low0_flag==0&&low1_flag==1&&high1_flag==1){
                double df0= sloc_low[0]-(Q.getData()[0]);
                dist = df0;
            }else if (low0_flag==0&&high1_flag==0){
                double df0= sloc_low[0]-(Q.getData()[0]);
                double df1= sloc_high[1]-(Q.getData()[1]);
                dist = Math.sqrt(Math.pow(df0,2)+Math.pow(df1,2));
            }else if (low0_flag==1&&high0_flag==1&&high1_flag==0){
                double df0= Q.getData()[1]-(sloc_high[1]);
                dist = df0;
            }else if (high0_flag==0&&high1_flag==0){
                double df0= sloc_high[0]-(Q.getData()[0]);
                double df1= sloc_high[1]-(Q.getData()[1]);
                dist = Math.sqrt(Math.pow(df0,2)+Math.pow(df1,2));
            }else if (high0_flag==0&&low1_flag==1&&high1_flag==1){
                double df0= Q.getData()[0]-(sloc_high[0]);
                dist = df0;
            }else if (high0_flag==0&&low1_flag==0){
                double df0= sloc_high[0]-(Q.getData()[0]);
                double df1= sloc_low[1]-(Q.getData()[1]);
                dist = Math.sqrt(Math.pow(df0,2)+Math.pow(df1,2));
            }else if (low0_flag==1&&high0_flag==1&&low1_flag==0){
                double df0= sloc_low[1]-(Q.getData()[1]);
                dist = df0;
            }else{
                dist = 0;
            }
            return dist;
        }

    }

    private boolean isDominatedInSetWithPlaintext(Rectangle rg,Point query, List<Point> entries){

        boolean flag = false;
        int count;
        double[] cdata = rg.getLow().getData();
        double dis = 0;
        if (rg.getDis()!=-1){
            dis = rg.getDis();
        }else{
         dis = getNewDist(rg,query);
        }

        for (Point sPoint:entries){
            count = 0;
            for (int i =0;i<cdata.length;i++){
                if (cdata[i]>sPoint.getData()[i]){
                    count++;
                }else if (cdata[i]<sPoint.getData()[i]){
                    count = count -cdata.length-2;
                }
            }
            double dis_p = sPoint.getDist();
            if (dis>dis_p){
                count++;
            }else if(dis<dis_p){
                count = count -cdata.length-2;
            }

            if (count>0){
                flag = true;
                return flag;
            }

        }
        return flag;

    }

    public List<Point> execute(RTree RT,Point query){
        List<Point> res = new LinkedList<>();
        Stack<Rectangle> queue = new Stack<>();
        Stack<RTNode> node_queue = new Stack<>();

        RTDirNode root = (RTDirNode) RT.getRoot();
        node_queue.addAll(root.getChildren());
        for (int i = 0; i<root.getUsedSpace();i++){
            queue.add(root.getDatas()[i]);
        }

        while(!queue.isEmpty()){
            RectAndNode rectAndNode= sortSRectWithPlaintext(node_queue,queue,query);
            node_queue = rectAndNode.getNode();
            queue = rectAndNode.getRect();
            Rectangle sRg = queue.pop();
            RTNode node = node_queue.pop();

            //测试
//            if(sRg.getLow().getLocation()[0]==6&&sRg.getLow().getLocation()[1]==1){
//                System.out.println("找到第7/6层！！！！！！！");
//            }
//            if(sRg.getLow().getLocation()[0]==10&&sRg.getLow().getLocation()[1]==1){
//                System.out.println("找到第5层！！！！！！！");
//            }
//            if(sRg.getLow().getLocation()[0]==67&&sRg.getLow().getLocation()[1]==2){
//                System.out.println("找到第4层！！！！！！！");
//            }
//            if(sRg.getLow().getLocation()[0]==420&&sRg.getLow().getLocation()[1]==2){
//                System.out.println("找到第3层！！！！！！！");
//            }
//            if(sRg.getLow().getLocation()[0]==420&&sRg.getLow().getLocation()[1]==536){
//                System.out.println("找到第2/1层！！！！！！！");
//            }
//            if(sRg.getLow().getLocation()[0]==420&&sRg.getLow().getLocation()[1]==732){
//                System.out.println("找到第0层！！！！！！！");
//            }

            boolean dom = isDominatedInSetWithPlaintext(sRg,query,res);
            if (!dom){

                if(node.getDatas()!=null) {

                    if(node.getClass().equals(RTDirNode.class)) {
                        RTDirNode dirNode = (RTDirNode) node;
                        List<RTNode> children = dirNode.getChildren();
                        Rectangle [] rtgs = dirNode.getDatas();

                        for (int k = 0;  k < children.size();k++){
                            if (!isDominatedInSetWithPlaintext(rtgs[k],query,res)){
                                queue.add(rtgs[k]);
                                node_queue.add(children.get(k));
                            }
                        }
                    }else{
                        Rectangle [] rtgs = node.getDatas();

                        for (int k = 0;  k < node.getUsedSpace();k++){
                            if (!isDominatedInSetWithPlaintext(rtgs[k],query,res)){
                                queue.add(rtgs[k]);
                                node_queue.add(new RTDirNode());
                            }
                        }
                    }


                }else{
                    double df0= sRg.getLow().getLocation()[0]-(query.getData()[0]);
                    double df1= sRg.getLow().getLocation()[1]-(query.getData()[1]);
                    Point p = sRg.getLow();
                    p.setDist(Math.sqrt(Math.pow(df0,2)+(Math.pow(df1,2))));
                    res.add(p);
                }
            }
        }

        return res;
    }
    public  void showTree (RTree tree){
        Queue<RTNode> queue = new LinkedList<>();

        queue.offer(tree.getRoot());

        while (!queue.isEmpty()) {
            RTNode node = queue.poll();
            Rectangle[] rectangles = node.getDatas();
            System.out.println(node.getLevel());
            for (int j = 0; j < rectangles.length; j++)
                System.out.println(rectangles[j]);
            if(node.getClass().equals(RTDirNode.class)) {
                RTDirNode dirNode = (RTDirNode) node;
                List<RTNode> children = dirNode.getChildren();
                queue.addAll(children);
            }
        }
    }

    public static void main(String[] args) {
        double[] f = {1,9,2,10,
                4,8,6,7,
                9,10,7,5,
                5,6,4,3,
                3,2,9,1,
                10,4,6,2,
                8,3,8,4
        };
        RTree tree = RTree.createRTree(f,3,2);

        System.out.println(">>>>>>>>>>>>>>>>遍历R-Tree>>>>>>>>>>>>>>>>>>>>>>");
        Queue<RTNode> queue = new LinkedList<>();

        queue.offer(tree.getRoot());

        while (!queue.isEmpty()) {
            RTNode node = queue.poll();
            Rectangle[] rectangles = node.getDatas();
            System.out.println(node.getLevel());
            for (int j = 0; j < rectangles.length; j++)
                System.out.println(rectangles[j]);
            if(node.getClass().equals(RTDirNode.class)) {
                RTDirNode dirNode = (RTDirNode) node;
                List<RTNode> children = dirNode.getChildren();
                queue.addAll(children);
            }
        }

        System.out.println(">>>>>>>>>>>>>>>>遍历R-Tree2>>>>>>>>>>>>>>>>>>>>>>");
        List<RTNode> list = tree.traversePostOrder(tree.getRoot());


//        List<RTNode> newList = new ArrayList<>();
//        CollectionUtils.addAll(newList, new Object[list.size()]);
//        Collections.copy(newList, list);

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

    }

}
