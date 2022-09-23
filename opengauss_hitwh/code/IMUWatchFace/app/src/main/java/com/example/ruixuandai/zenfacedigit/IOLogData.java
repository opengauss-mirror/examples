package com.example.ruixuandai.zenfacedigit;

import android.content.Intent;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by ruixuandai on 11/7/17.
 */

public class IOLogData {
    private static final String TAG = "IOLogData";


    static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss_SSS");
    static SimpleDateFormat formatter2 = new SimpleDateFormat("HH:mm:ss.SSS");
    static SimpleDateFormat formatter3 = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");



    static final String dataFolder = Environment.getExternalStorageDirectory()+"/CPSL/Raw";
    static final String logFolder = Environment.getExternalStorageDirectory()+"/CPSL/Log";





    public static boolean writeData(String fileName,
                                    byte[][] data, boolean append){
        long start = System.currentTimeMillis();
        File log = new File(dataFolder, fileName);
        FileOutputStream fOutStream;

        Log.d(TAG, log.getName());
        checkFile(log);

        try {
            fOutStream = new FileOutputStream(log, append);
            for(byte[] col : data) {
                fOutStream.write(col);
            }

            fOutStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "wrt fail!!!!!!!");
            return false;
        }
        Log.d(TAG, "Write Time:"+(System.currentTimeMillis()-start));
        return true;

    }



    public static void checkFile(File log){
        /*
        Check if the log exists
         */
        if (log.exists()) {
            return;
        }
        else {
            try {
                if (!log.getParentFile().exists()) {
                    //父目录不存在 创建父目录
                    Log.e(TAG, "creating parent directory..." + log.getParentFile());
                    if (!log.getParentFile().mkdirs()) {
                        Log.e(TAG, "created parent directory failed.");
                    }
                }
                log.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "/n cannot create new file");
            }
        }
    }


    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public static void writeLog(String TAG, String logString){
        String date = dateFormatter.format(new Date(System.currentTimeMillis()));


        File debugLog = new File(logFolder, date.substring(0,10)+"-watchFaceLog.txt");
        checkFile(debugLog);

        try {
            FileOutputStream lOutStream = new FileOutputStream(debugLog, true);
            String tempString =date.substring(11) + "---" +
                    TAG + ": " +logString +"\n";
            lOutStream.write(tempString.getBytes());
            lOutStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "wrt fail!!!!!!!");
        }

    }


    public static void writeActivityLog(String act){
            long curTime =  System.currentTimeMillis();
            String date = dateFormatter.format(new Date(curTime));


        File actLog = new File(logFolder, date.substring(0,10)+"-actLog.txt");
        checkFile(actLog);

        try {
            FileOutputStream lOutStream = new FileOutputStream(actLog, true);
            String tempString =date.substring(11) + ","+curTime+","+act +"\n";
            lOutStream.write(tempString.getBytes());
            lOutStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "wrt fail!!!!!!!");
        }
    }
}