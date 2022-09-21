package com.example.ruixuandai.zenfacedigit;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.PowerManager;
import android.util.Log;

import java.nio.ByteBuffer;


/**
 * Created by ruixuandai on 11/20/17.
 *
 * including save data
 *
 */

public class OneValueSensorListener implements SensorEventListener {

    private static final String TAG = "IMUSensorListener";

    String sensorName;

    PowerManager.WakeLock wakeLock;
    MyWatchFace myWatchFace;
    LogService mLogService;

    int bufferSize;


    long startEventTime;
    private long startSensorTime;
    private long lastSampleTime;

    int samples = 0;


    private ByteBuffer bufferX;
    private ByteBuffer eventTimeBuffer;


    float x;

    public OneValueSensorListener(LogService mLogService, PowerManager.WakeLock wakeLock, String sensorName, int bufferSize){
        this.mLogService = mLogService;
        this.wakeLock = wakeLock;
        this.sensorName = sensorName;
        this.bufferSize = bufferSize;
        bufferX = ByteBuffer.allocate(bufferSize*4);
        eventTimeBuffer = ByteBuffer.allocate(bufferSize*8);

        Log.v(TAG,"new listener"+sensorName);
    }



    @Override
    public void onSensorChanged(SensorEvent event) {



        x = event.values[0];


        if(IMUSensorListener.startEventTime < 0 ){
            IMUSensorListener.startEventTime = System.currentTimeMillis();
            IMUSensorListener.startSensorTime = event.timestamp;
            startEventTime = IMUSensorListener.startEventTime;
            startSensorTime = IMUSensorListener.startSensorTime;
        }
        else{
            startEventTime = IMUSensorListener.startEventTime;
            startSensorTime = IMUSensorListener.startSensorTime;
        }



        if(bufferX.remaining()>0){
            samples++;

            bufferX.putFloat(event.values[0]);
            eventTimeBuffer.putLong(startEventTime + (event.timestamp - startSensorTime)/1000000);
            lastSampleTime = event.timestamp;
        }
        else // samples == bufferSize, save the data
        {
            saveData();
            startEventTime = System.currentTimeMillis();
            startSensorTime = event.timestamp;


            bufferX.putFloat(event.values[0]);
            eventTimeBuffer.putLong(startEventTime + (event.timestamp - startSensorTime)/1000000);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void saveData(){
        eventTimeBuffer.position(0);
        long firstSampleTime=eventTimeBuffer.getLong();


        byte[] dataX = new byte[samples*4]; //x,y,z: 4 byte.  timestamp: 8 byte
        byte[] dataTime = new byte[samples*8]; //x,y,z: 4 byte.  timestamp: 8 byte

        bufferX.position(0);
        eventTimeBuffer.position(0);

        bufferX.get(dataX);
        eventTimeBuffer.get(dataTime);

        byte[][] accLog = {dataX,dataTime};

        String fileName = IOLogData.dateFormatter.format(firstSampleTime)+"-"+samples+"-"+sensorName;
        IOLogData.writeData(fileName, accLog, false);

        bufferX.clear();
        eventTimeBuffer.clear();
        samples = 1;
    }
}
