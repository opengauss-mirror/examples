package com.example.ruixuandai.zenfacedigit;

import android.content.Intent;
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


    static public long startEventTime = -1l;
    static public long startSensorTime;

    public long saveTime;


    int samples = 0;


    private ByteBuffer bufferX;
    private ByteBuffer bufferY;
    private ByteBuffer bufferZ;
    private ByteBuffer eventTimeBuffer;


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
        eventTimeBuffer = ByteBuffer.allocate(bufferSize*8);

        Log.v(TAG,"new listener"+sensorName);
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


        //static sensor time reference
        if(startEventTime <0 ){
            startEventTime = System.currentTimeMillis();
            startSensorTime = event.timestamp;
        }




        if(bufferX.remaining()>0){
            samples++;

            bufferX.putFloat(event.values[0]);
            bufferY.putFloat(event.values[1]);
            bufferZ.putFloat(event.values[2]);
            eventTimeBuffer.putLong(startEventTime + (event.timestamp - startSensorTime)/1000000);
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
        }
        else // samples == bufferSize, save the data
        {
            saveData();

            //Log the battery usage
            if(sensorName.equals("acc"))
                IOLogData.writeLog(TAG,"battery: " + ChargingState.getBatteryLevel());



            bufferX.putFloat(event.values[0]);
            bufferY.putFloat(event.values[1]);
            bufferZ.putFloat(event.values[2]);
            eventTimeBuffer.putLong(startEventTime + (event.timestamp - startSensorTime)/1000000);
            //Log.e("",startEventTime + (event.timestamp - startSensorTime)/1000000+"");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void saveData(){

        saveTime = System.currentTimeMillis();

        eventTimeBuffer.position(0);
        long firstSampleTime=eventTimeBuffer.getLong();

        byte[] dataX = new byte[samples*4]; //x,y,z: 4 byte.  timestamp: 8 byte
        byte[] dataY = new byte[samples*4]; //x,y,z: 4 byte.  timestamp: 8 byte
        byte[] dataZ = new byte[samples*4]; //x,y,z: 4 byte.  timestamp: 8 byte
        byte[] dataTime = new byte[samples*8]; //x,y,z: 4 byte.  timestamp: 8 byte

        bufferX.position(0);
        bufferY.position(0);
        bufferZ.position(0);
        eventTimeBuffer.position(0);


        bufferX.get(dataX);
        bufferY.get(dataY);
        bufferZ.get(dataZ);
        eventTimeBuffer.get(dataTime);

        byte[][] accLog = {dataX,dataY,dataZ,dataTime};

        String fileName = IOLogData.dateFormatter.format(firstSampleTime)+"-"+samples+"-"+sensorName;
        boolean wrtResult = IOLogData.writeData(fileName, accLog, false);
        if(!wrtResult){
            mLogService.startActivity(new Intent(mLogService.myWatchFace, PermissionActivity.class));
        }

        bufferX.clear();
        bufferY.clear();
        bufferZ.clear();
        eventTimeBuffer.clear();
        samples = 1;
    }
}
