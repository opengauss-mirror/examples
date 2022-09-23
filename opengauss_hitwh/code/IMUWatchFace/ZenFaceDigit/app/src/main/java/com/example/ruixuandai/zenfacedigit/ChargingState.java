package com.example.ruixuandai.zenfacedigit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.util.Log;

import com.amazonaws.AmazonClientException;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferType;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;

import java.io.File;
import java.util.List;


/**
 * Created by ruixuandai on 11/26/17.
 */

public class ChargingState extends BroadcastReceiver {
    private static final String TAG = "ChargingState";
    private static MyWatchFace myWatchFace;
    static AwsIoTMQTT mqtt;


    static AmazonS3Client s3Client;
    static UploadThread uThread = null;
    private static boolean wifiSwitch = false;
    static WifiManager wifi;
    static WifiManager.WifiLock wifiLock;



    public ChargingState(MyWatchFace myWatchFace){
        this.myWatchFace = myWatchFace;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        boolean isCharging = isPlugged(myWatchFace);
        Log.e(TAG,"isCharging?:" + isCharging);


        wifi=(WifiManager)myWatchFace.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiLock = wifi.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF,TAG);
        if(getAllLog() == null)
            return;

        if(isCharging){
            wifi.setWifiEnabled(true);//Turn on Wifi

            if(!wifiLock.isHeld())
                wifiLock.acquire();


            if(uThread == null ) {
                uThread = new UploadThread();
                uThread.start();
            }
            else if (!uThread.isAlive()){
                uThread = new UploadThread();
                uThread.start();
            }



        }else{


            if(wifiLock.isHeld())
                wifiLock.release();
            wifi.setWifiEnabled(!wifiSwitch);//Turn off Wifi

        }

    }
    public static boolean checkWifiOnAndConnected(Context context) {
        WifiManager wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        if (wifiMgr.isWifiEnabled()) { // Wi-Fi adapter is ON

            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();

            if( wifiInfo.getNetworkId() == -1 ){
                return false; // Not connected to an access point
            }
            return true; // Connected to an access point
        }
        else {
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

    public static float getBatteryLevel(){
        Intent batteryStatus = myWatchFace.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        float batteryPct = level / (float)scale;
        return batteryPct;
    }

    private File[] getAllLog(){
        String path = IOLogData.dataFolder;
        Log.d(TAG, "Path: " + path);
        File directory = new File(path);
        File[] files = directory.listFiles();
        if (files != null)
            Log.d(TAG, "Size: "+ files.length);

        return files;
    }



    public class UploadThread extends Thread{
        public void run(){
            setName("Uploading");
            // Wait the wifi connection
            while (!checkWifiOnAndConnected(myWatchFace))
            {
                Log.v(TAG,"Waiting for wifi");
                try {
                    Thread.sleep(1000L); // wait 1s
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if(!isPlugged(myWatchFace)) { // if it is not charging, terminate the thread

                    Log.v(TAG," Unplugged!");
                    wifi.setWifiEnabled(!wifiSwitch);//Turn off Wifi
                    return;
                }
            }

            try {
                // initiate S3
                if (s3Client == null) {
                    s3Client = AwsS3.getS3Client(myWatchFace);
                }
                // initiate MQTT
//                if (mqtt == null)
//                    mqtt = new AwsIoTMQTT(myWatchFace);
//                else if (mqtt.connectionStatus == -1)
//                    mqtt.connect();
            }catch (Exception e){
                Log.e(TAG,"AWS connection error",e);
            }

            // wait mqtt connection
            try {
                Thread.sleep(5000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


//            mqtt.publishString("charging:"+System.currentTimeMillis(),"zenwatch");



            // check the S3 bucket name exists
            try {
                if (!s3Client.doesBucketExist(AwsS3.BUCKET_NAME))
                    s3Client.createBucket(AwsS3.BUCKET_NAME);
            } catch (AmazonClientException exception){
                Log.v(TAG,"Check bucket error...",exception);
            }


            File[] watchLog = getAllLog();
            Log.v(TAG,"File num:" + watchLog.length);


            for(File file : watchLog) {

                // if wifi is disconnected, then terminate the upload thread
                if(!checkWifiOnAndConnected(myWatchFace)||!isPlugged(myWatchFace)){
                    Log.v(TAG,"Disconnected, no wifi or not plugged");
                    break;

                }


                String localName = file.getName();
                String fileUnixTime = localName.substring(0,10);
                String s3FileName;

                if(localName.charAt(localName.length()-1) == '-'){ // Label file
                    s3FileName = Build.DEVICE + "/" + Build.SERIAL +
                            "/" + fileUnixTime + "/" + file.getName().substring(11);
                }else { // Data file
                    String sensorType = localName.substring(localName.length()-3, localName.length());
                    s3FileName = Build.DEVICE + "/" + Build.SERIAL +
                            "/" + fileUnixTime + "/" + sensorType + "/" + localName.substring(11);
                }



                // check if the file exists on the S3,
                // if exits, delete the local file, continue next loop
                try {
                    if (s3Client.doesObjectExist(AwsS3.BUCKET_NAME, s3FileName)) {
                        Log.v(TAG, file.getName() + " exist.");
                        file.delete();
                        continue;
                    }
                } catch (Exception e){
                    Log.v(TAG,"Check file error...",e);
                }


                // Uploading the file
                Log.v(TAG,file.getName() + " uploading...");
                try{
                    s3Client.putObject(AwsS3.BUCKET_NAME, s3FileName, file);
                }
                catch (AmazonClientException exception){
                    Log.v(TAG,"Uploading time out...",exception);
                }

                try {
                    Thread.sleep(500L); // wait uploading
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }


            Log.v(TAG,"Upload finished");
            s3Client.shutdown();
//            mqtt.mqttManager.disconnect();
            s3Client = null;
            mqtt = null;

            return;

        }
    }


}
