package com.secure.util;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;

public class ReadData {

    public ArrayList<ArrayList<String>> readFile01(String path) throws IOException {
        ArrayList<ArrayList<String>> res = new ArrayList();
        FileReader fr=new FileReader(path);
        BufferedReader br=new BufferedReader(fr);
        String line="";
        String[] arrs=null;
//        br.readLine();
        while ((line=br.readLine())!=null) {
            arrs=line.split(" ");
            ArrayList<String> al = new ArrayList<>();
            for(String a:arrs){
                if (!"".equals(a)){
                    al.add(a);
                }

//                Paillier paillier = new Paillier();
//                System.out.println(paillier.Decryption(new BigInteger(a.trim())));
            }
            res.add(al);
        }
        br.close();
        fr.close();
        return res;
    }
    public void recordSize(String path) throws IOException {
        FileReader fr=new FileReader(path);
        BufferedReader br=new BufferedReader(fr);
        String line="";
        String[] arrs=null;

        int num =0;
        while ((line=br.readLine())!=null) {
            arrs=line.split(" ");
            ArrayList<String> al = new ArrayList<>();
            if (!arrs[0].equals("")){
                num++;
            }
        }
        String filename = path.substring(0,path.lastIndexOf("."));
        String [] p =filename.split("_");
//        if (Integer.valueOf(p[p.length-1])!=num){
            System.out.println(filename+":"+num+"条记录");
//        }

        br.close();

    }


    public ArrayList<ArrayList<String>> readFile02(String path) throws IOException {
        ArrayList<ArrayList<String>> res = new ArrayList();
        FileReader fr=new FileReader(path);
        BufferedReader br=new BufferedReader(fr);
        String line="";
        String[] arrs=null;
        br.readLine();
        while ((line=br.readLine())!=null) {
            arrs=line.split(" ");
            ArrayList<String> al = new ArrayList<>();
            String sss ="";
            for(String a:arrs){
                al.add(a);
                Paillier paillier = new Paillier();
               sss = sss +" "+paillier.Decryption(new BigInteger(a.trim())).toString();
            }
            System.out.println(sss);
            res.add(al);
        }
        br.close();
        fr.close();
        return res;
    }


    public void writeFile01(ArrayList<ArrayList<String>> r, String path) throws IOException {


        //写入中文字符时会出现乱码

        //BufferedWriter  bw=new BufferedWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File("E:/phsftp/evdokey/evdokey_201103221556.txt")), "UTF-8")));

        Double ma = new Double(1000000);
        for (int i =0;i<6;i++){
            String p = path.substring(0,path.lastIndexOf("."));
            FileWriter fw=new FileWriter(new File(p+"_"+1000*(2*i+1)+".txt"));
            BufferedWriter bw=new BufferedWriter(fw);

            int num = 0;
            for(ArrayList<String> al:r){
                String st = "" ;
                for(String a:al) {
                    BigDecimal temp = BigDecimal.valueOf(Double.valueOf(a)).multiply(BigDecimal.valueOf(ma));

                    int s = temp.intValue();
                    Paillier paillier = new Paillier();
//                BigInteger ss = paillier.Encryption(new BigInteger(s+""));
                    st = st+" "+s;
                }
//            System.out.println(st.trim());
                if(num > 1000*(2*i+1)){
                    break;
                }
                bw.write(st.trim()+"\t\n");
                num++;
            }
            bw.close();
            fw.close();
        }

    }

    /*
     * 读取指定路径下的文件名和目录名
     */
    public ArrayList<String> getFileList(String pathname) {
        ArrayList<String> res = new ArrayList<>();

        File file = new File(pathname);

        File[] fileList = file.listFiles();

        for (int i = 0; i < fileList.length; i++) {
            if (fileList[i].isFile()) {
                String fileName = fileList[i].getName();
//                System.out.println("读取文件：" + fileName);
                res.add(fileName);
            }
        }
        return res;
    }

    public int genarteRandomNum(){
        int max=1000,min=1;
        int ran2 = (int) (Math.random()*(max-min)+min);
        return ran2;
    }

    private void writeFileWithRandom(ArrayList<ArrayList<String>> r, String path) throws IOException {
            String p = path.substring(0,path.lastIndexOf("."));
            String[] pSet = p.split("_");
            int num_s = Integer.parseInt(pSet[pSet.length-1]);

            FileWriter fw=new FileWriter(new File(path));
            BufferedWriter bw=new BufferedWriter(fw);

            int num = 0;
            for(ArrayList<String> al:r){
                String st = "" ;
                st = st + genarteRandomNum()+" "+genarteRandomNum();

                for(String a:al) {
                    BigDecimal temp = BigDecimal.valueOf(Double.valueOf(a.trim()));
                    int s = temp.intValue();

                    st = st+" "+s;
                }
//            System.out.println(st.trim());
                num=num+1;
                if(num > num_s){
                    break;
                }
                bw.write(st.trim()+"\t\n");
            }
            bw.close();
            fw.close();

    }
    private void writeFileWith1Ton(ArrayList<ArrayList<String>> r, String path) throws IOException {

        String p = path.substring(0, path.lastIndexOf("."));
        String[] pSet = p.split("_");
        for (int i =1;i<9;i++) {

            int num_s = i*1000;

            FileWriter fw = new FileWriter(new File(pSet[0]+"_"+pSet[1]+"_"+num_s+".txt"));
            BufferedWriter bw = new BufferedWriter(fw);

            int num = 0;
            for (ArrayList<String> al : r) {
                String st = "";
//                st = st + genarteRandomNum() + " " + genarteRandomNum();

                for (String a : al) {
                    BigDecimal temp = BigDecimal.valueOf(Double.valueOf(a.trim()));
                    int s = temp.intValue();

                    st = st + " " + s;
                }
//            System.out.println(st.trim());
                num = num + 1;
                if (num > num_s) {
                    break;
                }
                bw.write(st.trim() + "\t\n");
            }
            bw.close();
            fw.close();
        }
    }


    public static void main(String[] args) {

    ReadData rd = new ReadData();
        ArrayList<String> filenames = rd.getFileList("D:\\idea\\SVLSQ\\exec\\execdata1ToN");
        for(String fn:filenames){
            try {
                ArrayList<ArrayList<String>> data  = rd.readFile01("D:\\idea\\SVLSQ\\exec\\execdata1ToN\\"+fn);
                rd.writeFileWith1Ton(data,"D:\\idea\\SVLSQ\\exec\\execdata1ToN\\"+fn);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("********文件中记录数*******");

        ArrayList<String> f = rd.getFileList("D:\\idea\\SVLSQ\\exec\\execdata1ToN\\");
        for(String fn:f){
            try {
                rd.recordSize("D:\\idea\\SVLSQ\\exec\\execdata1ToN\\"+fn);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
//
//        ArrayList<String> fns = rd.getFileList("D:\\idea\\data2\\");
//        for(String fn:fns){
//            try {
//                ArrayList<ArrayList<String>> data  = rd.readFile01("D:\\idea\\data2\\"+fn);
//                rd.writeFile01(data,"D:\\idea\\cdata2\\"+fn);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }

//        try {
//            ArrayList<ArrayList<String>> data  = rd.readFile01("D:\\idea\\data1\\2D_data_anti_corr1000");
//            rd.writeFile01(data,"D:\\idea\\cdata1\\2D_data_anti_corr1000");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

//        try {
//            ArrayList<ArrayList<String>> data  = rd.readFile02("D:\\idea\\cdata2\\5d_data_independent_1000");
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

//        for (int i= 0;i<100;i++){
//            System.out.println(rd.genarteRandomNum());;
//        }

    }


}