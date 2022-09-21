/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.ruixuandai.zenfacedigit;

import android.Manifest;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;

import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;


import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;
import android.widget.Toast;


import java.lang.ref.WeakReference;

import java.util.Calendar;
import java.util.Date;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;







/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 *
 *  Author: Ruixuan Dai,
 *  CPSL, WashU
 *
 * ToDo:
 * 1, restore the sensor timestamp
 * 2, MQTT protocol √
 * 3, Charging detection, auto switch WiFi√
 * 4, Read file list √
 * 5, AWS s3 to store file√
 * 6, Merge Assessment Activity


 *
 * Bugs:
 * 1, Sampling rate will double while interacting with the watch
 * 2, √  Disconnect will restart app (occasionally)
 * 3, √  The logService Thread may start twice
 * 4, √  12-22 11:54:29.055 13505-13564/com.example.ruixuandai.zenfacedigit E/AndroidRuntime: FATAL EXCEPTION: Thread-5
         Process: com.example.ruixuandai.zenfacedigit, PID: 13505
         com.amazonaws.AmazonClientException: Unable to execute HTTP request: timeout
         at com.amazonaws.http.AmazonHttpClient.executeHelper(AmazonHttpClient.java:441)
         at com.amazonaws.http.AmazonHttpClient.execute(AmazonHttpClient.java:212)
         at com.amazonaws.services.s3.AmazonS3Client.invoke(AmazonS3Client.java:4643)
         at com.amazonaws.services.s3.AmazonS3Client.putObject(AmazonS3Client.java:1747)
         at com.amazonaws.services.s3.AmazonS3Client.putObject(AmazonS3Client.java:1553)
         at com.example.ruixuandai.zenfacedigit.ChargingState$UploadThread.run(ChargingState.java:176)
         Caused by: java.net.SocketTimeoutException: timeout

 *
 *
 * V0.1.5 save the raw data into binary
 * V0.1.6 save raw data in xxxxxxxxx......yyyyyyy.........zzzzzz.........
 * V0.1.7 save raw data in xxxxxxxxx......yyyyyyy.........zzzzzz.........tttttt
 *        and show the log status
 * V0.1.8 use System.currentTimeMillis() as the sensor timestamp. This is actually the listener event
 *        timestamp, not the timestamp when the value is generated. event.timestamp is device-depended.
 * V0.2.0 Log the data in a service in the background
 * V0.2.1 Trace the time skew, service in foreground (Still stopped while idle?)
 * V0.2.2 Add wakelock
 * V0.2.3 Service IBinder implemented to update the notification
 * V0.2.3b Wakelock in onAmbientModeChanged, set the event delay in sensor register
 * V0.2.4 Local log for debug(to do), set high priority to the service thread, replace the wakeLock in onSensorChanged()
 * V0.3.0 Local data log works without sleeping!!! :-) Known bug: Sampling rate will double while interacting with the watch
 * V0.3.1 Rewrite sensorListener. \
 * V1.0.0 First edition for log function
 * V1.0.1 MQTT implemented
 * V1.0.2 Charging detection by Intent
 * V1.0.3 Auto turn on WiFi when charging
 * V1.1.0 First edition with S3 service. Rewrite log service thread(sometimes, the log thread may start twice).
 * V1.1.1 Improve stability and file structure on S3
 * V1.1.2 Bug fixes
 * V2.0.0RC First edition for log and uploading
 * V2.0.1 With touch survey
 * V2.0.2 Auto request STORAGE permission, Redraw WatchFace
 * V2.0.3 Battery on storage log
 * V2.0.4 Add wifiLock for uploading
 * V2.1.0 Add offBody sensor to auto register the IMU sensor
 */


public class MyWatchFace extends CanvasWatchFaceService  {

    private static final String TAG = "MyWatchFace";
    private static final String version = "2.1.0-HIT";

    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    private LogService.MyBinder myBinder = null;
    boolean connectFlag = false;
    Intent logIntent;
    Intent arIntent;
    LogService mLogService = null;
    ChargingState chargingState = null;


    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.v(TAG,"service IPC connecting...");
            myBinder = (LogService.MyBinder) service;
            mLogService = (LogService) myBinder.getService();
            connectFlag = true;
            myBinder.setMyWatchFace(MyWatchFace.this);
            Log.v(TAG,"service IPC connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            connectFlag = false;
            mLogService = null;
        }
    } ;



    @Override
    public Engine onCreateEngine() {
        Log.v(TAG,"face start");

        //request  permission
        if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED ){


            Toast.makeText(MyWatchFace.this, "Permission Needed!!!", Toast.LENGTH_SHORT).show();
            Log.v(TAG,"Permission Needed!!!" );

            boolean permissionFlag = false;
            startActivity(new Intent(this, PermissionActivity.class));
//            while(!permissionFlag){
//
//            }

//            new  AsyncTask<Void, Void, Boolean>() {
//
//                @Override
//                protected Boolean doInBackground(Void... params) {
//                    PermissionResponse response = null;
//                    try {
//                        response = PermissionEverywhere.getPermission(getApplicationContext(),
//                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                                        Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.BODY_SENSORS},
//                                123, "Permission Request", "This app needs storage and sensor permission", R.mipmap.ic_launcher).call();
//                        e.printStackTrace();
//                    }
//
//                    boolean isGranted = response.isGranted();
//
//                    return isGranted;
//                }
//
//                @Override
//                protected void onPostExecute(Boolean aBoolean) {
//                    super.onPostExecute(aBoolean);
//                    Toast.makeText(MyWatchFace.this, "STORAGE is Granted " + aBoolean, Toast.LENGTH_SHORT).show();
//                }
//            }.execute();
        }




        //start and bind the IMU Log service
        logIntent = new Intent(this, LogService.class);
        startService(logIntent);
        Log.v(TAG,"binding LogServive");
        this.getApplicationContext().bindService(logIntent, connection,  Context.BIND_AUTO_CREATE);

        //start act recognition
        arIntent = new Intent(this, ActRecognitionService.class);
        startService(arIntent);



        // Register the charging receiver
        chargingState = new ChargingState(this);
        registerReceiver(chargingState, new IntentFilter(Intent.ACTION_POWER_DISCONNECTED));
        registerReceiver(chargingState, new IntentFilter(Intent.ACTION_POWER_CONNECTED));
        //Check the charging status when the APP first start
        chargingState.onReceive(this,null);



        return new Engine();
    }




    private static class EngineHandler extends Handler {
        private final WeakReference<MyWatchFace.Engine> mWeakReference;

        public EngineHandler(MyWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            MyWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        final Handler mUpdateTimeHandler = new EngineHandler(this);
        boolean mRegisteredTimeZoneReceiver = false;
        Paint mBackgroundPaint;
        Paint mTextPaint;
        Paint mTextAcc;
        Paint mTextDate;

        boolean mAmbient;
        Calendar mCalendar;
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };
        float mXOffset;
        float mYOffset;





        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);



            setWatchFaceStyle(new WatchFaceStyle.Builder(MyWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());
            Resources resources = MyWatchFace.this.getResources();
            mYOffset = resources.getDimension(R.dimen.digital_y_offset);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.background));

            mTextPaint = new Paint();
            mTextPaint = createTextPaint(resources.getColor(R.color.digital_text));

            mTextAcc = new Paint();
            mTextAcc = createTextPaint(resources.getColor(R.color.digital_text));

            mTextDate = new Paint();
            mTextDate = createTextPaint(resources.getColor(R.color.date_text));


            mCalendar = Calendar.getInstance();

        }



        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            MyWatchFace.this.unregisterReceiver(chargingState);
            stopService(logIntent);
            super.onDestroy();
        }

        private Paint createTextPaint(int textColor) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(NORMAL_TYPEFACE);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            MyWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            MyWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = MyWatchFace.this.getResources();
            boolean isRound = insets.isRound();
            mXOffset = resources.getDimension(isRound
                    ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);

            float textSize = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);

            mTextPaint.setTextSize(textSize);
            mTextAcc.setTextSize(resources.getDimension(R.dimen.acc_text_size));
            mTextDate.setTextSize(resources.getDimension(R.dimen.date_text_size));
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);

            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mTextPaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    // TODO: Add code to handle the tap gesture.
//                    Toast.makeText(getApplicationContext(), R.string.message, Toast.LENGTH_SHORT)
//                            .show();
                    Log.e(TAG,getPackageName());
                    Intent launchIntent = getPackageManager().getLaunchIntentForPackage("ruixuandai.cpsl.wustl.survey");

                    if (launchIntent != null) {
                        Log.e(TAG,"find ruixuandai.cpsl.wustl.survey");
                            startActivity(launchIntent);//null pointer check in case package name was not found
                    }
                    else{
                        Toast.makeText(MyWatchFace.this, "Survey not installed...", Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {

            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
            }

            String batterLevel = String.format("%.02f",ChargingState.getBatteryLevel()*100)+"%";
            canvas.drawText(batterLevel,mXOffset+100,mYOffset-115,mTextAcc);

            // Draw H:MM in ambient mode or H:MM:SS in interactive mode.
            //formatter3 : "yyyy-MM-dd_HH:mm:ss"
            long now = System.currentTimeMillis();
            String timeText = IOLogData.formatter3.format(now);
            // Draw Date
            canvas.drawText(timeText.substring(0,10), mXOffset+15,mYOffset-75,mTextDate);
            String timeDisplay = mAmbient?timeText.substring(11,16):timeText.substring(11,19);
            // Draw Time
            canvas.drawText(timeDisplay, mXOffset, mYOffset, mTextPaint);





            canvas.drawText("Version:" + version, mXOffset + 20, mYOffset + 60, mTextAcc);


            // update notification
            if(connectFlag&&LogService.accListener!=null){
                canvas.drawText("Accelerations: " +
                        String.format("%.02f",LogService.accListener.x) + " " +
                        String.format("%.02f",LogService.accListener.y) + " " +
                        String.format("%.02f",LogService.accListener.z), mXOffset,mYOffset+30,mTextAcc);
                canvas.drawText("Gyroscope: " +
                        String.format("%.02f",LogService.gyroListener.x) + " " +
                        String.format("%.02f",LogService.gyroListener.y) + " " +
                        String.format("%.02f",LogService.gyroListener.z), mXOffset,mYOffset+45,mTextAcc);
                canvas.drawText("Last Save time:"
                        +IOLogData.formatter2.format(new Date(LogService.accListener.saveTime)),mXOffset,mYOffset+75,mTextAcc);
                canvas.drawText(String.format("Buffer: %.2f", LogService.accListener.getBufferPercentage()),mXOffset,mYOffset+90,mTextAcc);
            }


        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }
}
