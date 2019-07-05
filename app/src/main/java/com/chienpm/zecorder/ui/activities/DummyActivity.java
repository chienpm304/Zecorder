package com.chienpm.zecorder.ui.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.projection.MediaProjectionManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.chienpm.zecorder.R;
import com.chienpm.zecorder.ui.services.RecordingService;
import com.chienpm.zecorder.ui.utils.UiUtils;

import java.nio.channels.InterruptedByTimeoutException;

public class DummyActivity extends AppCompatActivity {

    private static final int REQUEST_PROJECTION_DATA = 1020;
    private static final String TAG = "chienpm_";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_dummy);

        Intent mRecordingServiceIntent = new Intent(getApplicationContext(), RecordingService.class);
        bindService(mRecordingServiceIntent, mRecordingServiceConnection, Context.BIND_AUTO_CREATE);

        Intent intent = getIntent().getParcelableExtra(Intent.EXTRA_INTENT);
        if(intent != null){
            startActivityForResult(intent, REQUEST_PROJECTION_DATA);
        }
        else{
            mRecordingService.notifyRequestProjectionData(null,-1);
            finish();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_PROJECTION_DATA) {
            Log.e(TAG, "Unknown request code: " + requestCode);
            mRecordingService.notifyRequestProjectionData(null, -1);
            finish();
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(this,
                    "User denied screen sharing permission", Toast.LENGTH_SHORT).show();
            mRecordingService.notifyRequestProjectionData(null, UiUtils.PERMISSION_SCREEN_SHARE_DENIED);
            finish();
        }
        mRecordingService.notifyRequestProjectionData(data, resultCode);
        finish();
    }


    private RecordingService mRecordingService;
    private Boolean mRecordingServiceBound = false;
    private ServiceConnection mRecordingServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            RecordingService.RecordingBinder binder = (RecordingService.RecordingBinder) service;
            mRecordingService = binder.getService();
            mRecordingServiceBound = true;
            UiUtils.toast(getApplicationContext(), "Recording service connected", Toast.LENGTH_SHORT);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mRecordingServiceBound = false;
            UiUtils.toast(getApplicationContext(), "Recording service disconnected", Toast.LENGTH_SHORT);
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mRecordingService!=null && mRecordingServiceBound) {
            unbindService(mRecordingServiceConnection);
            mRecordingServiceBound = false;

        }
    }
}
