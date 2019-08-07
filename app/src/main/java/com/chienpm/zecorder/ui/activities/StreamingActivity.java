package com.chienpm.zecorder.ui.activities;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.chienpm.zecorder.R;
import com.chienpm.zecorder.controllers.streaming.StreamProfile;
import com.chienpm.zecorder.ui.services.recording.RecordingControllerService;
import com.chienpm.zecorder.ui.services.streaming.StreamingControllerService;
import com.chienpm.zecorder.ui.utils.FacebookUtils;
import com.chienpm.zecorder.ui.utils.MyUtils;
import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;

public class StreamingActivity extends AppCompatActivity {

    private static final String TAG = "chienpm_log_stream";
    private StreamProfile mStreamProfile;
    private View mViewRoot;
    private Button mBtnStartStream, mBtnEndStream;

    final boolean DEBUG = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_streaming);
        initViews();
        if(DEBUG){
            mStreamProfile = new StreamProfile("","","");
        }
        else {
            Intent i = getIntent();
            mStreamProfile = (StreamProfile) i.getSerializableExtra(MyUtils.STREAM_PROFILE);
        }

    }

    private void initViews() {
        mViewRoot = findViewById(R.id.mViewRoot);
        mBtnStartStream = findViewById(R.id.button_start_stream);
        mBtnEndStream = findViewById(R.id.button_end_stream);

        mBtnStartStream.setOnClickListener(mStartStreamListener);
        mBtnEndStream.setOnClickListener(mEndStreamListener);
        if(DEBUG)
            mBtnStartStream.performClick();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(DEBUG)
            return;
        if(FacebookUtils.getInstance().isSignedIn()){
            requestFullLiveProfile(AccessToken.getCurrentAccessToken());
        }
        else{
//            signIn();
//            toggleAccountProfileInfo(false);
            MyUtils.showSnackBarNotification(mViewRoot, "Authentication failed!", Snackbar.LENGTH_INDEFINITE);
        }
    }

    private void requestFullLiveProfile(AccessToken accessToken) {
        try {
            GraphRequest request = GraphRequest.newGraphPathRequest(
                    accessToken,
                    "/"+mStreamProfile.getId(),
//                    new JSONObject("{\"fields\":\"ingest_streams\"}"),
                    new GraphRequest.Callback() {
                        @Override
                        public void onCompleted(GraphResponse response) {
                            // Insert your code here
                            String newStreamUrl;
                            Log.d(TAG, "onCompleted full live obj: "+response.getRawResponse());
                            try {
                                newStreamUrl = (String) response.getJSONObject().getJSONArray("ingest_streams")
                                        .getJSONObject(0).get("stream_url");

                            } catch (JSONException e) {
                                e.printStackTrace();
                                Log.d(TAG, "PARSE JSON Response FAILED");
                                newStreamUrl = "";
                            }
                            if(!TextUtils.isEmpty(newStreamUrl)) {
                                Log.d(TAG, "updated streamUrl: "+newStreamUrl);
                                mStreamProfile.setStreamUrl(newStreamUrl);
                            }
                        }
                    });

            Bundle parameters = new Bundle();
            parameters.putString("fields", "ingest_streams");
            request.setParameters(parameters);
            request.executeAsync();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    private final View.OnClickListener mStartStreamListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(isMyServiceRunning(StreamingControllerService.class)){
                MyUtils.showSnackBarNotification(mViewRoot,"Streaming service is running!", Snackbar.LENGTH_LONG);
                return;
            }

            if(mScreenCaptureIntent == null || mScreenCaptureResultCode == MyUtils.RESULT_CODE_FAILED)
                requestScreenCaptureIntent();

            if(hasPermission()) {
                startStreamingControllerService();
            }
            else{
                requestPermissions();
                requestScreenCaptureIntent();
            }
            //start stream

            //update stream status


        }
    };

    private View.OnClickListener mEndStreamListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

        }
    };


    //    Copy from mainActivity
    private int mScreenCaptureResultCode = MyUtils.RESULT_CODE_FAILED;

    private static final int PERMISSION_REQUEST_CODE = 3004;
    private static final int PERMISSION_DRAW_OVER_WINDOW = 3005;
    private static final int PERMISSION_RECORD_DISPLAY = 3006;
    private static String[] mPermission = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private Intent mScreenCaptureIntent = null;

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            // PERMISSION DRAW OVER
            if(!Settings.canDrawOverlays(this)){
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, PERMISSION_DRAW_OVER_WINDOW);
            }
            ActivityCompat.requestPermissions(this, mPermission, PERMISSION_REQUEST_CODE);

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == PERMISSION_DRAW_OVER_WINDOW) {

            //Check if the permission is granted or not.
            if (resultCode != RESULT_OK) { //Permission is not available
                MyUtils.showSnackBarNotification(mViewRoot, "Draw over other app permission not available.",Snackbar.LENGTH_SHORT);
            }
        }
        else if( requestCode == PERMISSION_RECORD_DISPLAY) {
            if(resultCode != RESULT_OK){
                MyUtils.showSnackBarNotification(mViewRoot, "Recording display permission not available.",Snackbar.LENGTH_SHORT);
                mScreenCaptureIntent = null;
            }
            else{
                mScreenCaptureIntent = data;
                mScreenCaptureIntent.putExtra(MyUtils.SCREEN_CAPTURE_INTENT_RESULT_CODE, resultCode);
                mScreenCaptureResultCode = resultCode;

                shouldStartRecordingControllerSerivce();
            }
        }
        else{
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void shouldStartRecordingControllerSerivce() {
        if (hasPermission() && !isMyServiceRunning(RecordingControllerService.class)) {
            MyUtils.showSnackBarNotification(mViewRoot, "Permissions Granted!", Snackbar.LENGTH_SHORT);
            startStreamingControllerService();
        }
    }

    private void startStreamingControllerService() {

        Intent streamingControllerService = new Intent(StreamingActivity.this, StreamingControllerService.class);

        if(checkCameraHardware(this)){
//            streamingControllerService.setAction("Camera_Available");
        }

        streamingControllerService.putExtra(Intent.EXTRA_INTENT, mScreenCaptureIntent);
        Bundle bundle = new Bundle();
        bundle.putSerializable(MyUtils.STREAM_PROFILE, mStreamProfile);
        streamingControllerService.putExtras(bundle);

        startService(streamingControllerService);

//        finish();
    }

    private void requestScreenCaptureIntent() {
        if(mScreenCaptureIntent == null){
            MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), PERMISSION_RECORD_DISPLAY);
        }
    }


    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean hasPermission(){
        int granted = PackageManager.PERMISSION_GRANTED;

        return ContextCompat.checkSelfPermission(this, mPermission[0]) == granted
                && ContextCompat.checkSelfPermission(this, mPermission[1]) == granted
                && ContextCompat.checkSelfPermission(this, mPermission[2]) == granted
                && Settings.canDrawOverlays(this)
                && mScreenCaptureIntent != null
                && mScreenCaptureResultCode != MyUtils.RESULT_CODE_FAILED;
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
