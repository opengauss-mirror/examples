package com.secure.alg;

import com.carrotsearch.sizeof.RamUsageEstimator;
import com.secure.rtree.Point;
import com.secure.rtree.RTree;
import com.secure.util.ObjectWithFile;
import com.secure.util.ReadData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestBBS {

    private void excute(String path, int capacity, Point query) {
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
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>类型"+typeName+"维度"+dim+"数量"+num+"容量"+capacity+">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        long startTime=System.currentTimeMillis();
        RTree tree = RTree.createRTree(f,capacity,dim);
        long endTime=System.currentTimeMillis();
        System.out.println("RTree的构造的时间： "+(endTime-startTime)+"ms");
        System.out.println("RTree的大小： "+ RamUsageEstimator.humanSizeOf(tree));
        ObjectWithFile.writeObjectToFile(tree,"D:\\skyline_dataset\\test_exec\\"+"basic_"+capacity+"_"+typeName+"_"+dim+"_"+num);

//        SVRTree svrTree = (SVRTree)ObjectWithFile.readObjectFromFile("D:\\idea\\SVLSQ\\test\\test_object\\"+"basic_"+typeName+"_"+dim+"_"+num);


        System.out.println(">>>>>>>>>>>>>>>>>>LSQ查询>>>>>>>>>>>>>>>>>>>>>>");

        LSQ lsq = new LSQ();
//       lsq.showTree(tree);

        long startTime1=System.currentTimeMillis();
        List<Point> res = lsq.execute(tree,query);
        long endTime1=System.currentTimeMillis();
        System.out.println("LSQ查询运行时间："+(endTime1-startTime1)+"ms");
        System.out.println("结果集大小："+res.size()+"个");
//        for (Point pp:res){
//            System.out.println(pp.toString()+","+pp.getDist());
//        }
    }

    public static void main(String[] args) {
        String mulu = "D:\\skyline_dataset\\testBBS02";
        TestBBS testBBS = new TestBBS();
        ReadData rd = new ReadData();
        ArrayList<String> f = rd.getFileList(mulu);


        double[] q = {6000, 5100};
//        double[] q = {10208, 76204};
        com.secure.rtree.Point query = new com.secure.rtree.Point(q);

        for(String fn:f){
            for (int ca = 3; ca<4;ca=ca+2){
                testBBS.excute(mulu+"\\"+fn,ca,query);
            }
        }
    }


}
