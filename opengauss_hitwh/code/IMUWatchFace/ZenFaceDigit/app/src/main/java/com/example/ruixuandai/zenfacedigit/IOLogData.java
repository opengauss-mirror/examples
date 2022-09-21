package com.example.ruixuandai.zenfacedigit;

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


    static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS");
    static SimpleDateFormat formatter2 = new SimpleDateFormat("HH:mm:ss.SSS");
    static SimpleDateFormat formatter3 = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
    static Date curDate = null;

    static final String dataFolder = Environment.getExternalStorageDirectory()+"/sensorLog";
    static File debugLog = new File(Environment.getExternalStorageDirectory(), "watchFace.log");


    public static void writeData(String fileName,
                                    byte[][] data, boolean append){
        long start = System.currentTimeMillis();
        File log = new File(dataFolder, fileName);
        FileOutputStream fOutStream;

        Log.e(TAG, log.getName());
        checkFile(log);

        try {
            fOutStream = new FileOutputStream(log, append);
            for(byte[] col : data)
                fOutStream.write(col);

            fOutStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "wrt fail!!!!!!!");
        }
        Log.e(TAG, "Write Time:"+(System.currentTimeMillis()-start));

    }

    public static void writeDataXYZ(String fileName,
                                    byte[] dataX,byte[] dataY,byte[] dataZ, boolean append){

        File log = new File(dataFolder, fileName);
        FileOutputStream fOutStream = null;

        Log.e(TAG, log.getName());
        checkFile(log);

        try {
            fOutStream = new FileOutputStream(log, append);
            fOutStream.write(dataX);
            fOutStream.write(dataY);
            fOutStream.write(dataZ);
            fOutStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "wrt fail!!!!!!!");
        }

    }



    public static void writeData(String fileName, byte[] data, boolean append) {
        File log = new File(dataFolder, fileName);
        FileOutputStream fOutStream = null;

        Log.e(TAG, log.getName());
        checkFile(log);



        try {
            fOutStream = new FileOutputStream(log, append);
            Log.v(TAG, ""+data.length);
            fOutStream.write(data);
            fOutStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "wrt fail!!!!!!!");
        }
    }


    public static void checkFile(File log){
        /*
        Check if the log exists
         */
        if (log.exists()) {
            Log.v(TAG, log.getName() + " exits");
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

    public static void writeLog(String TAG, String logString){
        checkFile(debugLog);

        try {
            FileOutputStream lOutStream = new FileOutputStream(debugLog, true);
            String tempString =dateFormatter.format(new Date(System.currentTimeMillis())) + "---" +
                    TAG + ": " +logString +"\n";
            lOutStream.write(tempString.getBytes());
            lOutStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "wrt fail!!!!!!!");
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

}