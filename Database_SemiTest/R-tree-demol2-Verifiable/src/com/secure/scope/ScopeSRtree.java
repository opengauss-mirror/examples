package com.secure.scope;

import com.secure.rtree.*;

import com.secure.ssvrtree.*;
import com.secure.svrtree.SPoint;
import com.secure.util.Digest;
import com.secure.util.Paillier;
import com.secure.util.ReadData;
import com.secure.util.Signat;
import com.secure.voronoi.Point;
import com.secure.voronoi.Vertex;
import com.secure.voronoi.VoronoiPoint;
import com.secure.vsptree.VSPRTDirNode;
import com.secure.vsptree.VSPRTNode;
import com.secure.vsptree.VSPRTree;
import com.secure.vsptree.VSPRectangle;
import org.apache.commons.collections.CollectionUtils;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class ScopeSRtree {

//    public static double enlarger = 1000.0;
    public static double enlarger = 1;

    public boolean noSpatialDM(List<BigInteger> a, List<BigInteger> b){
        boolean flag = true;
        for (int i =2; i<a.size();i++){
            if (a.get(i).compareTo(b.get(i))==1){
                flag =false;
                break;
            }
        }
        return flag;
    }

    public List<ArrayList<BigInteger>> DomSet(List<BigInteger> cell,List<ArrayList<BigInteger>> D){
        List<ArrayList<BigInteger>> Dom = new ArrayList<>();
        for (int i = 0; i<D.size();i++){
            boolean flag = true;
            if (noSpatialDM(D.get(i),cell)){
//                for (int j = 0;j<Dom.size();j++){
//                    int count = 0;
//                    for (int k = 0;k<D.get(i).size();k++){
//                        if (D.get(i).get(k).compareTo(Dom.get(j).get(k))==0){
//                            count= count+1;
//                        }
//                    }
//                    if (count==D.get(i).size()){
//                        flag = false;
//                        break;
//                    }
//                }
//                if (flag){
                    Dom.add(D.get(i));
//                }

            }
        }
        return Dom;
    }
    boolean isSame(ArrayList<Point> Dom,Point pt){
        boolean flag = true;
        for (int j = 0;j<Dom.size();j++){
            int count = 0;

            if (pt.getX()==Dom.get(j).getX()){
                    count= count+1;
            }
            if (pt.getY()==Dom.get(j).getY()){
                count= count+1;
            }

            if (count==2){
                flag = false;
                break;
            }
        }
        return flag;
    }

    public List<CellAndScope> generateScopeWithCell(List<ArrayList<BigInteger>> D){
        int DomSize = 0;
        List<CellAndScope> mapSets = new ArrayList<>();
        for (int i = 0;i<D.size();i++){
            ArrayList<BigInteger> cell = D.get(i);
            if(new BigInteger("119").equals(cell.get(0))||new BigInteger("798").equals(cell.get(0))){
                System.out.println("找到了！！！");
            }
            D.remove(i);
            List<ArrayList<BigInteger>> Dom = DomSet(cell,D);
            DomSize= DomSize+Dom.size();
            D.add(i,cell);
            VoronoiPoint VP = new VoronoiPoint();


            ArrayList<Point> pointList = new ArrayList<Point>();
            List<ArrayList<Double>> list = new ArrayList<>();
           if (!Dom.isEmpty()){
               for (int h = 0; h<Dom.size();h++){
                   //check一下是否有平行的
                   boolean flag_Parl = false;
                   for (int m=0;m<pointList.size();m++){
                       if (if_intersect_np(Dom.get(h).get(0).doubleValue(),-Dom.get(h).get(1).doubleValue(),
                               cell.get(0).doubleValue(),-cell.get(1).doubleValue(),
                               pointList.get(m).getX(),pointList.get(m).getY(),
                               cell.get(0).doubleValue(),-cell.get(1).doubleValue())==0){
                        double dis1 = getDistance(Dom.get(h).get(0).doubleValue(),-Dom.get(h).get(1).doubleValue(),
                                cell.get(0).doubleValue(),-cell.get(1).doubleValue());
                        double dis2 = getDistance( pointList.get(m).getX(),pointList.get(m).getY(),
                                   cell.get(0).doubleValue(),-cell.get(1).doubleValue());
                        if (dis2>dis1){
                            Point pt = new Point(Dom.get(h).get(0).doubleValue(),-Dom.get(h).get(1).doubleValue());
                            if (isSame(pointList,pt))
                                pointList.set(m,pt);
                        }
                           flag_Parl =true;
                       }
                   }
                   if (flag_Parl==false){
                       Point pt= new Point(Dom.get(h).get(0).doubleValue(),-Dom.get(h).get(1).doubleValue());
                       if (isSame(pointList,pt))
                           pointList.add(pt);
                   }
               }
               Point pt = new Point(cell.get(0).doubleValue(),-cell.get(1).doubleValue());
               if (isSame(pointList,pt))
                    pointList.add(pt);




               ArrayList<Vertex> chongFuVertexAL =
                       VP.generateCellWithPolygons(new Point(cell.get(0).doubleValue(),-cell.get(1).doubleValue()),
                               pointList);
//               去重
               ArrayList<Vertex> vertexAL =new ArrayList<>();
               for (Vertex cfvt:chongFuVertexAL){
                   boolean fg = true;
                   for (Vertex vt:vertexAL){
                        if (cfvt.getCoordinate().getX()==vt.getCoordinate().getX()&&
                                cfvt.getCoordinate().getY()==vt.getCoordinate().getY())
                            fg = false;
                   }
                   if (fg)
                       vertexAL.add(cfvt);
                   fg = true;
               }

               for (Vertex v:vertexAL){
                   List<Double> point = new ArrayList<>();
                   double x = Math.round(v.getCoordinate().getX()*enlarger);
                   double y = -Math.round(v.getCoordinate().getY()*enlarger);
                   point.add(x);
                   point.add(y);
                   list.add((ArrayList<Double>) point);
               }

           }
//            map.put(cell,list);
            CellAndScope cAs = new CellAndScope();
            cAs.setCell(cell);
            cAs.setScope(list);
            mapSets.add(cAs);
        }
        System.out.println("Dom的大小："+DomSize);
        return mapSets;
    }

    int if_intersect_np(double x1,double y1,double x2,double y2, double x3,double y3,double x4,double y4)
    {
//        double x;
//        x=((x1*y2-x2*y1)/(x2-x1)+(x4*y3-x3*y4)/(x4-x3))/((y2-y1)/(x2-x1)-(y4-y3)/(x4-x3));
//        if(((x1-x)*(x-x2)>=0)&&((x3-x)*(x-x4)>=0))
//            return 1;
//        else
//            return 0;
        double a = (y2-y1)/(x2-x1);
        double b = (y4-y3)/(x4-x3);
        if (a==b){
            return 0;
        }else{
            return 1;
        }
    }

    public double getDistance(double x1, double y1, double x2, double y2){
        double _x = Math.abs(x1 - x2);
        double _y = Math.abs(y1 - y2);
        return Math.sqrt(_x*_x+_y*_y);
    }
    public int checkDirection(BigInteger x1, BigInteger y1,BigInteger x2, BigInteger y2,
                              BigInteger x3, BigInteger y3) {

        BigInteger z1 = x1.subtract(x2).multiply(y3.subtract(y2));
        BigInteger z2 = y1.subtract(y2).multiply(x3.subtract(x2));
        BigInteger z = z1.subtract(z2);
        if (z.compareTo(new BigInteger("0"))==1) {
            return 1;
        } else if (z.compareTo(new BigInteger("0"))==0) {
            return 0;
        } else{
            return -1;
        }
    }
    public int checkDirection(double x1, double y1,double x2, double y2,
                              double x3, double y3) {
        double z = (x1 - x2) * (y3 - y2) - (y1 - y2) * (x3 - x2);
        if (z > 0) {
            return 1;
        } else if (z == 0) {
                return 0;
        } else{
            return -1;
        }
    }

    public int checkDirectionDecrytion(BigInteger xx1, BigInteger yy1,BigInteger xx2, BigInteger yy2,
                                       BigInteger xx3, BigInteger yy3) {

//        low00.compareTo(Paillier.n.divide(new BigInteger("2")))==1?low00.subtract(Paillier.n):low00;
        BigInteger ord_x1 = Paillier.Decryption(xx1);
        BigInteger ord_y1 = Paillier.Decryption(yy1);
        BigInteger ord_x2 = Paillier.Decryption(xx2);
        BigInteger ord_y2 = Paillier.Decryption(yy2);
        BigInteger ord_x3 = Paillier.Decryption(xx3);
        BigInteger ord_y3 = Paillier.Decryption(yy3);

        double x1 = ord_x1.compareTo(Paillier.n.divide(new BigInteger("2")))==1?ord_x1.subtract(Paillier.n).doubleValue():ord_x1.doubleValue();
        double y1 = ord_y1.compareTo(Paillier.n.divide(new BigInteger("2")))==1?ord_y1.subtract(Paillier.n).doubleValue():ord_y1.doubleValue();

        double x2 = ord_x2.compareTo(Paillier.n.divide(new BigInteger("2")))==1?ord_x2.subtract(Paillier.n).doubleValue():ord_x2.doubleValue();
        double y2 = ord_y2.compareTo(Paillier.n.divide(new BigInteger("2")))==1?ord_y2.subtract(Paillier.n).doubleValue():ord_y2.doubleValue();

        double x3 = ord_x3.compareTo(Paillier.n.divide(new BigInteger("2")))==1?ord_x3.subtract(Paillier.n).doubleValue():ord_x3.doubleValue();
        double y3 = ord_y3.compareTo(Paillier.n.divide(new BigInteger("2")))==1?ord_y3.subtract(Paillier.n).doubleValue():ord_y3.doubleValue();

        double a1 = (x1-x2)/enlarger;
        double a2 = (y1-y2)/enlarger;
        double b1 = (x3-x2)/enlarger;
        double b2 =(y3-y2)/enlarger;

        double z = a1 * b2 - a2 * b1;
        if (z > 0) {
            return 1;
        } else if (z == 0) {
            return 0;
        } else{
            return -1;
        }
    }

    private static List<SSVRTNode> cToS(List<RTNode> children, SSRectangle[] sRts, SSVRTree svrTree) {
        List<SSVRTNode> schildren = new ArrayList<SSVRTNode>();

        for (int i =0; i<children.size();i++){
            RTNode node  = children.get(i);

            Rectangle[] ds= node.getDatas();
            int level = node.getLevel();
            int usedSpace = node.getUsedSpace();
            SSRectangle[] sRectangles = new SSRectangle[usedSpace];
            for (int j = 0;j<usedSpace;j++){
                sRectangles[j] = new SSRectangle();
                sRectangles[j].setScopeAndHash(ds[j].getLow(),ds[j].getHigh());
            }
            SSVRTNode svrtNode = new SSVRTDirNode(svrTree,level, sRectangles, usedSpace);
            sRts[i].setChild(svrtNode);

            schildren.add(svrtNode);
        }

        return schildren;
    }

    public RTree createRTree(List<CellAndScope> scopeWithCell_List,int capacity){
        RTree tree = new RTree(capacity, 0.4f, Constants.RTREE_QUADRATIC, 2);
        for (int i = 0; i<scopeWithCell_List.size();i++)
        {
            List<BigInteger> cell = scopeWithCell_List.get(i).getCell();
            List<ArrayList<Double>> rect = scopeWithCell_List.get(i).getScope();

            final Rectangle rectangle = new Rectangle(cell,rect);
            tree.insert(rectangle);
        }
        return tree;
    }

    public SSVRTree createSSVRTree(RTree tree, int capacity){
        SSVRTree svrtree = new SSVRTree(capacity, 0.4f, Constants.RTREE_QUADRATIC, 2);
        Queue<RTNode> queue = new LinkedList<>();
        Queue<SSVRTNode> svqueue = new LinkedList<>();
        queue.offer(tree.getRoot());
        Rectangle[] ds = tree.getRoot().getDatas();
        int level = tree.getRoot().getLevel();
        int usedSpace = tree.getRoot().getUsedSpace();
        SSRectangle[] sRectangles = new SSRectangle[usedSpace];
        for (int i=0;i<usedSpace;i++){
            sRectangles[i] = new SSRectangle();
            sRectangles[i].setScopeAndHash(ds[i].getLow(),ds[i].getHigh());
        }
        SSVRTNode svrtNode = new SSVRTDirNode(svrtree,level, sRectangles, usedSpace);
        svrtree.setRoot(svrtNode);

        svqueue.offer(svrtree.root);

        while (!queue.isEmpty()) {
            RTNode node = queue.poll();
            SSVRTNode snode = svqueue.poll();

//            Rectangle[] rects = node.getDatas();
//            System.out.println(node.getLevel());
//            for (Rectangle rt:rects){
//
//                if (rt!=null){
//                    System.out.println(rt.getLow()+";"+rt.getHigh());
//                    if (rt.getCell()!=null){
//                        System.out.println("cell: "+rt.getCell());
//                        System.out.println("scope: "+rt.getScope());
//                    }
//                }
//            }

            if(node.getClass().equals(RTDirNode.class)) {
                RTDirNode dirNode = (RTDirNode) node;
                List<RTNode> children = dirNode.getChildren();
                List<SSVRTNode> schildren = cToS(children,snode.getDatas(),svrtree);

                ((SSVRTDirNode)snode).setChildren(schildren);

                svqueue.addAll(schildren);
                queue.addAll(children);
            }else{
                Rectangle[] uu = node.getDatas();
                SSRectangle[] ss = snode.getDatas();
                for (int k = 0; k<node.getUsedSpace();k++){

                    ss[k].setScopeAndScell(uu[k].getScope(),uu[k].getCell());
                }
            }
        }
        return svrtree;
    }

    public List<List<BigInteger>> execute(SSVRTree svr, SPoint sQuery){
        List<List<BigInteger>> res = new LinkedList<>();
        Stack<SSRectangle> queue = new Stack<>();

        for (SSRectangle sRectangle:svr.getRoot().getObjects()){
            if (SQPWithRect(sRectangle,sQuery)){
                queue.add(sRectangle);
            }
        }

        while(!queue.isEmpty()){
            SSRectangle sRg = queue.pop();
//            System.out.println("low:("+Paillier.Decryption(sRg.getScope().get(0).get(0))+","
//                    +Paillier.Decryption(sRg.getScope().get(0).get(1))+")");
//            System.out.println("high("+Paillier.Decryption(sRg.getScope().get(1).get(0))+","
//                    +Paillier.Decryption(sRg.getScope().get(1).get(1))+")");
//                System.out.println("左下角的点"+Paillier.Decryption(sRg.getLow().getSdata()[0])+","+
//                        Paillier.Decryption(sRg.getLow().getSdata()[1]));
            if (sRg.getChild()!=null){
                for (SSRectangle sRectangle: sRg.getChild().getObjects()){
                   if (sRectangle.getChild()!=null){
                       if (SQPWithRect(sRectangle,sQuery)){
                           queue.add(sRectangle);
                       }
                   }else{
//                       System.out.println("点"+Paillier.Decryption(sRectangle.getScell().get(2))+","+
//                               Paillier.Decryption(sRectangle.getScell().get(3)));
                       if (SQPWithScope(sRectangle,sQuery)){
                           queue.add(sRectangle);
                       }
                   }
                }
            }else{
                    res.add(sRg.getScell());
            }
        }

        return res;
    }

    public List<ArrayList<BigInteger>> executeWithVerification(SSVRTree svr, VSPRTree vsprTree, SPoint sQuery){
        List<ArrayList<BigInteger>> res = new LinkedList<>();
        Stack<SSRectangle> queue = new Stack<>();
        Stack<VSPRectangle> vQueue = new Stack<>();

        int vLevel = svr.getRoot().getLevel();
        int vUsedSpace = svr.getRoot().getUsedSpace();

        List<SSRectangle> objects = svr.getRoot().getObjects();
        VSPRectangle[] vds = new VSPRectangle[objects.size()];
        List<SSRectangle> list_srg =  svr.getRoot().getObjects();

        for (int i = 0; i<vUsedSpace; i++){
            VSPRectangle vspRect = new VSPRectangle();
            if (SQPWithRect(list_srg.get(i),sQuery)){
                queue.add(list_srg.get(i));
                vspRect.setScope(list_srg.get(i).getScope());
            }else{
                vspRect.setScope(list_srg.get(i).getScope());
                vspRect.setHashScope(list_srg.get(i).getHashScope());
            }
            vds[i] = vspRect;

//            System.out.println("low:("+Paillier.Decryption(vspRect.getScope().get(0).get(0))+","
//                    +Paillier.Decryption(vspRect.getScope().get(0).get(1))+")");
//            System.out.println("high("+Paillier.Decryption(vspRect.getScope().get(1).get(0))+","
//                    +Paillier.Decryption(vspRect.getScope().get(1).get(1))+")");
        }
        VSPRTNode vsprtNode = new VSPRTDirNode(vsprTree,vLevel,vds, vUsedSpace);
        vsprTree.setRoot(vsprtNode);

        for (VSPRectangle vspRectangle:vsprTree.getRoot().getObjects()){
            vQueue.add(vspRectangle);
        }

        while(!queue.isEmpty()){
            SSRectangle sRg = queue.pop();
            VSPRectangle vRg = vQueue.pop();

            if (sRg.getChild()!=null){

                SSVRTNode childSVRTNode = sRg.getChild();
                int vspLevel = childSVRTNode.getLevel();
                int vspUsedSpace = childSVRTNode.getUsedSpace();

                List<SSRectangle> childrenSrg = childSVRTNode.getObjects();

                VSPRectangle[] childrenVsprg = new VSPRectangle[childrenSrg.size()];

                for (int i = 0; i<childrenSrg.size();i++){
                    SSRectangle sRectangle = childrenSrg.get(i);
                    VSPRectangle vspRect = new VSPRectangle();
                    if (sRectangle.getChild()!=null){

                        vspRect.setScope(sRectangle.getScope());
                        if (SQPWithRect(sRectangle,sQuery)){
                            queue.add(sRectangle);
                            childrenVsprg[i] = vspRect;
                            vQueue.add(childrenVsprg[i]);
                        }else {
                            vspRect.setHashScope(sRectangle.getHashScope());
                            childrenVsprg[i] = vspRect;
                        }

                    }else{
                        if (SQPWithScope(sRectangle,sQuery)){
                            queue.add(sRectangle);
                            vspRect.setHashScope(sRectangle.getHashScope());
                            vspRect.setSigScopeWithCell(sRectangle.getSigScopeWithCell());
                            childrenVsprg[i] = vspRect;
                            vQueue.add(childrenVsprg[i]);
                        }else{
                            vspRect.setScope(sRectangle.getScope());
                            vspRect.setHashScope(sRectangle.getHashScope());
                            childrenVsprg[i] = vspRect;
                        }
                    }
                }
                VSPRTNode vspNode = new VSPRTDirNode(vsprTree,vspLevel,childrenVsprg, vspUsedSpace);
                vRg.setChild(vspNode);
            }else{
                res.add((ArrayList)sRg.getScell());
            }
        }

        return res;
    }

    private boolean SQPWithScope(SSRectangle sRectangle, SPoint sQuery) {
        boolean flag = true;
        List<ArrayList<BigInteger>> scope = sRectangle.getScope();

//        int y = checkDirectionWithCiphertext(scope.get(1).get(0),scope.get(1).get(1),
//                scope.get(0).get(0),scope.get(0).get(1),
//                sQuery.getSdata()[0],sQuery.getSdata()[1]);
        int y = checkDirectionDecrytion(scope.get(1).get(0),scope.get(1).get(1),
                scope.get(0).get(0),scope.get(0).get(1),
                sQuery.getSdata()[0],sQuery.getSdata()[1]);

        BigInteger cell0= Paillier.Decryption(sRectangle.getScell().get(0));
        BigInteger cell1= Paillier.Decryption(sRectangle.getScell().get(1));
        BigInteger cell2= Paillier.Decryption(sRectangle.getScell().get(2));
        //BigInteger cell3= Paillier.Decryption(sRectangle.getScell().get(3));

        for (int i = 1; i<scope.size();i++){
            if (y!=checkDirectionDecrytion(scope.get((i+1)%scope.size()).get(0),scope.get((i+1)%scope.size()).get(1),
                    scope.get(i).get(0),scope.get(i).get(1),
                    sQuery.getSdata()[0],sQuery.getSdata()[1])){
                flag = false;
                break;
            }
        }

//        int yy = checkDirection(Paillier.Decryption(scope.get(0).get(0)),Paillier.Decryption(scope.get(0).get(1)),
//                Paillier.Decryption(scope.get(scope.size()-1).get(0)),Paillier.Decryption(scope.get(scope.size()-1).get(1)),
//                sQuery.getCdata()[0],sQuery.getCdata()[1]);
//        for (int i = 0; i<scope.size()-1;i++){
//            if (yy!=checkDirection(Paillier.Decryption(scope.get(i+1).get(0)),Paillier.Decryption(scope.get(i+1).get(1)),
//                    Paillier.Decryption(scope.get(i).get(0)),Paillier.Decryption(scope.get(i).get(1)),
//                    sQuery.getCdata()[0],sQuery.getCdata()[1])){
//                flag = false;
//                break;
//            }
//        }

        return flag;
    }

    private int checkDirectionWithCiphertext(BigInteger x1, BigInteger y1,
                                             BigInteger x2, BigInteger y2,
                                             BigInteger x3, BigInteger y3) {

        //x1.subtract(x2).multiply(y3.subtract(y2))
        BigInteger z1 = Paillier.SM(x1.multiply(x2.modPow(Paillier.n.subtract(new BigInteger("1")),Paillier.nsquare)).mod(Paillier.nsquare),
                                    y3.multiply(y2.modPow(Paillier.n.subtract(new BigInteger("1")),Paillier.nsquare)).mod(Paillier.nsquare));
        //y1.subtract(y2).multiply(x3.subtract(x2))
        BigInteger z2 = Paillier.SM(y1.multiply(y2.modPow(Paillier.n.subtract(new BigInteger("1")),Paillier.nsquare)).mod(Paillier.nsquare),
                                   x3.multiply(x2.modPow(Paillier.n.subtract(new BigInteger("1")),Paillier.nsquare)).mod(Paillier.nsquare));
        BigInteger z3 = z1.multiply(z2.modPow(Paillier.n.subtract(new BigInteger("1")),Paillier.nsquare)).mod(Paillier.nsquare);
        BigInteger z = Paillier.Decryption(z3).mod(Paillier.n);
        if (z.compareTo(Paillier.n.divide(new BigInteger("2")))==1) {
            return 1;
        } else if (z.compareTo(Paillier.n.divide(new BigInteger("2")))==0) {
            return 0;
        } else{
            return -1;
        }
    }

    private boolean SQPWithRect(SSRectangle sRectangle, SPoint sQuery) {
        boolean flag = true;
        ArrayList<BigInteger> low = sRectangle.getScope().get(0);
        ArrayList<BigInteger> high = sRectangle.getScope().get(1);

        BigInteger sq0 = Paillier.Decryption(sQuery.getSdata()[0]);
        BigInteger sq1 = Paillier.Decryption(sQuery.getSdata()[1]);

        BigInteger low00 =  Paillier.Decryption(low.get(0));
        BigInteger llow00 = low00.compareTo(Paillier.n.divide(new BigInteger("2")))==1?low00.subtract(Paillier.n):low00;
        if (sq0.compareTo(llow00)<0){
            flag = false;
        }
        BigInteger high00 =  Paillier.Decryption(high.get(0));
        BigInteger hhigh00 = high00.compareTo(Paillier.n.divide(new BigInteger("2")))==1?high00.subtract(Paillier.n):high00;
        if (sq0.compareTo(hhigh00)>0){
            flag = false;
        }
        BigInteger low01 =  Paillier.Decryption(low.get(1));
        BigInteger llow01 = low01.compareTo(Paillier.n.divide(new BigInteger("2")))==1?low01.subtract(Paillier.n):low01;
        if (sq1.compareTo(llow01)<0){
            flag = false;
        }
        BigInteger high01 =  Paillier.Decryption(high.get(1));
        BigInteger hhigh01 = high01.compareTo(Paillier.n.divide(new BigInteger("2")))==1?high01.subtract(Paillier.n):high01;
        if (sq1.compareTo(hhigh01)>0){
            flag = false;
        }


        return flag;
    }


    public boolean isInRect(Rectangle rectangle,com.secure.rtree.Point query){
        boolean flag = true;
        com.secure.rtree.Point low = rectangle.getLow();
        com.secure.rtree.Point high = rectangle.getHigh();
        if (query.getData()[0]<low.getData()[0]){
            flag = false;
        }
        if (query.getData()[0]>high.getData()[0]){
            flag = false;
        }
        if (query.getData()[1]<low.getData()[1]){
            flag = false;
        }
        if (query.getData()[1]>high.getData()[1]){
            flag = false;
        }

        return flag;
    }

    public boolean isInRect(List<ArrayList<BigInteger>> scope,ArrayList<BigInteger> query){
        boolean flag = true;
        ArrayList<BigInteger> low = scope.get(0);
        ArrayList<BigInteger> high = scope.get(1);
        if (query.get(0).compareTo(low.get(0))==-1){
            flag = false;
        }
        if (query.get(1).compareTo(low.get(1))==-1){
            flag = false;
        }
        if (query.get(0).compareTo(high.get(0))==1){
            flag = false;
        }
        if (query.get(1).compareTo(high.get(1))== 1){
            flag = false;
        }

        return flag;
    }


    public boolean isInScope(List<ArrayList<Double>> scope,com.secure.rtree.Point query) {
        boolean flag = true;
        int y = checkDirection(scope.get(0).get(0),scope.get(0).get(1),
                scope.get(scope.size()-1).get(0),scope.get(scope.size()-1).get(1),
                query.getData()[0],query.getData()[1]);
        for (int i = 0; i<scope.size()-1;i++){
            if (y!=checkDirection(scope.get(i+1).get(0),scope.get(i+1).get(1),
                    scope.get(i).get(0),scope.get(i).get(1),
                    query.getData()[0],query.getData()[1])){
                flag = false;
                break;
            }
        }
        return flag;
    }

    public boolean isInScope(List<ArrayList<BigInteger>> scope,ArrayList<BigInteger> query) {
        boolean flag = true;
        int y = checkDirection(scope.get(1).get(0),scope.get(1).get(1),
                scope.get(0).get(0),scope.get(0).get(1),
                query.get(0),query.get(1));
        for (int i =1; i<scope.size();i++){
            if (y!=checkDirection(scope.get((i+1)%scope.size()).get(0),scope.get((i+1)%scope.size()).get(1),
                    scope.get(i).get(0),scope.get(i).get(1),
                    query.get(0),query.get(1))){
                flag = false;
                break;
            }
        }
        return flag;
    }

    public List<ArrayList<BigInteger>> returnToClient(List<ArrayList<BigInteger>> res, VSPRTree vsprTree){

        Queue<VSPRectangle> vQueue = new LinkedList<>();

        for (VSPRectangle vspRect:vsprTree.getRoot().getDatas()){
            vQueue.offer(vspRect);

        }

        while (!vQueue.isEmpty()) {
            VSPRectangle vspRectangle = vQueue.poll();
            List<ArrayList<BigInteger>> scope =vspRectangle.getScope();

            if (scope !=null){
                if (scope.size()==2){
                    ArrayList<BigInteger> point1 = new ArrayList<>();
                    ArrayList<BigInteger> point2 = new ArrayList<>();
//                    System.out.println("low:(" + Paillier.Decryption(vspRectangle.getScope().get(0).get(0)) + ","
//                            + Paillier.Decryption(vspRectangle.getScope().get(0).get(1)) + ")");
                    BigInteger sp00 = Paillier.Decryption(vspRectangle.getScope().get(0).get(0));
                    point1.add(sp00.compareTo(Paillier.n.divide(new BigInteger("2")))==1?sp00.subtract(Paillier.n):sp00);

                    BigInteger sp01 = Paillier.Decryption(vspRectangle.getScope().get(0).get(1));
                    point1.add(sp01.compareTo(Paillier.n.divide(new BigInteger("2")))==1?sp01.subtract(Paillier.n):sp01);
//                    System.out.println("high(" + Paillier.Decryption(vspRectangle.getScope().get(1).get(0)) + ","
//                            + Paillier.Decryption(vspRectangle.getScope().get(1).get(1)) + ")");
                    BigInteger sp10 = Paillier.Decryption(vspRectangle.getScope().get(1).get(0));
                    point2.add(sp10.compareTo(Paillier.n.divide(new BigInteger("2")))==1?sp10.subtract(Paillier.n):sp10);
                    BigInteger sp11 = Paillier.Decryption(vspRectangle.getScope().get(1).get(1));
                    point2.add(sp11.compareTo(Paillier.n.divide(new BigInteger("2")))==1?sp11.subtract(Paillier.n):sp11);
                    List<ArrayList<BigInteger>> newscope = new ArrayList<>();
                    newscope.add(point1);
                    newscope.add(point2);
                    vspRectangle.setScope(newscope);
                }else{
                    List<ArrayList<BigInteger>> newscope = new ArrayList<>();
                    for (ArrayList<BigInteger> pt:scope){
                        ArrayList<BigInteger> point = new ArrayList<>();
//                        System.out.print("("+Paillier.Decryption(pt.get(0))+","
//                                         +Paillier.Decryption(pt.get(1))+")>>>");
                        BigInteger sp0 = Paillier.Decryption(pt.get(0));
                        point.add(sp0.compareTo(Paillier.n.divide(new BigInteger("2")))==1?sp0.subtract(Paillier.n):sp0);
                        BigInteger sp1 = Paillier.Decryption(pt.get(1));
                        point.add(sp1.compareTo(Paillier.n.divide(new BigInteger("2")))==1?sp1.subtract(Paillier.n):sp1);
                        newscope.add(point);
                    }
                    vspRectangle.setScope(newscope);
//                    System.out.println("\n");
                }
            }else{
                String hashScope = vspRectangle.getHashScope();
                byte[] sig = vspRectangle.getSigScopeWithCell();
//                System.out.println("hash and sig is("+hashScope+","+sig+")");
            }

            if (vspRectangle.getChild()!=null){
                List<VSPRectangle> childRectangles = vspRectangle.getChild().getObjects();
                for (int j = 0; j < childRectangles.size(); j++) {
                    vQueue.add(childRectangles.get(j));
                }
            }
        }

        List<ArrayList<BigInteger>> R = new ArrayList<>();

        for (List<BigInteger> list:res){
            List<BigInteger> point = new ArrayList<>();

            for (int i = 0; i<list.size();i++){
                point.add(Paillier.Decryption(list.get(i)));
            }
            R.add((ArrayList)point);
        }
        return R;
    }

    private boolean isDominatedEachRes(ArrayList<BigInteger> P,double dist, ArrayList<Double> disSet,
                                                   List<ArrayList<BigInteger>> res) {
        boolean flag = false;
        int count;

        for (int j = 0; j <res.size();j++){
            count = 0;
            for (int i =2;i<P.size();i++){
                if (P.get(i).compareTo(res.get(j).get(i))==1){
                    count++;
                }else if (P.get(i).compareTo(res.get(j).get(i))==-1){
                    count = count -P.size()-2;
                }
            }
            if (dist>disSet.get(j)){
                count++;
            }else if(dist<disSet.get(j)){
                count = count -P.size()-2;
            }

            if (count>0){
                flag = true;
                break;
            }


        }
        return flag;
    }

    public boolean verifySig(List<ArrayList<BigInteger>> res,String hash, byte[] sig){
        boolean flag = false;
        for (ArrayList<BigInteger> cell:res){
            String string_cell = "";
            for (BigInteger c:cell){
                string_cell = string_cell+c.toString(10);
            }
//            System.out.println(string_cell);
//            System.out.println(hash);
            boolean f = false;
            try {
                f = Signat.excuteUnSig(Digest.afterMD5(string_cell)+hash,sig);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if (f) {
                flag = true;
                res.remove(cell);
                break;
            }
        }

     return flag;
    }

    public boolean verifyTree(VSPRTree vsprTree, ArrayList<BigInteger> Q, List<ArrayList<BigInteger>> res) {
        boolean flag = true;
        //深度拷贝
        List<ArrayList<BigInteger>> res_bat = new ArrayList<>();
        CollectionUtils.addAll(res_bat, new Object[res.size()]);
        Collections.copy(res_bat, res);

        //先验证结果是否互相支配
        //1.先计算距离
        ArrayList<Double> disSet = new ArrayList<>();
        for (int i = 0; i < res.size(); i++) {
            double dis = getDistance(Q.get(0).doubleValue()/ScopeSRtree.enlarger, Q.get(1).doubleValue()/ScopeSRtree.enlarger,
                    res.get(i).get(0).doubleValue(), res.get(i).get(1).doubleValue());
            disSet.add(dis);
        }
        for (int i = 0; i < res.size(); i++) {
            if (isDominatedEachRes(res.get(i), disSet.get(i), disSet, res)) {
                flag = false;
            }
        }

        Stack<VSPRectangle> queue = new Stack<>();
        VSPRTree vr = vsprTree;
        queue.addAll(vr.getRoot().getObjects());

        while (!queue.isEmpty()) {

            VSPRectangle vspRg = queue.pop();
            List<ArrayList<BigInteger>> scope = vspRg.getScope();

            if (scope != null) {
                if (scope.size() == 2 && vspRg.getHashScope() == null) {
                    if (scope.get(0).get(0).equals(new BigInteger("0"))&&scope.get(0).get(1).equals(new BigInteger("17000")))
                        System.out.println("get!!!!!!!!!!!");
                    if (!isInRect(scope, Q))
                        flag = false;
                }
                if (scope.size() == 2 && vspRg.getHashScope() != null) {


                    if (isInRect(scope, Q))
                        flag = false;
                }
                if (scope.size() > 2) {
                    if (isInScope(scope, Q))
                        flag = false;
                }

            } else {
                String hashScope = vspRg.getHashScope();
                byte[] sig = vspRg.getSigScopeWithCell();
                if (!verifySig(res_bat,hashScope,sig))
                    flag = false;
            }

            if (vspRg.getChild()!=null){
                List<VSPRectangle> childRectangles = vspRg.getChild().getObjects();
                for (int j = 0; j < childRectangles.size(); j++) {
                    queue.add(childRectangles.get(j));
                }
            }

        }
        return flag;
    }


    public static void main(String[] args) throws IOException {
//        List<ArrayList<BigInteger>> D = new ArrayList<ArrayList<BigInteger>>();
//        ReadData rd = new ReadData();
//        ArrayList<ArrayList<String>> data = rd.readFile01("D:\\idea\\SVLSQ\\data_test\\test_2_12.txt");
//        for (int i = 0; i < data.size(); i++) {
//            ArrayList<BigInteger> d = new ArrayList<>();
//            for (int j = 0; j < data.get(i).size(); j++) {
//                d.add(new BigInteger(data.get(i).get(j)));
//            }
//            D.add(d);
//        }
//        ScopeSRtree SSRT = new ScopeSRtree();
//        Map<List<BigInteger>, List<ArrayList<Double>>> xx = SSRT.generateScopeWithCell(D);
////        for (Map.Entry entry : xx.entrySet()) {
////            List<ArrayList<Double>> vauleList = (List<ArrayList<Double>>) entry.getValue();
////            List<BigInteger> keyList = (List<BigInteger>) entry.getKey();
////
////            boolean inSide = true;
////            if (vauleList.size() != 0) {
////                int y = SSRT.checkDirection(vauleList.get(0).get(0), vauleList.get(0).get(1),
////                        vauleList.get(vauleList.size() - 1).get(0), vauleList.get(vauleList.size() - 1).get(1),
////                        20, 20);
////                for (int i = 0; i < vauleList.size() - 1; i++) {
////                    if (y != SSRT.checkDirection(vauleList.get(i + 1).get(0), vauleList.get(i + 1).get(1),
////                            vauleList.get(i).get(0), vauleList.get(i).get(1),
////                            20, 20)) {
////                        inSide = false;
////                        break;
////                    }
////                }
////            }
////            if (inSide) {
////                System.out.println("(" + keyList.get(2) + "," + keyList.get(3) + ")" + "is skyline");
////            }
////        }
//
////        int x = SSRT.if_intersect_np(12, -10, 16, -18, 10, -6, 16, -18);
//
////        int y1 = SSRT.checkDirection(new BigInteger("3"),new BigInteger("0"),
////                new BigInteger("2"),new BigInteger("3"),
////                new BigInteger("2"),new BigInteger("3"));
////
////        int y2 = SSRT.checkDirection(new BigInteger("0"),new BigInteger("0"),
////                new BigInteger("3"),new BigInteger("0"),
////                new BigInteger("2"),new BigInteger("3"));
//
//        /**
//         * 遍历rtree
//         */
//        System.out.println(">>>>>>>>>>>>>>>>新建R-tree>>>>>>>>>>>>>>>>>>>>>>");
//        Map<List<BigInteger>, List<ArrayList<Double>>> map = new HashMap<>();
//
//        for (Map.Entry entry : xx.entrySet()) {
//            List<ArrayList<Double>> vauleList = (List<ArrayList<Double>>) entry.getValue();
//            List<BigInteger> key = (List<BigInteger>) entry.getKey();
//            if (vauleList.size() == 0) {
//                System.out.println("(" + key.get(0) + "," + key.get(1) + "," + key.get(2) + "," + key.get(3) + ")" + "is skyline");
//            } else {
//                map.put(key, vauleList);
//            }
//        }
//
//        RTree tree = SSRT.createRTree(map, 3);
//        SSVRTree svrTree = SSRT.createSSVRTree(tree, 3);
//
//        System.out.println(">>>>>>>>>>>>>>>>遍历R-Tree>>>>>>>>>>>>>>>>>>>>>>");
//        double[] q = {20.0, 20.0};
//        com.secure.rtree.Point query = new com.secure.rtree.Point(q);
//
//        Queue<RTNode> queue = new LinkedList<>();
//
//        queue.offer(tree.getRoot());
//
//        while (!queue.isEmpty()) {
//            RTNode node = queue.poll();
//            Rectangle[] rectangles = node.getDatas();
////            System.out.println(node.getLevel());
////            for (int j = 0; j < rectangles.length; j++)
////                System.out.println(rectangles[j]);
//            if (node.getClass().equals(RTDirNode.class)) {
//                RTDirNode dirNode = (RTDirNode) node;
//                List<RTNode> children = dirNode.getChildren();
//                for (int k = 0; k < children.size(); k++) {
//                    if (SSRT.isInRect(rectangles[k], query)) {
//                        queue.add(children.get(k));
//                    }
//                }
//            } else {
//                for (int j = 0; j < node.getUsedSpace(); j++) {
//                    if (SSRT.isInScope(rectangles[j].getScope(), query)) {
//                        List<BigInteger> key = rectangles[j].getCell();
//                        System.out.println("(" + key.get(0) + "," + key.get(1) + "," + key.get(2) + "," + key.get(3) + ")" + "is skyline");
//                    }
//                }
//            }
//        }
//
//        System.out.println(">>>>>>>>>>>>>>>Scope密文查询>>>>>>>>>>>>>>>>");
//        List<List<BigInteger>> res = SSRT.execute(svrTree, new SPoint(query));
//        for (List<BigInteger> key : res) {
//            System.out.println("(" + Paillier.Decryption(key.get(0)) + "," + Paillier.Decryption(key.get(1)) +
//                    "," + Paillier.Decryption(key.get(2)) + "," + Paillier.Decryption(key.get(3)) + ")" + "is skyline");
//        }
//
//        System.out.println(">>>>>>>>>>>>>>>>>>>>>>可验证查询>>>>>>>>>>>>>>>>>>>>>>");
//        VSPRTree vsprTree = new VSPRTree(3, 0.4f, Constants.RTREE_QUADRATIC, 2);
//        List<ArrayList<BigInteger>> vRes = SSRT.executeWithVerification(svrTree, vsprTree, new SPoint(query));
//
//        List<ArrayList<BigInteger>> plaintREs =SSRT.returnToClient(vRes,vsprTree);
//        for (List<BigInteger> key : plaintREs) {
//            System.out.println("(" + key.get(0) + "," + key.get(1) +
//                    "," + key.get(2) + "," +key.get(3) + ")" + "is skyline");
//        }
//
//        System.out.println(">>>>>>>>>>>>>>>>>>验证结果>>>>>>>>>>>>>>>>>>>>>>>");
//        List<BigInteger> Q = new ArrayList<>();
//        Q.add(new BigInteger("20"));
//        Q.add(new BigInteger("20"));
//        if (SSRT.verifyTree(vsprTree,(ArrayList)Q,plaintREs)){
//            System.out.println("验证成功！！！！！！");
//        }


    }
}
