package com.secure.alg;


import com.secure.rtree.Point;
import com.secure.scope.ScopeSRtree;
import com.secure.svrtree.SRectangle;
import com.secure.svrtree.SVRTNode;
import com.secure.util.Signat;
import com.secure.vtree.*;
import com.secure.rtree.Constants;
import com.secure.svrtree.SPoint;
import com.secure.svrtree.SVRTree;
import com.secure.util.Digest;
import com.secure.util.ObjectWithFile;
import com.secure.util.Paillier;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class BVLSQ {
    private SVRTree svrTree;

    public BVLSQ(SVRTree svrTree) {
        this.svrTree = svrTree;
    }

    public BVLSQ() {
    }

    public SVRTree getSvrTree() {
        return svrTree;
    }

    public void setSvrTree(SVRTree svrTree) {
        this.svrTree = svrTree;
    }

    private double distWithMiwen(SRectangle sRectangle,SPoint sQuery){
        double NoSquare_dis= 0;
        if (sRectangle.getDis()==-1){
            NoSquare_dis = getNoSquareDist(sRectangle,sQuery);
            sRectangle.setDis(NoSquare_dis);
        }else{
            NoSquare_dis = sRectangle.getDis();
        }

        for (BigInteger sp:sRectangle.getLow().getCdata()){
            NoSquare_dis = NoSquare_dis+sp.doubleValue();
        }

        return NoSquare_dis;
    }

    private double getNoSquareDist(SRectangle sRectangle, SPoint sQuery) {
        BigInteger[] sloc_low = sRectangle.getLow().getSlocation();
        BigInteger[] sloc_high = sRectangle.getHigh().getSlocation();

        BigInteger square_dis = new BigInteger("0");
        //x\<=y,返回[1];x>y, 返回[0]
        double low0_flag = Paillier.Decryption(Paillier.IntegerComp(sloc_low[0],sQuery.getSdata()[0])).doubleValue();
        double high0_flag = Paillier.Decryption(Paillier.IntegerComp(sQuery.getSdata()[0],sloc_high[0])).doubleValue();

        double low1_flag = Paillier.Decryption(Paillier.IntegerComp(sloc_low[1],sQuery.getSdata()[1])).doubleValue();
        double high1_flag = Paillier.Decryption(Paillier.IntegerComp(sQuery.getSdata()[1],sloc_high[1])).doubleValue();

        if (low0_flag==0&&low1_flag==0){
            BigInteger df0= sloc_low[0].multiply(sQuery.getSdata()[0].modPow(Paillier.n.subtract(new BigInteger("1")),Paillier.nsquare));
            BigInteger df1= sloc_low[1].multiply(sQuery.getSdata()[1].modPow(Paillier.n.subtract(new BigInteger("1")),Paillier.nsquare));
            square_dis = Paillier.SM(df0,df0).multiply(Paillier.SM(df1,df1).mod(Paillier.nsquare));
        }else if (low0_flag==0&&low1_flag==1&&high1_flag==1){
            BigInteger df0= sloc_low[0].multiply(sQuery.getSdata()[0].modPow(Paillier.n.subtract(new BigInteger("1")),Paillier.nsquare));
            square_dis = Paillier.SM(df0,df0).mod(Paillier.nsquare);
        }else if (low0_flag==0&&high1_flag==0){
            BigInteger df0= sloc_low[0].multiply(sQuery.getSdata()[0].modPow(Paillier.n.subtract(new BigInteger("1")),Paillier.nsquare));
            BigInteger df1= sloc_high[1].multiply(sQuery.getSdata()[1].modPow(Paillier.n.subtract(new BigInteger("1")),Paillier.nsquare));
            square_dis = Paillier.SM(df0,df0).multiply(Paillier.SM(df1,df1).mod(Paillier.nsquare));
        }else if (low0_flag==1&&high0_flag==1&&high1_flag==0){
            BigInteger df0= sloc_high[1].multiply(sQuery.getSdata()[1].modPow(Paillier.n.subtract(new BigInteger("1")),Paillier.nsquare));
            square_dis = Paillier.SM(df0,df0).mod(Paillier.nsquare);
        }else if (high0_flag==0&&high1_flag==0){
            BigInteger df0= sloc_high[0].multiply(sQuery.getSdata()[0].modPow(Paillier.n.subtract(new BigInteger("1")),Paillier.nsquare));
            BigInteger df1= sloc_high[1].multiply(sQuery.getSdata()[1].modPow(Paillier.n.subtract(new BigInteger("1")),Paillier.nsquare));
            square_dis = Paillier.SM(df0,df0).multiply(Paillier.SM(df1,df1).mod(Paillier.nsquare));
        }else if (high0_flag==0&&low1_flag==1&&high1_flag==1){
            BigInteger df0= sloc_high[0].multiply(sQuery.getSdata()[0].modPow(Paillier.n.subtract(new BigInteger("1")),Paillier.nsquare));
            square_dis = Paillier.SM(df0,df0).mod(Paillier.nsquare);
        }else if (high0_flag==0&&low1_flag==0){
            BigInteger df0= sloc_high[0].multiply(sQuery.getSdata()[0].modPow(Paillier.n.subtract(new BigInteger("1")),Paillier.nsquare));
            BigInteger df1= sloc_low[1].multiply(sQuery.getSdata()[1].modPow(Paillier.n.subtract(new BigInteger("1")),Paillier.nsquare));
            square_dis = Paillier.SM(df0,df0).multiply(Paillier.SM(df1,df1).mod(Paillier.nsquare));
        }else if (low0_flag==1&&high0_flag==1&&low1_flag==0){
            BigInteger df0= sloc_low[1].multiply(sQuery.getSdata()[1].modPow(Paillier.n.subtract(new BigInteger("1")),Paillier.nsquare));
            square_dis = Paillier.SM(df0,df0).mod(Paillier.nsquare);
        }else{
            square_dis = new BigInteger("0");
        }
        return Math.sqrt(Paillier.Decryption(square_dis).doubleValue());
    }

    private BigInteger distWithPlaintext(SPoint sPoint){
        BigInteger dis = new BigInteger("0");
        for (BigInteger sp:sPoint.getCdata()){
            dis = dis.add(sp);
        }
        return dis;
    }

//    public Stack<SRectangle> sortSRect(Stack<SRectangle> queue){
//        Stack<SRectangle> headMinQueue = new Stack<>();
//
//        SPoint sminsp = queue.pop().getLow();
////        for (BigInteger b:sminsp.getSdata()){
////            System.out.println("取出第一个"+Paillier.Decryption(b));
////        }
//
//        while (!queue.isEmpty()) {
//            SPoint sp = queue.pop().getLow();
////            for (BigInteger b:sp.getSdata()){
////                System.out.println("取出下一个"+Paillier.Decryption(b));
////            }
//
//            BigInteger out = Paillier.IntegerComp(dist(sminsp),dist(sp));
////            BigInteger tmp1 =  Paillier.Decryption(out);
////            System.out.println("dist(sminsp)："+Paillier.Decryption(dist(sminsp)));
////            System.out.println("dist(sp)："+Paillier.Decryption(dist(sp)));
////            System.out.println("大小："+tmp1);
//
//
//            BigInteger fout = Paillier.Encryption(new BigInteger("1")).multiply
//                    (out.modPow(Paillier.n.subtract(new BigInteger("1")), Paillier.nsquare)).mod(Paillier.nsquare);
////            BigInteger tmp2 =  Paillier.Decryption(fout);
////            System.out.println("大小："+tmp2);
//
//            BigInteger[] minSData = new BigInteger[sminsp.getSdata().length];
//            BigInteger[] minCData = new BigInteger[sminsp.getCdata().length];
//            BigInteger[] maxSData = new BigInteger[sminsp.getSdata().length];
//            BigInteger[] maxCData = new BigInteger[sminsp.getCdata().length];
//
//            for (int j =0;j<sp.getSdata().length;j++){
//                minSData[j] = Paillier.SM(sminsp.getSdata()[j],out).multiply(Paillier.SM(sp.getSdata()[j],fout))
//                        .mod(Paillier.nsquare);
//                minCData[j] = Paillier.SM(sminsp.getCdata()[j],out).multiply(Paillier.SM(sp.getCdata()[j],fout))
//                        .mod(Paillier.nsquare);
//
//                maxSData[j] = Paillier.SM(sminsp.getSdata()[j],fout).multiply(Paillier.SM(sp.getSdata()[j],out))
//                        .mod(Paillier.nsquare);
//                maxCData[j] = Paillier.SM(sminsp.getCdata()[j],fout).multiply(Paillier.SM(sp.getCdata()[j],out))
//                        .mod(Paillier.nsquare);
//            }
//
//            BigInteger minHSData = Paillier.SM(sminsp.geteHSdata(),out).multiply(Paillier.SM(sp.geteHSdata(),fout))
//                    .mod(Paillier.nsquare);
//            BigInteger minHCData = Paillier.SM(sminsp.geteHCdata(),out).multiply(Paillier.SM(sp.geteHCdata(),fout))
//                    .mod(Paillier.nsquare);
//
//            BigInteger maxHSData = Paillier.SM(sminsp.geteHSdata(),fout).multiply(Paillier.SM(sp.geteHSdata(),out))
//                    .mod(Paillier.nsquare);
//            BigInteger maxHCData = Paillier.SM(sminsp.geteHCdata(),fout).multiply(Paillier.SM(sp.geteHCdata(),out))
//                    .mod(Paillier.nsquare);
//
//            headMinQueue.push(new SRectangle(new SPoint(maxSData,maxHSData,maxCData,maxHCData)));
//            sminsp = new SPoint(minSData,minHSData,minCData,minHCData);
//        }
//        headMinQueue.push(new SRectangle(sminsp));
//        return headMinQueue;
//    }

    public Stack<SRectangle> sortSRectWithPlaintext(Stack<SRectangle> queue,SPoint sQuery){
        Stack<SRectangle> headMinQueue = new Stack<>();

        SRectangle sminsle = queue.pop();

//        SPoint sminsp = sminsle.getLow();
        double mindist = distWithMiwen(sminsle,sQuery);

        while (!queue.isEmpty()) {
            SRectangle sle = queue.pop();

            double spdist = distWithMiwen(sle,sQuery);

            if (mindist<spdist){
                headMinQueue.push(sle);
            }else{
                headMinQueue.push(sminsle);
                sminsle = sle;
                mindist = spdist;
            }
        }
        headMinQueue.push(sminsle);
        return headMinQueue;
    }

    public Stack<SAndVRectangle<SRectangle, VRectangle>> sortSandVRectWithPlaintext
                                        (Stack<SAndVRectangle<SRectangle, VRectangle>> queue,SPoint sQuery){
        Stack<SAndVRectangle<SRectangle, VRectangle>> headMinQueue = new Stack<>();

        SAndVRectangle<SRectangle, VRectangle> minSAndVRect = queue.pop();
        SRectangle sminsle  =minSAndVRect.getOb1();

//        double mindist = mindistWithPlaintext(sminsle,sQuery);

        double mindist = 0;
        if (minSAndVRect.getMindist()==-1){
            mindist  = distWithMiwen(sminsle,sQuery);
        }else{
            mindist = minSAndVRect.getMindist();
        }



        while (!queue.isEmpty()) {
            SAndVRectangle<SRectangle, VRectangle> sAndVRect = queue.pop();
            SRectangle sle = sAndVRect.getOb1();

//            double spdist = mindistWithPlaintext(sle,sQuery);

            double spdist = 0;
            if (sAndVRect.getMindist()==-1){
                spdist  = distWithMiwen(sle,sQuery);
            }else{
                spdist = sAndVRect.getMindist();
            }

//            int out = mindist.compareTo(spdist);

            if (mindist<=spdist){
                headMinQueue.push(sAndVRect);
            }else{
                headMinQueue.push(minSAndVRect);
                minSAndVRect = sAndVRect;
                sminsle = sle;
                mindist = spdist;
            }
        }
        headMinQueue.push(minSAndVRect);
        return headMinQueue;
    }

//    private double mindistWithPlaintext(SRectangle sRectangle, SPoint sQuery) {
//        double dis1= 0.0;
//        if (sRectangle.getDis()==-1){
//            double dis0 = distWithMiwen(sRectangle,sQuery);
//            dis1 = dis0;
//            sRectangle.setDis(dis1);
//        }else{
//            dis1 = sRectangle.getDis();
//        }
//
//
//
//        for (BigInteger sp:sRectangle.getLow().getCdata()){
//            dis1 = dis1+sp.doubleValue();
//        }
////        BigInteger sd = Paillier.Decryption(square_dis);
//        return dis1;
//    }

    private double getNewDist(SRectangle sRectangle, SPoint sQ) {
        double dist = 0;

        BigInteger[] sloc_low = sRectangle.getLow().getClocation();
        BigInteger[] sloc_high = sRectangle.getHigh().getClocation();

        ArrayList<BigInteger> Q = new ArrayList<>();
        Q.add(sQ.getCdata()[0]);
        Q.add(sQ.getCdata()[1]);

        if (sloc_low[0].equals(sloc_high[0])&&sloc_low[1].equals(sloc_high[1])){
            BigInteger df0= sloc_low[0].subtract(Q.get(0));
            BigInteger df1= sloc_low[1].subtract(Q.get(1));
            dist = Math.sqrt(df0.pow(2).doubleValue()+df1.pow(2).doubleValue());
            return dist;
        }else{
            //x\<=y,返回[1];x>y, 返回[0]
            double low0_flag;
            double high0_flag;
            double low1_flag;
            double high1_flag;
            if (sloc_low[0].compareTo(Q.get(0))==1){
                low0_flag = 0;
            }else{
                low0_flag = 1;
            }

            if (Q.get(0).compareTo(sloc_high[0])==1){
                high0_flag = 0;
            }else{
                high0_flag = 1;
            }

            if (sloc_low[1].compareTo(Q.get(1))==1){
                low1_flag = 0;
            }else{
                low1_flag = 1;
            }
            if (Q.get(1).compareTo(sloc_high[1])==1){
                high1_flag = 0;
            }else{
                high1_flag = 1;
            }


            if (low0_flag==0&&low1_flag==0){
                BigInteger df0= sloc_low[0].subtract(Q.get(0));
                BigInteger df1= sloc_low[1].subtract(Q.get(1));
                dist = Math.sqrt(df0.pow(2).doubleValue()+df1.pow(2).doubleValue());
            }else if (low0_flag==0&&low1_flag==1&&high1_flag==1){
                BigInteger df0= sloc_low[0].subtract(Q.get(0));
                dist = df0.doubleValue();
            }else if (low0_flag==0&&high1_flag==0){
                BigInteger df0= sloc_low[0].subtract(Q.get(0));
                BigInteger df1= sloc_high[1].subtract(Q.get(1));
                dist = Math.sqrt(df0.pow(2).doubleValue()+df1.pow(2).doubleValue());
            }else if (low0_flag==1&&high0_flag==1&&high1_flag==0){
                BigInteger df0= Q.get(1).subtract(sloc_high[1]);
                dist = df0.doubleValue();
            }else if (high0_flag==0&&high1_flag==0){
                BigInteger df0= sloc_high[0].subtract(Q.get(0));
                BigInteger df1= sloc_high[1].subtract(Q.get(1));
                dist = Math.sqrt(df0.pow(2).doubleValue()+df1.pow(2).doubleValue());
            }else if (high0_flag==0&&low1_flag==1&&high1_flag==1){
                BigInteger df0= Q.get(0).subtract(sloc_high[0]);
                dist = df0.doubleValue();
            }else if (high0_flag==0&&low1_flag==0){
                BigInteger df0= sloc_high[0].subtract(Q.get(0));
                BigInteger df1= sloc_low[1].subtract(Q.get(1));
                dist = Math.sqrt(df0.pow(2).doubleValue()+df1.pow(2).doubleValue());
            }else if (low0_flag==1&&high0_flag==1&&low1_flag==0){
                BigInteger df0= sloc_low[1].subtract(Q.get(1));
                dist = df0.doubleValue();
            }else{
                dist = 0;
            }
            return dist;
        }

    }

    private boolean isDominatedInSet(SRectangle sRg, List<SPoint> entries){
        List<BigInteger> b = Arrays.asList(sRg.getLow().getSdata());
        for (SPoint entry:entries){
            List<BigInteger> a = Arrays.asList(entry.getSdata());
            BigInteger res = Paillier.ESDOM(a,b);
            if (Paillier.Decryption(res).equals(new BigInteger("1"))) {
                return true;
            }
        }
        return false;
    }

    private boolean isDominatedInSetWithPlaintext(SRectangle sRg,SPoint sQuery, List<SPoint> entries){

        boolean flag = false;
        int count;
        BigInteger[] cdata = sRg.getLow().getCdata();
        double dis = 0;
        if (sRg.getDis()!=-1){
            dis = sRg.getDis();
        }else{
            dis = getNewDist(sRg,sQuery);
        }

        for (SPoint sPoint:entries){
            count = 0;
            for (int i =0;i<cdata.length;i++){
                if (cdata[i].compareTo(sPoint.getCdata()[i])==1){
                    count++;
                }else if (cdata[i].compareTo(sPoint.getCdata()[i])==-1){
                    count = count -cdata.length-2;
                }
            }
            double dis_p = sPoint.getsDist();
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

    public List<SPoint> execute(SPoint sQuery){
        List<SPoint> res = new LinkedList<>();
        Stack<SRectangle> queue = new Stack<>();
        SVRTree svr = getSvrTree();
        queue.addAll(svr.getRoot().getObjects());

        while(!queue.isEmpty()){
            queue = sortSRectWithPlaintext(queue,sQuery);
            SRectangle sRg = queue.pop();
//            if (Paillier.Decryption(sRg.getLow().getSdata()[0]).compareTo(new BigInteger("88"))==0){
//                System.out.println("88888888888888");
//            }

//            System.out.println("点"+Paillier.Decryption(sRg.getLow().getSlocation()[0])+","
//                    +Paillier.Decryption(sRg.getLow().getSlocation()[1])+","+Paillier.Decryption(sRg.getLow().getSdata()[0])+","+
//                    Paillier.Decryption(sRg.getLow().getSdata()[1]));
//            if(new BigInteger("10").equals(Paillier.Decryption(sRg.getLow().getSdata()[0]))){
//                System.out.println("找到了！");
//            }
            boolean dom = isDominatedInSetWithPlaintext(sRg,sQuery,res);
            if (!dom){
//                System.out.println("左下角的点"+Paillier.Decryption(sRg.getLow().getSdata()[0])+","+
//                        Paillier.Decryption(sRg.getLow().getSdata()[1]));
                if (sRg.getChild()!=null){
                    for (SRectangle sRectangle: sRg.getChild().getObjects()){
                        if (!isDominatedInSetWithPlaintext(sRectangle,sQuery,res)){
                            queue.add(sRectangle);
                        }
                    }
                }else{
//                    BigInteger df0= sRg.getLow().getSlocation()[0].multiply(sQuery.getSdata()[0].modPow(Paillier.n.subtract(new BigInteger("1")),Paillier.nsquare));
//                    BigInteger df1= sRg.getLow().getSlocation()[1].multiply(sQuery.getSdata()[1].modPow(Paillier.n.subtract(new BigInteger("1")),Paillier.nsquare));
//                    sRg.getLow().setsDist(Paillier.SM(df0,df0).multiply(Paillier.SM(df1,df1).mod(Paillier.nsquare)));
                    BigInteger df0= sRg.getLow().getClocation()[0].subtract(sQuery.getCdata()[0]);
                    BigInteger df1= sRg.getLow().getClocation()[1].subtract(sQuery.getCdata()[1]);
                    sRg.getLow().setsDist(Math.sqrt(df0.pow(2).add(df1.pow(2)).doubleValue()));
                    res.add(sRg.getLow());
                }
            }
        }

        return res;
    }

    public List<SPoint> executeWithVerification(VRTree vrTree,SPoint sQuery){
        List<SPoint> res = new LinkedList<>();
        Stack<SAndVRectangle<SRectangle, VRectangle>> queue = new Stack<>();

        SVRTree svr = getSvrTree();
        List<SRectangle> objects = svr.getRoot().getObjects();
        VRectangle[] vds = new VRectangle[objects.size()];

        int vLevel = svr.getRoot().getLevel();
        int vUsedSpace = svr.getRoot().getUsedSpace();


        for (int i = 0; i<vUsedSpace; i++){
            vds[i] = new VRectangle();
            queue.add(new SAndVRectangle<>(objects.get(i),vds[i]));
        }

        VRTNode vrtNode = new VRTDirNode(vrTree,vLevel,vds, vUsedSpace);
        vrTree.setRoot(vrtNode);

        while(!queue.isEmpty()){
            //排序，把最小的最前面
            queue = sortSandVRectWithPlaintext(queue,sQuery);

            SAndVRectangle<SRectangle, VRectangle> sAndVrg = queue.pop();
            SRectangle sRg = sAndVrg.getOb1();
            VRectangle vRg = sAndVrg.getOb2();

            if (isDominatedInSetWithPlaintextVerification(sRg,sQuery,res)){
                vRg.setLow(new VPoint(sRg.getLow().geteHSdata(), sRg.getLow().getCdata(),sRg.getLow().getClocation()
                        , sRg.getLow().geteHCdata()));
                vRg.setHigh(new VPoint(sRg.getHigh().geteHSdata(), sRg.getHigh().getCdata(),sRg.getHigh().getClocation()
                        , sRg.getHigh().geteHCdata()));
            }else{
                if (sRg.getChild()!=null){
                    SVRTNode childSVRTNode = sRg.getChild();
                    List<SRectangle> childrenSrg = childSVRTNode.getObjects();
                    VRectangle[] childrenVrg = new VRectangle[childrenSrg.size()];
                    for (int k =0; k<childrenSrg.size();k++){
                        if (isDominatedInSetWithPlaintextVerification(childrenSrg.get(k),sQuery,res)){
                            childrenVrg[k] = new VRectangle(new VPoint(childrenSrg.get(k).getLow().geteHSdata(),
                                          childrenSrg.get(k).getLow().getCdata(), childrenSrg.get(k).getLow().getClocation(),
                                    childrenSrg.get(k).getLow().geteHCdata()),
                                    new VPoint(childrenSrg.get(k).getHigh().geteHSdata(),
                                            childrenSrg.get(k).getHigh().getCdata(), childrenSrg.get(k).getHigh().getClocation(),
                                            childrenSrg.get(k).getHigh().geteHCdata())
                                    );

                        }else{
                            childrenVrg[k] = new VRectangle(new VPoint(childrenSrg.get(k).getLow().geteHSdata(),
                                    childrenSrg.get(k).getLow().getCdata(),childrenSrg.get(k).getLow().getClocation(),
                                    childrenSrg.get(k).getLow().geteHCdata()),
                                    new VPoint(childrenSrg.get(k).getHigh().geteHSdata(),
                                            childrenSrg.get(k).getHigh().getCdata(),childrenSrg.get(k).getHigh().getClocation(),
                                            childrenSrg.get(k).getHigh().geteHCdata())
                                    );
                            queue.add(new SAndVRectangle<SRectangle, VRectangle>(childrenSrg.get(k),childrenVrg[k]));
                        }
                    }
                    vRg.setLow(new VPoint(sRg.getLow().geteHSdata(), sRg.getLow().getCdata(),sRg.getLow().getClocation(),
                                                             sRg.getLow().geteHCdata()));
                    vRg.setHigh(new VPoint(sRg.getHigh().geteHSdata(), sRg.getHigh().getCdata(),sRg.getHigh().getClocation(),
                            sRg.getHigh().geteHCdata()));
                    vRg.setChild(new VRTDirNode(vrTree,childSVRTNode.getLevel(),
                            childrenVrg,childSVRTNode.getUsedSpace()));
                }else{
                    BigInteger df0= sRg.getLow().getClocation()[0].subtract(sQuery.getCdata()[0]);
                    BigInteger df1= sRg.getLow().getClocation()[1].subtract(sQuery.getCdata()[1]);
                    sRg.getLow().setsDist(Math.sqrt(df0.pow(2).add(df1.pow(2)).doubleValue()));
                    res.add(sRg.getLow());
                }
            }
        }

        return res;
    }

    private boolean isDominatedInSetWithPlaintextVerification(SRectangle sRg,SPoint sQuery, List<SPoint> entries) {


        boolean flag = false;
        int count;
        BigInteger[] cdata = sRg.getLow().getCdata();
        double dis = 0.0;
        if (sRg.getDis()!=-1){
            dis = sRg.getDis();
        }else{
            dis = getNoSquareDist(sRg,sQuery);
        }

        for (SPoint sPoint:entries){
            count = 0;
            for (int i =0;i<cdata.length;i++){
                if (cdata[i].compareTo(sPoint.getCdata()[i])==1){
                    count++;
                }else if (cdata[i].compareTo(sPoint.getCdata()[i])==-1){
                    count = count -cdata.length-2;
                }
            }
            double dis_p = sPoint.getsDist();
            if (dis>(dis_p)){
                count++;
            }else if(dis<(dis_p)){
                count = count -cdata.length-2;
            }

            if (count>0){
                flag = true;
                return flag;
            }

        }
        return flag;

    }

    public boolean verifyTree(VRTree vrTree, List<SPoint> res,List<BigInteger> Q) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        boolean flag = true;
        //先验证结果是否互相支配
        //1.先计算距离
        ArrayList<Double> disSet = new ArrayList<>();
        for (int i = 0; i < res.size(); i++) {
            double dis = new ScopeSRtree().getDistance(Q.get(0).doubleValue(), Q.get(1).doubleValue(),
                    res.get(i).getClocation()[0].doubleValue(), res.get(i).getClocation()[1].doubleValue());
            disSet.add(dis);
        }
        for (int i = 0; i < res.size(); i++) {
            if (isDominatedEachRes(res.get(i),res, disSet.get(i), disSet)) {
                flag = false;
            }
        }

        Stack<VRectangle> queue = new Stack<>();
        VRTree vr = vrTree;
        queue.addAll(vr.getRoot().getObjects());

        while(!queue.isEmpty()){

            VRectangle vRg = queue.pop();

            String cdatastr = ""+vRg.getLow().getcLocation()[0]+vRg.getLow().getcLocation()[1];


            BigInteger[] cdata = vRg.getLow().getCdata();
            for (int i = 0; i < cdata.length;i++){
                cdatastr = cdatastr + cdata[i];
            }
            BigInteger hCdata = vRg.getLow().geteHSdata();
            BigInteger s=null;
            try {
                s = new BigInteger(Digest.afterMD5(cdatastr),16);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
//            BigInteger ss = new BigInteger(s, 16);
            if(!s.equals(hCdata)){
                flag = false;
            }

            if (vRg.getChild()!=null){
                queue.addAll(vRg.getChild().getObjects());
            }else{
                //判断是否结果集中的点;存在MBR的，因为被剪枝了
                if(!isInRes(vRg,res)){
//                    if (vRg.getLow().getCdata()[0].equals(new BigInteger("88")))
//                        System.out.println("get!!!!!");
                    if(!isDominatedInRes(vRg,Q,res,disSet)){
                        flag =false;
                    }
                }else {
                   boolean f = Signat.excuteUnSig(Digest.afterMD5(cdatastr),vRg.getLow().geteHCdata());
                    if (!f)
                        flag = false;
                }

                  }
                }

        return flag;
    }

    //这种判断是否正确
    private boolean isInRes(VRectangle vRg, List<SPoint> res) {
        boolean flag = false;
        BigInteger[] cdata = vRg.getLow().getCdata();
        BigInteger[] cHighData = vRg.getHigh().getCdata();

        BigInteger[] clocation = vRg.getLow().getcLocation();
        BigInteger[] cHighCloc = vRg.getHigh().getcLocation();

        String cdl = ""+clocation[0]+clocation[1];
        for (BigInteger c:cdata){
            cdl = cdl+c;
        }

        String cHighdl = ""+cHighCloc[0]+cHighCloc[1];
        for (BigInteger c:cHighData){
            cHighdl = cHighdl +c;
        }

        if (cdl.equals(cHighdl)){
            for (SPoint sPoint:res){
                String resdl = ""+sPoint.getClocation()[0]+sPoint.getClocation()[1];
                for (BigInteger c:sPoint.getCdata()){
                    resdl = resdl+c;
                }
                if (resdl.equals(cdl)){
                    flag = true;
                    return flag;
                }
            }
        }

        return flag;
    }

    private boolean isDominatedInRes(VRectangle vRg, List<BigInteger> Q, List<SPoint> res, ArrayList<Double> distSet) {
        boolean flag = false;
        int count;
        BigInteger[] cdata = vRg.getLow().getCdata();
        double dist = 0.0;

//        计算距离
        BigInteger[] sloc_low = vRg.getLow().getcLocation();
        BigInteger[] sloc_high = vRg.getHigh().getcLocation();

       if (sloc_low[0].equals(sloc_high[0])&&sloc_low[1].equals(sloc_high[1])){
           BigInteger df0= sloc_low[0].subtract(Q.get(0));
           BigInteger df1= sloc_low[1].subtract(Q.get(1));
           dist = Math.sqrt(df0.pow(2).doubleValue()+df1.pow(2).doubleValue());
       }else{
           //x\<=y,返回[1];x>y, 返回[0]
           double low0_flag;
           double high0_flag;
           double low1_flag;
           double high1_flag;
           if (sloc_low[0].compareTo(Q.get(0))==1){
               low0_flag = 0;
           }else{
               low0_flag = 1;
           }

           if (Q.get(0).compareTo(sloc_high[0])==1){
               high0_flag = 0;
           }else{
               high0_flag = 1;
           }

           if (sloc_low[1].compareTo(Q.get(1))==1){
               low1_flag = 0;
           }else{
               low1_flag = 1;
           }
           if (Q.get(1).compareTo(sloc_high[1])==1){
               high1_flag = 0;
           }else{
               high1_flag = 1;
           }


           if (low0_flag==0&&low1_flag==0){
               BigInteger df0= sloc_low[0].subtract(Q.get(0));
               BigInteger df1= sloc_low[1].subtract(Q.get(1));
               dist = Math.sqrt(df0.pow(2).doubleValue()+df1.pow(2).doubleValue());
           }else if (low0_flag==0&&low1_flag==1&&high1_flag==1){
               BigInteger df0= sloc_low[0].subtract(Q.get(0));
               dist = df0.doubleValue();
           }else if (low0_flag==0&&high1_flag==0){
               BigInteger df0= sloc_low[0].subtract(Q.get(0));
               BigInteger df1= sloc_high[1].subtract(Q.get(1));
               dist = Math.sqrt(df0.pow(2).doubleValue()+df1.pow(2).doubleValue());
           }else if (low0_flag==1&&high0_flag==1&&high1_flag==0){
               BigInteger df0= Q.get(1).subtract(sloc_high[1]);
               dist = df0.doubleValue();
           }else if (high0_flag==0&&high1_flag==0){
               BigInteger df0= sloc_high[0].subtract(Q.get(0));
               BigInteger df1= sloc_high[1].subtract(Q.get(1));
               dist = Math.sqrt(df0.pow(2).doubleValue()+df1.pow(2).doubleValue());
           }else if (high0_flag==0&&low1_flag==1&&high1_flag==1){
               BigInteger df0= Q.get(0).subtract(sloc_high[0]);
               dist = df0.doubleValue();
           }else if (high0_flag==0&&low1_flag==0){
               BigInteger df0= sloc_high[0].subtract(Q.get(0));
               BigInteger df1= sloc_low[1].subtract(Q.get(1));
               dist = Math.sqrt(df0.pow(2).doubleValue()+df1.pow(2).doubleValue());
           }else if (low0_flag==1&&high0_flag==1&&low1_flag==0){
               BigInteger df0= sloc_low[1].subtract(Q.get(1));
               dist = df0.doubleValue();
           }else{
               dist = 0;
           }
       }

        for (int j = 0;j<res.size();j++){
            count = 0;
            for (int i =0;i<cdata.length;i++){
                if (cdata[i].compareTo(res.get(j).getCdata()[i])==1){
                    count++;
                }else if (cdata[i].compareTo(res.get(j).getCdata()[i])==-1){
                    count = count -cdata.length;
                }
            }
            if (dist>distSet.get(j)){
                count++;
            }else if(dist<distSet.get(j)){
                count = count -cdata.length-2;
            }

            if (count>0){
                flag = true;
            }

        }
        return flag;
    }

    private boolean isDominatedEachRes(SPoint P, List<SPoint> res,double dist , ArrayList<Double> distSet) {
        boolean flag = false;
        int count;
        BigInteger[] cdata = P.getCdata();
        for (int j = 0;j<res.size();j++){
            count = 0;
            for (int i =0;i<cdata.length;i++){
                if (cdata[i].compareTo(res.get(j).getCdata()[i])==1){
                    count++;
                }else if (cdata[i].compareTo(res.get(j).getCdata()[i])==-1){
                    count = count -cdata.length-2;
                }
            }
            if (dist>distSet.get(j)){
                count++;
            }else if(dist<distSet.get(j)){
                count = count -cdata.length-2;
            }


            if (count>0){
                flag = true;
                return flag;
            }

        }
        return flag;
    }

    public static double[] readFile(String path) throws IOException {
        String filename = path.substring(0,path.lastIndexOf("."));
        String [] p =filename.split("_");
        Integer num_p = Integer.valueOf(p[p.length - 1]);
        Integer dim_p = Integer.valueOf(p[p.length - 2]);

        double[] res = new double[num_p*(dim_p+2)];
        FileReader fr=new FileReader(path);
        BufferedReader br=new BufferedReader(fr);
        String line="";
        String[] arrs=null;
        int i = 0;
        while ((line=br.readLine())!=null) {
            arrs=line.split(" ");
            ArrayList<Float> al = new ArrayList<>();
            for(String a:arrs){
                if (!"".equals(a)){

                    res[i] = Integer.valueOf(a.trim());
                    i++;
                }

            }

        }
        br.close();
        fr.close();
        return res;
    }

//    public void preProcessingTree(VRTree vrTree){
//        Queue<VRTNode> vQueue = new LinkedList<>();
//        vQueue.offer(vrTree.getRoot());
//
//        while (!vQueue.isEmpty()) {
//            VRTNode node = vQueue.poll();
//            VRectangle[] vrectangles = node.getDatas();
//
//            for (int j = 0; j < vrectangles.length; j++) {
//                vrectangles[j].getLow().seteHCdata(Paillier.Decryption(vrectangles[j].getLow().geteHCdata()));
//                for (int i = 0; i<vrectangles[j].getLow().getCdata().length;i++){
//                    vrectangles[j].getLow().getCdata()[i] = Paillier.Decryption(vrectangles[j].getLow().getCdata()[i]);
//                }
//                if(vrectangles[j].getChild()!=null)
//                    vQueue.add(vrectangles[j].getChild());
//            }
//        }
//    }
//    public List<SPoint> preProcessingRes(List<SPoint> res){
//        List<SPoint> R = new ArrayList<>();
//
//        for (SPoint sPoint:res){
//            SPoint p = new SPoint();
//            p.seteHCdata(sPoint.geteHCdata());
//            BigInteger[] tmp = new BigInteger[sPoint.getCdata().length];
//            for (int i = 0; i<sPoint.getCdata().length;i++){
//                tmp[i] = Paillier.Decryption(sPoint.getCdata()[i]);
//            }
//            p.setCdata(tmp);
//            R.add(p);
//        }
//        return R;
//    }

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
//        double[] f = {1,9,
//                2,10,
//                4,8,
//                6,7,
//                9,10,
//                7,5,
//                5,6,
//                4,3,
//                3,2,
//                9,1,
//                10,4,
//                6,2,
//                8,3,
//                8,4,
//        };
//        SVRTree svrTree = SVRTree.createSVRTree(f,3,2);
//        ObjectWithFile.writeObjectToFile(svrTree,"test");
//        SVRTree svrTree = (SVRTree) ObjectWithFile.readObjectFromFile("test");
//        BVLSQ bvlsq = new BVLSQ(svrTree);
//        List<SPoint> res = bvlsq.execute();
//        for (SPoint s:res){
//            System.out.println("结果为："+ Paillier.Decryption(s.getSdata()[0])+","+ Paillier.Decryption(s.getSdata()[1]));
//        }
//
//        VRTree vrTree = new VRTree(3, 0.4f, Constants.RTREE_QUADRATIC, 2);
//
//        double[] data={20.0,20.0};
//
//        List<SPoint> res2 = bvlsq.executeWithVerification(vrTree,new SPoint(new Point(data)));
//
//        for (SPoint s:res2){
//            System.out.println("结果为："+ Paillier.Decryption(s.getSdata()[0])+","+ Paillier.Decryption(s.getSdata()[1]));
//        }

//        bvlsq.preProcessingTree(vrTree);
//        System.out.println("验证结果："+bvlsq.verifyTree(vrTree,res2));

//        System.out.println(">>>>>>>>>>>>>>>>遍历SVR-Tree>>>>>>>>>>>>>>>>>>>>>>");
//        Queue<SVRTNode> queue = new LinkedList<>();
//
//        queue.offer(svrTree.getRoot());
//
//        while (!queue.isEmpty()) {
//            SVRTNode node = queue.poll();
//            SRectangle[] srectangles = node.getDatas();
//            System.out.println(node.getLevel());
//            for (int j = 0; j < srectangles.length; j++)
//                System.out.println(srectangles[j]);
//            if(node.getClass().equals(SVRTDirNode.class)) {
//                SVRTDirNode dirNode = (SVRTDirNode) node;
//                List<SVRTNode> children = dirNode.getChildren();
//                queue.addAll(children);
//            }
//        }
//
//        System.out.println(">>>>>>>>>>>>>>>>遍历VR-Tree>>>>>>>>>>>>>>>>>>>>>>");
//        Queue<VRTNode> vQueue = new LinkedList<>();
//
//        vQueue.offer(vrTree.getRoot());
//
//        while (!vQueue.isEmpty()) {
//            VRTNode node = vQueue.poll();
//            VRectangle[] vrectangles = node.getDatas();
//            System.out.println(node.getLevel());
//            for (int j = 0; j < vrectangles.length; j++) {
//                System.out.println(vrectangles[j]);
//                if(vrectangles[j].getChild()!=null)
//                    vQueue.add(vrectangles[j].getChild());
//            }
//        }
//        Stack<SRectangle> queue = new Stack<>();
//        float[] f = {2,10,
//                    6,7,
//                    7,5,
//                    4,3,
//                    9,1,
//                    6,2,
//                    8,4
//        };
//        for(int i = 0; i < f.length;)
//        {
//            SRectangle sRectangle = new SRectangle(new SPoint(new Point(new float[]{f[i++],f[i++]})));
//            queue.add(sRectangle);
//        }
//        BVLSQ bsq = new BVLSQ();
//        SPoint sPoint = bsq.sortSRect(queue).pop().getLow();
//
//        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
//        String strS = "";
//        for (BigInteger b:sPoint.getSdata()){
//            System.out.println(b+">>>>>>>>"+Paillier.Decryption(b));
//            strS = strS + Paillier.Decryption(b);
//        }
//        System.out.println(sPoint.geteHSdata()+">>>>>>>>>>>>>>>>>"+Paillier.Decryption(sPoint.geteHSdata()));
//        System.out.println("验证："+strS+"的摘要"+ new BigInteger(Digest.afterMD5(strS),16));
//
//        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
//        String strC = "";
//        for (BigInteger b:sPoint.getCdata()){
//            System.out.println(b+">>>>>>>>"+Paillier.Decryption(b));
//            strC = strC + Paillier.Decryption(b);
//        }
//        System.out.println(sPoint.geteHCdata()+">>>>>>>>>>>>>>>>>"+Paillier.Decryption(sPoint.geteHCdata()));
//        System.out.println("验证："+ new BigInteger(Digest.afterMD5(strC),16));
//        System.out.println("验证是否匹配"+new BigInteger(Digest.afterMD5(new OPE().encrypt(new BigInteger("4"))+
//                ""+new OPE().encrypt(new BigInteger("3"))),16));

        System.out.println(">>>>>>>>>>>>>>>>>>>>>测试距离>>>>>>>>>>>>>>>>>>>>>>");

        double [] low1 = {10.0,10.0};
        SPoint low_p = new SPoint(new Point(low1,low1));
        double [] high1 = {30.0,20.0};
        SPoint high_p = new SPoint(new Point(high1,high1));

        SRectangle sRect = new SRectangle(low_p,high_p);

        double[] q={24,5};
        SPoint sQuery = new SPoint(new Point(q));

        BVLSQ bvlsq = new BVLSQ();
        bvlsq.distWithMiwen(sRect,sQuery);

    }

}
