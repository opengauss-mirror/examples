package com.secure.alg;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;


import com.carrotsearch.sizeof.RamUsageEstimator;
import com.secure.rtree.Constants;
import com.secure.rtree.RTree;
import com.secure.scope.CellAndScope;
import com.secure.scope.ScopeSRtree;
import com.secure.ssvrtree.SSVRTree;
import com.secure.svrtree.SPoint;
import com.secure.util.ObjectWithFile;
import com.secure.util.Paillier;
import com.secure.util.ReadData;
import com.secure.vsptree.VSPRTree;

public class TestAlg {
    public void BAndS(String path, int capacity, SPoint sQuery) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        double[] f = new double[0];
        String filename = path.substring(0,path.lastIndexOf("."));
        String[] split = filename.split("//"); //linux openEuler
        //String[] split = filename.split("\\\\");//windows
        //String[] split = filename.split("\\\\");//linux lab_ubuntu

        String [] p =split[split.length-1].split("_");
        String typeName = p[0];//服务器上运行有修改
        Integer num = Integer.valueOf(p[p.length - 1]);
        Integer dim = Integer.valueOf(p[p.length - 2]);

        try {
            f = BVLSQ.readFile(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>类型"+typeName+"维度"+dim+"数量"+num+"容量"+capacity+">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
//      www
//        System.out.println(">>>>>>>>>>>>>>>>>>BVLSQ查询>>>>>>>>>>>>>>>>>>>>>>");
//
//        BVLSQ bvlsq = new BVLSQ(svrTree);
//
//        long startTime1=System.currentTimeMillis();
//        List<SPoint> res = bvlsq.execute(sQuery);
//        long endTime1=System.currentTimeMillis();
//        System.out.println("BVLSQ查询运行时间："+(endTime1-startTime1)+"ms");
//
////        for (SPoint s:res){
////            System.out.println("结果为："+ Paillier.Decryption(s.getSlocation()[0])+","+Paillier.Decryption(s.getSlocation()[1])+","+
////                    Paillier.Decryption(s.getSdata()[0])+","+Paillier.Decryption(s.getSdata()[1]));
////        }
//
////        System.out.println(">>>>>>>>>>>>>>>>BVLSQ查询与生成验证>>>>>>>>>>>>>>>>>>>>>>");
//        VRTree vrTree = new VRTree(capacity, 0.4f, Constants.RTREE_QUADRATIC, dim);
//        long startTime2=System.currentTimeMillis();
//        List<SPoint> res2 = bvlsq.executeWithVerification(vrTree,sQuery);
//        long endTime2=System.currentTimeMillis();
////        System.out.println("BVLSQ构造验证树的运行时间： "+(endTime2-startTime2)+"ms");
////
////        for (SPoint s:res2){
////            System.out.println("结果为："+  Paillier.Decryption(s.getSlocation()[0])+","+Paillier.Decryption(s.getSlocation()[1])+","+
////                                          Paillier.Decryption(s.getSdata()[0])+","+Paillier.Decryption(s.getSdata()[1]));
////        }
////        System.out.println("BVLSQ查询的结果数量为： "+res2.size()+"个");
//        System.out.println("Size of VO:"+ RamUsageEstimator.humanSizeOf(vrTree));
//
//        List<BigInteger> Q = new ArrayList<>();
//        Q.add(Paillier.Decryption(sQuery.getSdata()[0]));
//        Q.add(Paillier.Decryption(sQuery.getSdata()[1]));
//
//        long startTime3 = System.currentTimeMillis();
//        boolean v_flag = bvlsq.verifyTree(vrTree,res2,Q);
//        long endTime3 = System.currentTimeMillis();
//        if (v_flag){
//            System.out.println("验证成功！！！！！！");
//        }
//        System.out.println("BVLSQ验证skyline结果的时间： "+(endTime3-startTime3)+"ms");


        System.out.println(">>>>>>>>>>>>>>>>>>><<>>>>SVLSQ>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        List<ArrayList<BigInteger>> D = new ArrayList<ArrayList<BigInteger>>();
        ReadData rd = new ReadData();
        ArrayList<ArrayList<String>> data = null;
        try {
            data = rd.readFile01(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < data.size(); i++) {
            ArrayList<BigInteger> d = new ArrayList<>();
            for (int j = 0; j < data.get(i).size(); j++) {

                BigInteger tmp =  new BigInteger(data.get(i).get(j).trim());
                d.add(tmp);
            }
            D.add(d);
        }
        ScopeSRtree SSRT = new ScopeSRtree();

        //构建索引计时开始
        List<ArrayList<BigInteger>> plaintREs = new ArrayList<>();

        long scope_startTime=System.currentTimeMillis();

        List<CellAndScope> xx = SSRT.generateScopeWithCell(D);
        List<CellAndScope> mapSets = new ArrayList<>();

        for (int ll=0;ll<xx.size();ll++) {
            List<ArrayList<Double>> vauleList = xx.get(ll).getScope();
            List<BigInteger> key = xx.get(ll).getCell();
            if (vauleList.size() == 0) {
                plaintREs.add((ArrayList<BigInteger>)key);
            } else {
                CellAndScope cAs = new CellAndScope();
                cAs.setCell(key);
                cAs.setScope(vauleList);
                mapSets.add(cAs);
            }
        }
//        System.out.println("时间111111111111111： "+(System.currentTimeMillis()-scope_startTime)+"ms");
        RTree tree = SSRT.createRTree(mapSets, capacity);
        SSVRTree ssvrTree = SSRT.createSSVRTree(tree, capacity);
        long scope_endTime=System.currentTimeMillis();
        System.out.println("SVSRTree的构造的时间： "+(scope_endTime-scope_startTime)+"ms");
        System.out.println("SVSRTree的大小： "+ RamUsageEstimator.humanSizeOf(ssvrTree));
        //linux lab_ubuntu
        ObjectWithFile.writeObjectToFile(ssvrTree,"//home//lujf//svlsq//skyline_dataset//test_exec//" +"scope_"+capacity+"_"+typeName+"_"+dim+"_"+num);
        //ObjectWithFile.writeObjectToFile(ssvrTree,"//root//svlsq//skyline_dataset//test_exec//" +"scope_"+capacity+"_"+typeName+"_"+dim+"_"+num);
        //ObjectWithFile.writeObjectToFile(ssvrTree,"D:\\skyline_dataset\\test_exec\\" +"scope_"+capacity+"_"+typeName+"_"+dim+"_"+num);

//        SSVRTree ssvrTree = (SSVRTree)ObjectWithFile.readObjectFromFile("D:\\skyline_dataset\\test_exec\\"
//                                                                           +"scope_"+capacity+"_"+typeName+"_"+dim+"_"+num);

        System.out.println(">>>>>>>>>>>>>>>>SVLSQ查询>>>>>>>>>>>>>>>>>>>>>>");
        double[] qEnlarge = {sQuery.getCdata()[0].doubleValue()*(ScopeSRtree.enlarger), sQuery.getCdata()[1].doubleValue()*(ScopeSRtree.enlarger)};
        com.secure.rtree.Point queryEnlarge = new com.secure.rtree.Point(qEnlarge);
        SPoint sEnlargeQeury = new SPoint(queryEnlarge);


        long scope_startTime1=System.currentTimeMillis();
        List<List<BigInteger>> res_scope = SSRT.execute(ssvrTree, sEnlargeQeury);
        long scope_endTime1=System.currentTimeMillis();
        System.out.println("SVLSQ查询运行时间："+(scope_endTime1-scope_startTime1)+"ms");
//
////        for (List<BigInteger> key : plaintREs) {
////            System.out.println("(" + (key.get(0)) + "," + (key.get(1)) +
////                    "," + (key.get(2)) + "," + (key.get(3)) + ")" + "is plaint skyline");
////        }
////        for (List<BigInteger> key : res_scope) {
////            System.out.println("(" + Paillier.Decryption(key.get(0)) + "," + Paillier.Decryption(key.get(1)) +
////                    "," + Paillier.Decryption(key.get(2)) + "," + Paillier.Decryption(key.get(3)) + ")" + "is skyline");
////        }
////        System.out.println("结果数量："+res_scope.size()+plaintREs.size());
//
//        System.out.println(">>>>>>>>>>>>>>>>scope查询与生成验证>>>>>>>>>>>>>>>>>>>>>>");

        long scope_startTime2=System.currentTimeMillis();
        VSPRTree vsprTree = new VSPRTree(capacity, 0.4f, Constants.RTREE_QUADRATIC, 2);
        List<ArrayList<BigInteger>> vRes = SSRT.executeWithVerification(ssvrTree, vsprTree, sEnlargeQeury);
        long scope_endTime2=System.currentTimeMillis();
//        System.out.println("scope查询与构造验证树的运行时间： "+(scope_endTime2-scope_startTime2)+"ms");

//        List<ArrayList<BigInteger>> vRes_plait = SSRT.returnToClient(vRes,vsprTree);
//
//        plaintREs.addAll(vRes_plait);
//
////        for (List<BigInteger> key : plaintREs) {
////            System.out.println("(" + key.get(0) + "," + key.get(1) +
////                    "," + key.get(2) + "," + key.get(3) + ")" + "is skyline");
////        }
////        System.out.println("scope查询的结果数量为： "+plaintREs.size()+"个");
        System.out.println("Size of VO:"+ RamUsageEstimator.humanSizeOf(vsprTree));

        List<BigInteger> QEnlarge = new ArrayList<>();
        QEnlarge.add(Paillier.Decryption(sQuery.getSdata()[0]).multiply(new BigDecimal(ScopeSRtree.enlarger).toBigIntegerExact()));
        QEnlarge.add(Paillier.Decryption(sQuery.getSdata()[1]).multiply(new BigDecimal(ScopeSRtree.enlarger).toBigIntegerExact()));

        long scope_startTime3 = System.currentTimeMillis();
        if (SSRT.verifyTree(vsprTree,(ArrayList)QEnlarge,plaintREs)){
            System.out.println("验证成功！！！！！！");
        }
        long scope_endTime3 = System.currentTimeMillis();

        System.out.println("SVLSQ验证skyline结果的时间： "+(scope_endTime3-scope_startTime3)+"ms");

////        if (!(SSRT.verifyTree(vsprTree,(ArrayList)QEnlarge,vRes_plait)&&v_flag&&(plaintREs.size()==res2.size()))){
////            System.out.println(Paillier.Decryption(sQuery.getSdata()[0])+","+Paillier.Decryption(sQuery.getSdata()[1]));
////        }


    }

    public static void main(String[] args) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        String mulu = "//home//lujf//svlsq//skyline_dataset//test04";//linux lab_ubuntu
        //String mulu = "//root//svlsq//skyline_dataset//test04";//linux openEuler
        //String mulu = "D:\\skyline_dataset\\test04";//windows

        TestAlg testAlg = new TestAlg();
        ReadData rd = new ReadData();
        ArrayList<String> f = rd.getFileList(mulu);


        double[] q = {100,403};
        com.secure.rtree.Point query = new com.secure.rtree.Point(q);
        SPoint sQuery = new SPoint(query);
        for(String fn:f){
            for (int ca = 3; ca<4;ca=ca+2){
                //testAlg.BAndS(mulu+"\\"+fn,ca,sQuery);//windows
                testAlg.BAndS(mulu+"//"+fn,ca,sQuery); //linux
            }
        }

//        for(String fn:f){
//            for (int i =10;i<100;i++){
//                for (int j =1;j<100;j++){
//                    double[] q = {i, j};
//                    com.secure.rtree.Point query = new com.secure.rtree.Point(q);
//                    SPoint sQuery = new SPoint(query);
//                    testAlg.BAndS("D:\\idea\\SVLSQ\\test\\test_data\\"+fn,3,sQuery);
//                }
//            }
//
//        }
//        testAlg.BAndS("D:\\idea\\index_skyline\\skyline data\\proc_data\\anti_3_1000.txt",3);

    }

}
