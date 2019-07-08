package com.chienpm.zecorder.ui.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.WindowManager;

import com.chienpm.zecorder.ui.utils.UiUtils;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RecordingService extends IntentService {
    private final IBinder mIBinder = new RecordingBinder();

    private static final String TAG = "chienpm";
    private static final List<Resolution> RESOLUTIONS = new ArrayList<Resolution>() {{
        add(new Resolution(320,180, 30, 800*1000));
        add(new Resolution(640, 360, 30, 2*1000*1000));
        add(new Resolution(1280,720, 30, 4*1000*1000));
        add(new Resolution(1920, 1080, 30, 10*1000*1000));
    }};

    private int mScreenDensity;
    private MediaProjectionManager mProjectionManager;
    private int mDisplayWidth;
    private int mDisplayHeight;
    private boolean mScreenSharing;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
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
    private Intent mScreenCaptureIntent;
    private int mScreenCaptureResultCode;


    public RecordingService(){
        super(UiUtils.RECORDING_INTENT_SERVICE_NAME);
    }

    public void prepareToRecording() {
        Log.d(TAG, "RecordingService: prepareToRecording()");
        initRecorder();
        shareScreen();
    }

    public void startRecording(){
        mIsRecording = true;
        prepareToRecording();
        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();
    }

    public void stopRecording(){
        mMediaRecorder.stop();
        mMediaRecorder.reset();
        Log.d(TAG, "Stopping Recording");
        stopScreenSharing();
    }


    public class RecordingBinder extends Binder{
        public RecordingService getService(){
            return RecordingService.this;
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "RecordingService: onBind()");
        mScreenCaptureIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
        mScreenCaptureResultCode = mScreenCaptureIntent.getIntExtra(UiUtils.SCREEN_CAPTURE_INTENT_RESULT_CODE, UiUtils.RESULT_CODE_FAILED);
        Log.d(TAG, "onBind: "+ mScreenCaptureIntent);
        return mIBinder;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "RecordingService: onCreate()");
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        DisplayMetrics metrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(metrics);

        mMediaRecorder = new MediaRecorder();

        mScreenDensity = metrics.densityDpi;

        mProjectionManager =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        //TOdo: chooose resolution and orientation
        mResolution = RESOLUTIONS.get(2);
        mDisplayWidth = mResolution.x;
        mDisplayHeight = mResolution.y;
    }

    private void stopScreenSharing() {
        Log.d(TAG, "RecordingService: stopScreenSharing()");
        //Todo: Save file here
        mScreenSharing = false;
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        destroyMediaProjection();
    }

    private void initRecorder() {
        Log.d(TAG, "RecordingService: initRecorder()");
        try {
            Timestamp timestamp = new Timestamp(new Date().getTime());

            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mMediaRecorder.setOutputFile(Environment
                    .getExternalStoragePublicDirectory(Environment
                            .DIRECTORY_DOWNLOADS) + "/Zecorder-"+timestamp+".mp4");
            mMediaRecorder.setVideoSize(mDisplayWidth, mDisplayHeight);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mMediaRecorder.setVideoEncodingBitRate(mResolution.bitrate);
            mMediaRecorder.setVideoFrameRate(mResolution.fps);
            int rotation = mWindowManager.getDefaultDisplay().getRotation();
            int orientation = ORIENTATIONS.get(rotation + 90);
            mMediaRecorder.setOrientationHint(orientation);
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void shareScreen() {
        Log.d(TAG, "RecordingService: initRecorder()");
        mScreenSharing = true;
        if (mMediaProjection == null) {
            mMediaProjectionCallback = new MediaProjectionCallback();
            mMediaProjection = mProjectionManager.getMediaProjection(mScreenCaptureResultCode, mScreenCaptureIntent);
            mMediaProjection.registerCallback(mMediaProjectionCallback, null);

        }

    }



    private VirtualDisplay createVirtualDisplay() {
        Log.d(TAG, "RecordingService: createVirtualDisplay()");
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
        Log.d(TAG, "RecordingService: destroyMediaProjection()");
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
        int fps;
        int bitrate;

        public Resolution(int x, int y, int fps, int bitrate) {
            this.x = x;
            this.y = y;
            this.fps = fps;
            this.bitrate = bitrate;
        }
        @Override
        public String toString() {
            return x + "x" + y;
        }
    }

}
