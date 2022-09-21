package com.example.ruixuandai.zenfacedigit;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.widget.TextView;

public class PermissionActivity extends WearableActivity {

    private TextView mTextView;
    private static final String TAG = "PermissionActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.permission_main);

        mTextView = (TextView) findViewById(R.id.text);

        // Enables Always-on
        setAmbientEnabled();


        Log.v(TAG, "Start requesting Permission");
        //request STORAGE permission
        if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.BODY_SENSORS}, 1);
        }

        //request Body sensor permission
        if(checkSelfPermission(Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED ){
            Log.v(TAG, "No Body Sensor permission");
            requestPermissions(new String[]{Manifest.permission.BODY_SENSORS}, 2);
        }

        finish();

    }
}
