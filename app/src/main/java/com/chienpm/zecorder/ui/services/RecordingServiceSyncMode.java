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

import com.chienpm.zecorder.ui.encoder.MediaAudioEncoder;
import com.chienpm.zecorder.ui.encoder.MediaEncoder;
import com.chienpm.zecorder.ui.encoder.MediaMuxerWrapper;
import com.chienpm.zecorder.ui.encoder.MediaScreenEncoder;
import com.chienpm.zecorder.ui.utils.MyUtils;

import java.io.IOException;

public class RecordingServiceSyncMode extends Service {
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

    public class RecordingBinder extends Binder{
        public RecordingServiceSyncMode getService(){
            return RecordingServiceSyncMode.this;
        }
    }

    public RecordingServiceSyncMode() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Get the display size and density.
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        mScreenWidth = metrics.widthPixels;
        mScreenHeight = metrics.heightPixels;
        mScreenDensity = metrics.densityDpi;


        mMediaProjectionManager = (MediaProjectionManager) getSystemService(
                Context.MEDIA_PROJECTION_SERVICE);

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
                    new MediaScreenEncoder(mMuxer, mMediaEncoderListener, mMediaProjection, mScreenWidth, mScreenHeight, mScreenDensity, 800 * 1024, 30);
                }
                if (true) {
                    // for audio capturing
                    new MediaAudioEncoder(mMuxer, mMediaEncoderListener);
                }
                mMuxer.prepare();
                mMuxer.startRecording();
            } catch (final IOException e) {
                Log.e(TAG, "startScreenRecord:", e);
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

    public void stopRecording() {
        if (DEBUG) Log.v(TAG, "stopRecording:mMuxer=" + mMuxer);
        synchronized (sSync) {
            if (mMuxer != null) {
                mMuxer.stopRecording();
                mMuxer = null;
                // you should not wait here
            }
        }
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
