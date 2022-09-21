package com.example.ruixuandai.zenfacedigit;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;


public class ActRecognitionService extends Service {

    public static final String TAG = "ActRecognitionService";

    private static final int REQUEST_OAUTH_REQUEST_CODE = 1;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    static final long DETECTION_INTERVAL_IN_MILLISECONDS = 180*1000;//120 * 1000; // 120 seconds

    private static MyWatchFace myWatchFace;

    private static ActivityRecognitionClient mActivityRecognitionClient;
    private PendingIntent actRegIntent;

    @Override
    public void onCreate(){
        Log.d(TAG, "onCreate executed");
        mActivityRecognitionClient = new ActivityRecognitionClient(this);



    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        Log.d(TAG, "onStartCommand() executed");

        actRegIntent = getActivityDetectionPendingIntent();

        try{
        Task<Void> task = mActivityRecognitionClient.requestActivityUpdates(
                DETECTION_INTERVAL_IN_MILLISECONDS,
                actRegIntent);

        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "AR Register succeeded");
            }
        });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "AR Register failed");
            }
        });}catch (Exception e){
            Log.e(TAG, "No google play service");
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        stopActRecognition();
        super.onDestroy();

        Log.d(TAG, "onDestroy() executed");
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    /**
     * Gets a PendingIntent to be sent for each activity detection.
     */
    private PendingIntent getActivityDetectionPendingIntent() {
        Intent intent = new Intent(this, DetectedActivitiesIntentService.class);

        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // requestActivityUpdates() and removeActivityUpdates().
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }


    public void stopActRecognition(){
        Task<Void> task = mActivityRecognitionClient.removeActivityUpdates(actRegIntent);

        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "Remove succeeded");
            }
        });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "Remove failed");
            }
        });
    }

}
