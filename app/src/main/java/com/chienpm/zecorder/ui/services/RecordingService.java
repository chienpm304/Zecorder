package com.chienpm.zecorder.ui.services;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.ToggleButton;

import com.chienpm.zecorder.ui.activities.DummyActivity;
import com.chienpm.zecorder.ui.activities.RecordScreenActivity;
import com.chienpm.zecorder.ui.utils.UiUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RecordingService extends Service {
    private final IBinder mIBinder = new RecordingBinder();

    private static final String TAG = "chienpm";
//    private static final int PERMISSION_RECORD_DISPLAY = 1;
    private static final List<Resolution> RESOLUTIONS = new ArrayList<Resolution>() {{
        add(new Resolution(640,360));
        add(new Resolution(960,540));
        add(new Resolution(1366,768));
        add(new Resolution(1600,900));
    }};
    private int mScreenDensity;
    private MediaProjectionManager mProjectionManager;
    private int mDisplayWidth;
    private int mDisplayHeight;
    private boolean mScreenSharing;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
//    private Surface mSurface;
//    private SurfaceView mSurfaceView;
//    private ToggleButton mToggleButton;
    private MediaProjectionCallback mMediaProjectionCallback;
    private MediaRecorder mMediaRecorder;
    WindowManager mWindowManager;
    private boolean mIsRecording = false;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private Resolution mResolution;


    public void notifyRequestProjectionData(Intent data, int resultCode) {
        if(data!=null){
            mMediaProjectionCallback = new MediaProjectionCallback();
            mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
            mMediaProjection.registerCallback(mMediaProjectionCallback, null);
//            mVirtualDisplay = createVirtualDisplay();
//            mMediaRecorder.start();
            Log.d(TAG, "notifyRequestProjectionData: DONE");
        }
        else{
            if(resultCode == UiUtils.PERMISSION_SCREEN_SHARE_DENIED)
                Log.d(TAG, "notifyRequestProjectionData: PERMISSION SHARE SCREEN DENIED");
            Log.d(TAG, "notifyRequestProjectionData: FAILED");
        }
    }

    public void prepareToRecording() {
        initRecorder();
        shareScreen();
    }

    public void startRecording(){
        mIsRecording = true;
        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();
    }

    public void stopRecording(){
        mMediaRecorder.stop();
        mMediaRecorder.reset();
        Log.v(TAG, "Stopping Recording");
        stopScreenSharing();
    }


    public class RecordingBinder extends Binder{
        public RecordingService getService(){
            return RecordingService.this;
        }

    }
    public RecordingService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mIBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        DisplayMetrics metrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(metrics);

        mMediaRecorder = new MediaRecorder();

        mScreenDensity = metrics.densityDpi;

        mProjectionManager =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        //TOdo: chooose resolution and orientation
        mResolution = RESOLUTIONS.get(1);
        mDisplayWidth = mResolution.y;
        mDisplayHeight = mResolution.x;
    }




    private void stopScreenSharing() {
        //Todo: Save file here
        mScreenSharing = false;
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        destroyMediaProjection();
    }

    private void initRecorder() {
        try {
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mMediaRecorder.setOutputFile(Environment
                    .getExternalStoragePublicDirectory(Environment
                            .DIRECTORY_DOWNLOADS) + "/video1.mp4");
            mMediaRecorder.setVideoSize(mDisplayWidth, mDisplayHeight);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mMediaRecorder.setVideoEncodingBitRate(512 * 1000);
            mMediaRecorder.setVideoFrameRate(30);
            int rotation = mWindowManager.getDefaultDisplay().getRotation();
            int orientation = ORIENTATIONS.get(rotation + 90);
            mMediaRecorder.setOrientationHint(orientation);
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void shareScreen() {
        mScreenSharing = true;
        if (mMediaProjection == null) {
            startDummyActiviyForResult();
            return;
        }

    }

    private void startDummyActiviyForResult() {
        Intent intent = new Intent(getApplicationContext(), DummyActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(Intent.EXTRA_INTENT, mProjectionManager.createScreenCaptureIntent());

        startActivities(new Intent[]{intent});
//        getApplicationContext().startActivityForResult(mProjectionManager.createScreenCaptureIntent(), PERMISSION_RECORD_DISPLAY);
    }


    private VirtualDisplay createVirtualDisplay() {
        return mMediaProjection.createVirtualDisplay("ScreenSharingDemo",
                mDisplayWidth, mDisplayHeight, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mMediaRecorder.getSurface(), null /*Callbacks*/, null /*Handler*/);
    }

    private void resizeVirtualDisplay() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.resize(mDisplayWidth, mDisplayHeight, mScreenDensity);
    }

    private void destroyMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection.unregisterCallback(mMediaProjectionCallback);
            mMediaProjection.stop();
            mMediaProjection = null;
        }
        Log.i(TAG, "MediaProjection Stopped");
    }

    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            if (mIsRecording) {
                mIsRecording = false;
                mMediaRecorder.stop();
                mMediaRecorder.reset();
                Log.v(TAG, "Recording Stopped");
            }
            mMediaProjection = null;
            stopRecording();
        }
    }

    private static class Resolution {
        int x;
        int y;
        public Resolution(int x, int y) {
            this.x = x;
            this.y = y;
        }
        @Override
        public String toString() {
            return x + "x" + y;
        }
    }

}
