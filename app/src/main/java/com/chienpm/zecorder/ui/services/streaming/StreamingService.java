package com.chienpm.zecorder.ui.services.streaming;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.Display;

import com.chienpm.zecorder.R;
import com.chienpm.zecorder.controllers.encoder.RenderUtil.CustomDecorator;
import com.chienpm.zecorder.controllers.settings.SettingManager;
import com.chienpm.zecorder.controllers.settings.VideoSetting;
import com.chienpm.zecorder.controllers.streaming.StreamAudioEncoder;
import com.chienpm.zecorder.controllers.streaming.StreamEncoder;
import com.chienpm.zecorder.controllers.streaming.StreamMuxerWrapper;
import com.chienpm.zecorder.controllers.streaming.StreamProfile;
import com.chienpm.zecorder.controllers.streaming.StreamScreenEncoder;
import com.chienpm.zecorder.ui.utils.MyUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StreamingService extends Service {
    private static final boolean DEBUG = false;	// TODO set false on release
    private final IBinder mIBinder = new StreamingBinder();

    private static final String TAG = "chienpm";
    private MediaProjectionManager mMediaProjectionManager;
    private MediaProjection mMediaProjection;
    private Intent mScreenCaptureIntent;
    private int mScreenCaptureResultCode;
    private int mScreenWidth, mScreenHeight, mScreenDensity;
    private StreamMuxerWrapper mMuxer;
    private static final Object sSync = new Object();
    private VideoSetting mCurrentVideoSetting;
    private StreamProfile mStreamProfile;

    public class StreamingBinder extends Binder{
        public StreamingService getService(){
            return StreamingService.this;
        }
    }

    public StreamingService() {

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

        mStreamProfile = (StreamProfile) intent.getSerializableExtra(MyUtils.STREAM_PROFILE);

        Log.d(TAG, "onBindStream: "+ mScreenCaptureIntent);
        Log.d(TAG, "onBindStream: "+ mStreamProfile.toString());
        return mIBinder;
    }

    public void startStreaming() {
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

                mCurrentVideoSetting = SettingManager.getVideoProfile(getApplicationContext());

                if (DEBUG) Log.v(TAG, "startStreaming:");
                try {
                    mMuxer = new StreamMuxerWrapper(this,  mStreamProfile, mCurrentVideoSetting);    // if you record audio only, ".m4a" is also OK.
                    if (true) {
                        // for screen capturing
                        //todo: setting video parameter here

                        List<CustomDecorator> decors = createDecorators();

                        new StreamScreenEncoder(mMuxer, mMediaEncoderListener, mMediaProjection, mCurrentVideoSetting, mScreenDensity, decors);
                    }
                    if (true) {
                        // for audio capturing
                        //todo: setting audio setting here
                        new StreamAudioEncoder(mMuxer, mMediaEncoderListener);
                    }
                    mMuxer.prepare();
                    mMuxer.startStreaming();
                } catch (final IOException e) {
                    Log.e(TAG, "startScreenRecord:", e);
                }
            }
        }

    }

    private ArrayList<CustomDecorator> createDecorators() {
        ArrayList<CustomDecorator> list = new ArrayList<>();

        //watermask
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.wartermark);

        list.add(new CustomDecorator(bitmap, new Size(240, 240), new Point(0, 0)));

        return list;
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
    public VideoSetting stopRecording() {
        if (DEBUG) Log.v(TAG, "stopStreaming:mMuxer=" + mMuxer);

        String outputFile = "";

        synchronized (sSync) {
            if (mMuxer != null) {

//                outputFile = mMuxer.getOutputPath();
                mCurrentVideoSetting.setOutputPath(outputFile);
                mMuxer.stopStreaming();
                mMuxer = null;
                // you should not wait here
            }
        }
        return mCurrentVideoSetting;
    }

    private static final StreamEncoder.StreamEncoderListener mMediaEncoderListener = new StreamEncoder.StreamEncoderListener() {
        @Override
        public void onPrepared(StreamEncoder encoder) {
            if (DEBUG) Log.v(TAG, "onPrepared:encoder=" + encoder);
        }

        @Override
        public void onStopped(StreamEncoder encoder) {
            if (DEBUG) Log.v(TAG, "onStopped:encoder=" + encoder);
        }

    };
}
