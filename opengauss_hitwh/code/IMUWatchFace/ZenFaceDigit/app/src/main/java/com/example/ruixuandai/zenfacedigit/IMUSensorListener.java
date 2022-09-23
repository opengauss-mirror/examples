package com.example.ruixuandai.zenfacedigit;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.PowerManager;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.Date;


/**
 * Created by ruixuandai on 11/20/17.
 *
 * including save data
 *
 */

public class IMUSensorListener implements SensorEventListener {

    private static final String TAG = "IMUSensorListener";

    String sensorName;

    PowerManager.WakeLock wakeLock;
    MyWatchFace myWatchFace;
    LogService mLogService;

    int bufferSize;


    long startEventTime;
    long startSensorTime;
    long lastSampleTime;

    int samples = 0;


    ByteBuffer bufferX;
    ByteBuffer bufferY;
    ByteBuffer bufferZ;
    ByteBuffer eventTime;
//    ByteBuffer sensorTime = ByteBuffer.allocate(bufferSize*8);


    float x;
    float y;
    float z;

    public IMUSensorListener(LogService mLogService, PowerManager.WakeLock wakeLock, String sensorName, int bufferSize){
        this.mLogService = mLogService;
        this.wakeLock = wakeLock;
        this.sensorName = sensorName;
        this.bufferSize = bufferSize;
        bufferX = ByteBuffer.allocate(bufferSize*4);
        bufferY = ByteBuffer.allocate(bufferSize*4);
        bufferZ = ByteBuffer.allocate(bufferSize*4);
        eventTime = ByteBuffer.allocate(bufferSize*8);
        
        Log.v(TAG,"new listener"+sensorName);
    }
    public IMUSensorListener(PowerManager.WakeLock wakeLock, String sensorName, MyWatchFace myWatchFace){
        this.wakeLock = wakeLock;
        this.sensorName = sensorName;
        this.myWatchFace = myWatchFace;
    }

    public float getBufferPercentage(){
        return (float)bufferX.position()/bufferX.capacity() * 100;
    }


    @Override
    public void onSensorChanged(SensorEvent event) {

        if(!wakeLock.isHeld()){
            wakeLock.acquire();
            Log.v(TAG,"acquiring lock");
        }


        x = event.values[0];
        y = event.values[1];
        z = event.values[2];

        // Update notification
        if(samples % (LogService.samplingRate*LogService.notifUpdatePeriod) == 0 &&
                event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            mLogService.updateNotification("Last Save time:\n"
                            +IOLogData.formatter2.format(new Date(startEventTime)),
                    String.format("Buffer: %.2f", (float)bufferX.position()/bufferX.capacity() * 100)  +"%\n");
        }


        if(samples == 0 ){
            startEventTime = System.currentTimeMillis();
            startSensorTime = event.timestamp;
        }



        if(bufferX.remaining()>0){
            samples++;

            bufferX.putFloat(event.values[0]);
            bufferY.putFloat(event.values[1]);
            bufferZ.putFloat(event.values[2]);
            eventTime.putLong(startEventTime + (event.timestamp - startSensorTime)/1000000);
//            sensorTime.putLong(event.timestamp);

//            if(samples%100 == 0){
//                Log.v(TAG,sensorName+" interval:"+((event.timestamp - lastSampleTime)/1000000) +"ms");
//                Log.v(TAG,"battery: " + ChargingState.getBatteryLevel());
//            }




//            Log.v(TAG,String.format("Buffer: %.0f", (float)bufferX.position()/bufferX.capacity() * 100)  +"%");

//            if(samples%100 == 0)
//                myWatchFace.mLogService.updateNotification(""+IOLogData.formatter2.format(new Date(myWatchFace.mLogService.accListener.startEventTime)),
//                        "bufferSize:" + myWatchFace.mLogService.accListener.samples +"\n");

//            if(samples%100 == 0){
//                Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
//                for (Thread t : threadSet){
//                    Log.v(TAG,t.getName());
//                }
//            }
            lastSampleTime = event.timestamp;
        }
        else // samples == bufferSize, save the data
        {

            startEventTime = System.currentTimeMillis();
            startSensorTime = event.timestamp;

            saveData();

            //Log the battery usage
            if(sensorName.equals("acc"))
                IOLogData.writeLog(TAG,"battery: " + ChargingState.getBatteryLevel());

            samples = 1;

            bufferX.putFloat(event.values[0]);
            bufferY.putFloat(event.values[1]);
            bufferZ.putFloat(event.values[2]);
            eventTime.putLong(startEventTime + (event.timestamp - startSensorTime)/1000000);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void saveData(){
        byte[][] accLog = {bufferX.array().clone(),bufferY.array().clone(),bufferZ.array().clone(),
                eventTime.array().clone()};

        String fileName = IOLogData.dateFormatter.format(startEventTime)+"-"+samples+"-"+sensorName;
        IOLogData.writeData(fileName, accLog, false);

        bufferX.clear();
        bufferY.clear();
        bufferZ.clear();
        eventTime.clear();
    }
}
