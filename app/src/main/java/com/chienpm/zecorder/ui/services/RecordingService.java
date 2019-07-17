package com.chienpm.zecorder.ui.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;

import com.chienpm.zecorder.data.entities.Video;
import com.chienpm.zecorder.ui.encoder.MediaAudioEncoder;
import com.chienpm.zecorder.ui.encoder.MediaEncoder;
import com.chienpm.zecorder.ui.encoder.MediaMuxerWrapper;
import com.chienpm.zecorder.ui.encoder.MediaScreenEncoder;
import com.chienpm.zecorder.ui.utils.MyUtils;
import com.chienpm.zecorder.controllers.settings.SettingManager;
import com.chienpm.zecorder.controllers.settings.VideoSetting;

import java.io.IOException;

public class RecordingService extends Service {
    private static final boolean DEBUG = false;	// TODO set false on release
    private final IBinder mIBinder = new RecordingBinder();

    private static final String TAG = "chienpm";
    private MediaProjectionManager mMediaProjectionManager;
    private MediaProjection mMediaProjection;
    private Intent mScreenCaptureIntent;
    private int mScreenCaptureResultCode;
    private int mScreenWidth, mScreenHeight, mScreenDensity;
    private MediaMuxerWrapper mMuxer;
    private static final Object sSync = new Object();
    private VideoSetting mCurrentVideoSetting;

    public class RecordingBinder extends Binder{
        public RecordingService getService(){
            return RecordingService.this;
        }
    }

    public RecordingService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        mMediaProjectionManager = (MediaProjectionManager) getSystemService(
                Context.MEDIA_PROJECTION_SERVICE);

    }

    private void getScreenSize() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        mScreenDensity = metrics.densityDpi;
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        if (width > height) {
            final float scale_x = width / 1920f;
            final float scale_y = height / 1080f;
            final float scale = Math.max(scale_x,  scale_y);
            width = (int)(width / scale);
            height = (int)(height / scale);
        } else {
            final float scale_x = width / 1080f;
            final float scale_y = height / 1920f;
            final float scale = Math.max(scale_x,  scale_y);
            width = (int)(width / scale);
            height = (int)(height / scale);
        }
        mScreenWidth = width;
        mScreenHeight = height;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "RecordingService: onBind()");
        mScreenCaptureIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
        mScreenCaptureResultCode = mScreenCaptureIntent.getIntExtra(MyUtils.SCREEN_CAPTURE_INTENT_RESULT_CODE, MyUtils.RESULT_CODE_FAILED);
        Log.d(TAG, "onBind: "+ mScreenCaptureIntent);
        return mIBinder;
    }

    public void startRecording() {
        synchronized (sSync) {
            if(mMuxer==null) {
                getScreenSize();
                mMediaProjection = mMediaProjectionManager.getMediaProjection(mScreenCaptureResultCode, mScreenCaptureIntent);
                DisplayManager dm = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
                Display defaultDisplay;
                if (dm != null) {
                    defaultDisplay = dm.getDisplay(Display.DEFAULT_DISPLAY);
                } else {
                    throw new IllegalStateException("Cannot display manager?!?");
                }
                if (defaultDisplay == null) {
                    throw new RuntimeException("No display found.");
                }


                if (DEBUG) Log.v(TAG, "startRecording:");
                try {
                    mMuxer = new MediaMuxerWrapper(this, ".mp4");    // if you record audio only, ".m4a" is also OK.
                    if (true) {
                        // for screen capturing
                        //todo: setting video parameter here
                        VideoSetting videoSetting = SettingManager.getVideoProfile(getApplicationContext());
                        mCurrentVideoSetting = videoSetting;
                        new MediaScreenEncoder(mMuxer, mMediaEncoderListener, mMediaProjection, mCurrentVideoSetting, mScreenDensity);
                    }
                    if (true) {
                        // for audio capturing
                        //todo: setting audio setting here
                        new MediaAudioEncoder(mMuxer, mMediaEncoderListener);
                    }
                    mMuxer.prepare();
                    mMuxer.startRecording();
                } catch (final IOException e) {
                    Log.e(TAG, "startScreenRecord:", e);
                }
            }
        }

    }
    public void pauseScreenRecord() {
        synchronized (sSync) {
            if (mMuxer != null) {
                mMuxer.pauseRecording();
            }
        }
    }

    public void resumeScreenRecord() {
        synchronized (sSync) {
            if (mMuxer != null) {
                mMuxer.resumeRecording();
            }
        }
    }

    //Return output file
    public String stopRecording() {
        if (DEBUG) Log.v(TAG, "stopRecording:mMuxer=" + mMuxer);

        String outputFile = "";

        synchronized (sSync) {
            if (mMuxer != null) {

                outputFile = mMuxer.getOutputPath();

                mMuxer.stopRecording();
                mMuxer = null;
                // you should not wait here
            }
        }
        return outputFile;
    }

    private static final MediaEncoder.MediaEncoderListener mMediaEncoderListener = new MediaEncoder.MediaEncoderListener() {
        @Override
        public void onPrepared(final MediaEncoder encoder) {
            if (DEBUG) Log.v(TAG, "onPrepared:encoder=" + encoder);
        }

        @Override
        public void onStopped(final MediaEncoder encoder) {
            if (DEBUG) Log.v(TAG, "onStopped:encoder=" + encoder);
        }
    };
}
