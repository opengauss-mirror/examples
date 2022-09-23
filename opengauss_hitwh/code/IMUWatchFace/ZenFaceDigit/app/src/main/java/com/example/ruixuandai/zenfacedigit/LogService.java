package com.example.ruixuandai.zenfacedigit;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Process;
import android.support.v4.app.NotificationCompat;
import android.util.Log;


/**
 * Created by ruixuandai on 11/8/17.
 *
 * add support for offBody sensor
 *
 */



public class LogService extends Service implements SensorEventListener {
    public static final String TAG = "LogService";




    private MyBinder mBinder = new MyBinder();




    public static int samplingRate = 25; //Hz
    public static int notifUpdatePeriod = 10; //s
    public static int bufferSize = samplingRate*60*10; //10min


    private static SensorManager sManager;
    private static Sensor mSensorAccelerometer;
    private static Sensor mSensorGyroscope;

    public static boolean logState = false;
    Sensor mOffBody = null;

    public static IMUSensorListener accListener;
    public static IMUSensorListener gyroListener;
    public MyWatchFace myWatchFace= null;

    private static HandlerThread mSensorThread;
    private static Handler mSensorHandler;

    public static final String CHANNEL_IMU = "CHANNEL_IMU";
    private static final int notificationId = 001;
    private static final String NOTIFICATION_CHANNEL_ID = "001";
    private NotificationCompat.Builder mBuilder;
    NotificationManager notificationManager = null;

    PowerManager.WakeLock wakeLock = null;



    @Override
    public void onCreate() {
        super.onCreate();
        // Notification channel ID is ignored for Android 7.1.1
        // (API level 25) and lower.

        // Get an instance of the NotificationManager service
//        notificationManager =NotificationManagerCompat.from(this);
        notificationManager = getSystemService(NotificationManager.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
        {
            int importance = NotificationManager.IMPORTANCE_MIN;
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, CHANNEL_IMU, importance);
            notificationChannel.enableLights(false);
            notificationChannel.enableVibration(false);
            notificationChannel.setShowBadge(false);


            assert notificationManager != null;
            notificationManager.createNotificationChannel(notificationChannel);
        }


        sManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
        {
            mOffBody = sManager.getDefaultSensor(34,true);
            if(mOffBody == null)
                Log.e(TAG,"No OffBody sensor");
            sManager.registerListener(this, mOffBody, SensorManager.SENSOR_DELAY_NORMAL);
        }

        NotificationCompat.Builder initiate = setNotification("Sensor Log","Service Start");
        // Issue the notification with notification manager.

        startForeground(notificationId, initiate.build());


        Log.d(TAG, "onCreate() executed");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand() executed");




//        if (logState == false){
//            startSensors();
//        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSenors();
        Log.d(TAG, "onDestroy() executed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private NotificationCompat.Builder setNotification(String title, String text){
        // Build intent for notification content
        Intent viewIntent = new Intent(this, MyWatchFace.class);
        PendingIntent viewPendingIntent =
            PendingIntent.getActivity(this, 0, viewIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);


        mBuilder
                .setSmallIcon(R.drawable.smallic)
                .setContentTitle(title)
                .setContentText(text)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setContentIntent(viewPendingIntent);


        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
        {
            mBuilder.setChannelId(NOTIFICATION_CHANNEL_ID)
            .setOnlyAlertOnce(true);
        }



        return mBuilder;
    }

    /**
     * This is the method that can be called to update the Notification
     */
    public void updateNotification(String title, String textUpdate) {


        mBuilder = setNotification(title,textUpdate);



        notificationManager.notify(notificationId, mBuilder.build());


    }


    class MyBinder extends Binder {

        public Service getService(){
            return LogService.this; }
        public void setMyWatchFace(MyWatchFace mywatchFace){
            LogService.this.myWatchFace = mywatchFace;
        }

    }

    public void startSensors(){
        Log.v(TAG, "Sensor start");
        logState = true;
        PowerManager powerMgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerMgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        if(!wakeLock.isHeld()){
            wakeLock.acquire();
            Log.v(TAG,"acquiring wakeLock");
        }

        mSensorThread = new HandlerThread("Sensor thread", Process.THREAD_PRIORITY_BACKGROUND);
        mSensorThread.start(); //IMPORTANT: keep low priority not to block ambient package!????? Does not work
        mSensorHandler = new Handler(mSensorThread.getLooper()); //Blocks until looper is prepared, which is fairly quick

        sManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorAccelerometer = sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorGyroscope = sManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);


        // Sometimes the IMUListener may be registered twice
        if (gyroListener == null)
            gyroListener = new IMUSensorListener(this, wakeLock, "gyr", bufferSize);
        else {
            sManager.unregisterListener(gyroListener,mSensorGyroscope);
            gyroListener = new IMUSensorListener(this, wakeLock, "gyr",bufferSize);
        }

        if (accListener == null)
            accListener = new IMUSensorListener(this, wakeLock, "acc",bufferSize);
        else {
            sManager.unregisterListener(accListener,mSensorAccelerometer);
            accListener = new IMUSensorListener(this, wakeLock, "acc",bufferSize);
        }

        Log.v(TAG,"wakeup: "+mSensorAccelerometer.isWakeUpSensor()); //false
        Log.v(TAG,"Max delay: "+mSensorAccelerometer.getMaxDelay());  //1000000us
        Log.v(TAG,"Min delay: "+mSensorAccelerometer.getMinDelay()); //5000us
        Log.v(TAG,"Power: "+mSensorAccelerometer.getPower());     //0.13mA
        Log.v(TAG,"FIFO: "+mSensorAccelerometer.getFifoMaxEventCount()); //447
        Log.v(TAG,"Reserved FIFO: "+mSensorAccelerometer.getFifoReservedEventCount()); //0



        sManager.registerListener(gyroListener,mSensorGyroscope,1000000/samplingRate,
                1000000,mSensorHandler);
        sManager.registerListener(accListener,mSensorAccelerometer,1000000/samplingRate,
                1000000,mSensorHandler); //max delay: 20ms * 100 sample * 1000us/ms
        logState = true;
    }

    public void stopSenors(){
        Log.d(TAG,"Sensor stopped");
        logState = false;
        sManager.unregisterListener(gyroListener,mSensorGyroscope);
        sManager.unregisterListener(accListener,mSensorAccelerometer);
        mSensorThread.quitSafely();
        gyroListener.saveData();
        accListener.saveData();
        if(wakeLock.isHeld())
            wakeLock.release();
        gyroListener=null;
        accListener=null;
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.d(TAG,"OFFBody sensor:" + event.values[0]);


        if(event.values[0] < 1&&logState == true){
            stopSenors();
        }
        if(event.values[0] >0 &&logState == false){
            startSensors();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


}
