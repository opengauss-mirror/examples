package com.example.ruixuandai.zenfacedigit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.text.format.Formatter;
import android.util.Log;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


/**
 * Created by ruixuandai on 11/26/17.
 */

public class ChargingState extends BroadcastReceiver {
    private static final String TAG = "ChargingState";
    private static MyWatchFace myWatchFace;

    static AmazonS3Client s3Client;
    static UploadThread uThread = null;
    private static boolean wifiSwitch = false;
    static WifiManager wifi;
    static WifiManager.WifiLock wifiLock;
    static Boolean ExidFlag = false;
    private Socket socket=null;
    private Context context;
    private String spilt=";;;";
    private String ip="192.168.0.104";
    public ChargingState(MyWatchFace myWatchFace) {
        this.myWatchFace = myWatchFace;
    }

    @Override
    public void onReceive(final Context context, Intent intent) {

        this.context=context;
        new Thread(new Runnable() {
            @Override
            public void run() {
                // wait charging connection stable
                try {
                    Thread.sleep(5000L);  // 5s
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                boolean isCharging = isPlugged(myWatchFace);
                Log.e(TAG, "isCharging?:" + isCharging);
                wifi = (WifiManager) myWatchFace.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                wifiLock = wifi.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, TAG);

                if (isCharging) {
                    wifi.setWifiEnabled(true);//Turn on Wifi

                    if (!wifiLock.isHeld())
                        wifiLock.acquire();
                    if (uThread == null) {
                        uThread = new UploadThread();
                        uThread.start();
                    } else if (!uThread.isAlive()) {
                        uThread = new UploadThread();
                        uThread.start();
                    }

                } else {
                    if (wifiLock.isHeld())
                        wifiLock.release();
                    wifi.setWifiEnabled(!wifiSwitch);//Turn off Wifi

                }


            }
        }).start();


    }


    public static boolean checkWifiOnAndConnected(Context context) {
        WifiManager wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        if (wifiMgr.isWifiEnabled()) { // Wi-Fi adapter is ON

            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();

            if (wifiInfo.getNetworkId() == -1) {
                return false; // Not connected to an access point
            }
            return true; // Connected to an access point
        } else {
            return false; // Wi-Fi adapter is OFF
        }
    }


    public static boolean isPlugged(Context context) {
        boolean isPlugged;
        Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        isPlugged = plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB;

        return isPlugged;
    }

    public static boolean isPlugged() {
        boolean isPlugged;
        Intent intent = myWatchFace.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        isPlugged = plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB;

        return isPlugged;
    }

    public static float getBatteryLevel() {
        Intent batteryStatus = myWatchFace.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        return level / (float) scale;
    }

    private File[] getAllFile(String path) {
        Log.d(TAG, "Path: " + path);
        File directory = new File(path);
        File[] files = directory.listFiles();

        return files;
    }

    private boolean findExict() {
        String fileName = "WT-DeviceID.txt";
        Boolean f = s3Client.doesObjectExist(AwsS3.BUCKET_NAME, fileName);
        if (f.equals(false)) {
            return false;
        } else {
            byte[] data = null;
            InputStream input = null;
            FileOutputStream fileOutputStream = null;
            S3Object s3Obj = null;
            try {
                s3Obj = s3Client.getObject(AwsS3.BUCKET_NAME, fileName);
                input = s3Obj.getObjectContent();

                data = new byte[input.available()];
                String filekey = Environment.getExternalStorageDirectory() + "/CPSL/" + fileName;
                fileOutputStream = new FileOutputStream(filekey);
                int len = 0;
                while ((len = input.read(data)) != -1) {
                    fileOutputStream.write(data, 0, len);
                }
                System.out.println("下载文件成功");
                BufferedReader br=new BufferedReader(new FileReader(new File(filekey)));
                String s=null;
                while((s=br.readLine())!=null){
                    if(s.equals(Build.SERIAL)){
                        return true;
                    }
                }
                br.close();
                return false;
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
        return false;
    }

    public class UploadThread extends Thread {
        public void run() {
            setName("Uploading");
            // Wait the wifi connection
            while (!checkWifiOnAndConnected(myWatchFace)) {
                Log.v(TAG, "Waiting for wifi");
                try {
                    Thread.sleep(1000L); // wait 1s
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (!isPlugged(myWatchFace)) { // if it is not charging, terminate the thread

                    Log.v(TAG, " Unplugged!");
                    wifi.setWifiEnabled(!wifiSwitch);//Turn off Wifi
                    return;
                }
            }

            try {
                // initiate S3
                if (s3Client == null) {
                    s3Client = AwsS3.getS3Client(myWatchFace);
                }

            } catch (Exception e) {
                Log.e(TAG, "AWS connection error", e);
            }

            // wait mqtt connection
            try {
                Thread.sleep(5000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // check the S3 bucket name exists
            try {
                if (!s3Client.doesBucketExist(AwsS3.BUCKET_NAME))
                    s3Client.createBucket(AwsS3.BUCKET_NAME);
            } catch (AmazonClientException exception) {
                Log.v(TAG, "Check bucket error...", exception);
            }


            try {
                uploadData();
            } catch (IOException e) {
                e.printStackTrace();
            }

            //维护日期列表
//            upLoadDate();

            try {
                uploadLog();
            } catch (IOException e) {
                e.printStackTrace();
            }

            //维护手表ID文件
//            uploadExict();


            Log.v(TAG, "Upload finished");
            s3Client.shutdown();
            s3Client = null;

            return;

        }
        public String getWifiIP(Context context){
//            Log.i(tag,"1111111111111111111");
            WifiManager wifi_service = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            DhcpInfo dhcpInfo = wifi_service.getDhcpInfo();
            String routeIp = Formatter.formatIpAddress(dhcpInfo.gateway);
            //Toast.makeText(context,routeIp,Toast.LENGTH_LONG);
            Log.i(TAG, "wifi route ip：" + routeIp);
            return routeIp;
        }

        private void uploadData() throws IOException {
            File[] watchLog = getAllFile(IOLogData.dataFolder);
            Log.v(TAG, "File num:" + watchLog.length);

            for (File file : watchLog) {
                // if wifi is disconnected, then terminate the upload thread
                if (!checkWifiOnAndConnected(myWatchFace) || !isPlugged(myWatchFace)) {
                    Log.v(TAG, "Disconnected, no wifi or not plugged");
                    break;

                }
//                try {
//                    if(socket==null||socket.isClosed()){
//                        socket = new Socket(ip,1880);
//                    }
//                }catch (Exception e){
//                    Log.e(TAG, "socket connect error", e);
//                }
//                // 获取Socket的OutputStream对象用于发送数据。
//                OutputStream outputStream = socket.getOutputStream();
                String localName = file.getName();
                String fileDate = localName.substring(0, 10);
                String s3FileName;
//                if (localName.charAt(localName.length() - 1) == '-') { // Label file
//                    s3FileName = fileDate+"-"+localName.substring(11);
//                } else { // Data file
//                    String sensorType = localName.substring(localName.length() - 3, localName.length());
//                    s3FileName = fileDate+"-"+sensorType+"-"+localName.substring(11,localName.length() - 4);
//                }
//                outputStream.write((s3FileName+spilt).getBytes());
//
//                //outputStream.flush();
//                InputStream inputStream = new FileInputStream(file);
//                byte buffer[] = new byte[1024*64];
//                int temp = 0;
//                // 循环读取文件
//                while ((temp = inputStream.read(buffer)) != -1) {
//                    // 把数据写入到OuputStream对象中
//                    outputStream.write(buffer, 0, temp);
//
//                }
//                outputStream.flush();
//                inputStream.close();
//                outputStream.close();
//                socket.close();
//                file.delete();
                if (localName.charAt(localName.length() - 1) == '-') { // Label file
                    s3FileName = "rpi3/"+fileDate+"/"+localName.substring(11);
                } else { // Data file
                    String sensorType = localName.substring(localName.length() - 3, localName.length());
                    s3FileName = "rpi3/"+fileDate+"/"+sensorType+"-"+localName.substring(11,localName.length() - 4);
                }
//                if (localName.charAt(localName.length() - 1) == '-') { // Label file
//                    s3FileName = "rpi3" + "/" + sensorType + Build.SERIAL +
//                            "/" + fileDate + "/" + file.getName().substring(11);
//                } else { // Data file
//                    String sensorType = localName.substring(localName.length() - 3, localName.length());
//                    s3FileName = Build.DEVICE + "/" + "WT-" + Build.SERIAL +
//                            "/" + fileDate + "/" + sensorType + "/" + localName.substring(11);
//                }
                // check if the file exists on the S3,
                 //if exits, delete the local file, continue next loop
                try {
                    if (s3Client.doesObjectExist(AwsS3.BUCKET_NAME, s3FileName)) {
                        Log.v(TAG, file.getName() + " exist.");
                        file.delete();
                        continue;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Check file error. Please check Internet permission ", e);
                }

                boolean fileUploadFlag = false;
                for (int i = 0; i < 3 && !fileUploadFlag; i++) {
                    // Uploading the file
                    Log.v(TAG, file.getName() + " uploading...");
                    try {
                        s3Client.putObject(AwsS3.BUCKET_NAME, s3FileName, file);
                        fileUploadFlag = true;
                    } catch (AmazonClientException exception) {
                        Log.v(TAG, "Uploading time out...", exception);
                        fileUploadFlag = false;
                    }
                }

            }
        }

        private void uploadLog() throws IOException {
            File[] watchLog = getAllFile(IOLogData.logFolder);
            Log.v(TAG, "File num:" + watchLog.length);
            for (File file : watchLog) {
                // if wifi is disconnected, then terminate the upload thread
                if (!checkWifiOnAndConnected(myWatchFace) || !isPlugged(myWatchFace)) {
                    Log.v(TAG, "Disconnected, no wifi or not plugged");
                    break;

                }
//                try {
//                    if(socket==null||socket.isClosed()){
//                        socket = new Socket(ip,1880);
//                    }
//                }catch (Exception e){
//                    Log.e(TAG, "socket connect error", e);
//                }
//                 //获取Socket的OutputStream对象用于发送数据。
//                OutputStream outputStream = socket.getOutputStream();
                String localName = file.getName();
                String fileDate = localName.substring(0, 10);
                //String s3FileName = fileDate + "-" + "wat-"+file.getName();
//                outputStream.write((s3FileName+spilt).getBytes());
//                InputStream inputStream = new FileInputStream(file);
//                byte buffer[] = new byte[4 * 1024];
//                int temp = 0;
//                // 循环读取文件
//                while ((temp = inputStream.read(buffer)) != -1) {
//                    // 把数据写入到OuputStream对象中
//                    outputStream.write(buffer, 0, temp);
//                }
//                outputStream.flush();
//                inputStream.close();
//                outputStream.close();
//                socket.close();
//
//                file.delete();
                String s3FileName = "rpi3" + "/"+fileDate + "/" + "wat-"+file.getName();
//                String s3FileName = "rpi3" + "/" + "wat-" + Build.SERIAL +
//                        "/" + fileDate + "/" + file.getName().substring(11);
                 //Uploading the log file
                boolean fileUploadFlag = false;
                for (int i = 0; i < 3 && !fileUploadFlag; i++) {
                    // Uploading the file
                    Log.v(TAG, file.getName() + " uploading...");
                    try {
                        s3Client.putObject(AwsS3.BUCKET_NAME, s3FileName, file);
                        fileUploadFlag = true;
                    } catch (AmazonClientException exception) {
                        Log.v(TAG, "Uploading time out...", exception);
                        fileUploadFlag = false;
                    }
                }


                // Delete local Log file before today
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

                    Date currentDate = sdf.parse(sdf.format(new Date(System.currentTimeMillis())));
                    Date fDate = sdf.parse(fileDate);
                    if (currentDate.after(fDate)) {
                        file.delete();
                        Log.v(TAG, file.getName() + " Old log deleted");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }

    }
}



