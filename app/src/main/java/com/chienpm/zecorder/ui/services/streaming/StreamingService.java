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
import android.widget.Toast;

import com.chienpm.zecorder.R;
import com.chienpm.zecorder.controllers.encoder.RenderUtil.CustomDecorator;
import com.chienpm.zecorder.controllers.streaming.StreamProfile;
import com.chienpm.zecorder.ui.utils.MyUtils;
import com.takusemba.rtmppublisher.Publisher;
import com.takusemba.rtmppublisher.PublisherListener;

import java.util.ArrayList;

public class StreamingService extends Service implements PublisherListener {
    private static final boolean DEBUG = false;	// TODO set false on release
    public static final String KEY_NOTIFY_MSG = "stream service notify";

    public static final String NOTIFY_MSG_CONNECTION_FAILED = "STREAM CONNECTION FAILED";
    public static final String NOTIFY_MSG_CONNECTION_STARTED = "STREAM CONNECTION SUCCEED";
    public static final String NOTIFY_MSG_ERROR = "STREAM CONNECTION ERROR";
    public static final String NOTIFY_MSG_CONNECTION_DISCONNECTED = "NOTIFY_MSG_CONNECTION_DISCONNECTED";
    public static final String NOTIFY_MSG_STREAM_STOPPED = "NOTIFY_MSG_STREAM_STOPPED";

    private final IBinder mIBinder = new StreamingBinder();

    private static final String TAG = "StreamService_chienpm";
    private MediaProjectionManager mMediaProjectionManager;
    private MediaProjection mMediaProjection;
    private Intent mScreenCaptureIntent;
    private int mScreenCaptureResultCode;
    private int mScreenWidth, mScreenHeight, mScreenDensity;
    private Publisher mPublisher;
    private static final Object sSync = new Object();

    private StreamProfile mStreamProfile;

    private String url = "rtmp://127.0.0.1/live/key";
//    private String url = "rtmp://10.199.220.239/live/key";


    //Implement Publisher listener
    @Override
    public void onStarted() {
        Log.i(TAG, "onStarted");
        notifyStreamControllerAction(NOTIFY_MSG_CONNECTION_STARTED);
    }

    @Override
    public void onStopped() {
        Log.i(TAG, "onStopped");
        notifyStreamControllerAction(NOTIFY_MSG_STREAM_STOPPED);
    }

    @Override
    public void onDisconnected() {
        Log.i(TAG, "onDisconnected");
        notifyStreamControllerAction(NOTIFY_MSG_CONNECTION_DISCONNECTED);
    }

    @Override
    public void onFailedToConnect() {
        if(mPublisher!=null && mPublisher.isPublishing())
            mPublisher.stopPublishing();
        notifyStreamControllerAction(NOTIFY_MSG_CONNECTION_FAILED);
        Log.i(TAG, "onFailedToConnect");
        MyUtils.toast(getApplicationContext(), "Streaming Connection Failed", Toast.LENGTH_SHORT);
    }

    public class StreamingBinder extends Binder{
        public StreamingService getService(){
            return StreamingService.this;
        }
    }

    public StreamingService() {

    }

    void notifyStreamControllerAction(String notify_msg){
        Intent intent = new Intent(getApplicationContext(), StreamingControllerService.class);
        intent.setAction(MyUtils.ACTION_NOTIFY_FROM_STREAM_SERVICE);
        intent.putExtra(KEY_NOTIFY_MSG, notify_msg);
        startService(intent);
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
        //just support landscape
        if(width>height)
        {
            mScreenWidth = width;
            mScreenHeight = height;
        }
        else {
            mScreenWidth = height;
            mScreenHeight = width;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "RecordingService: onBind()");
        mScreenCaptureIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
        mScreenCaptureResultCode = mScreenCaptureIntent.getIntExtra(MyUtils.SCREEN_CAPTURE_INTENT_RESULT_CODE, MyUtils.RESULT_CODE_FAILED);

        mStreamProfile = (StreamProfile) intent.getSerializableExtra(MyUtils.STREAM_PROFILE);

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

        Log.d(TAG, "onBindStream: "+ mScreenCaptureIntent);
        Log.d(TAG, "onBindStream: "+ mStreamProfile.toString());
        return mIBinder;
    }

    public void startStreaming() {
        synchronized (sSync) {
            if(mPublisher==null) {
                if (DEBUG) Log.v(TAG, "startStreaming:");
                try {

                    mPublisher = new Publisher.Builder()
                            .setUrl(url)
                            .setSize(mScreenWidth, mScreenHeight)
                            .setAudioBitrate(Publisher.Builder.DEFAULT_AUDIO_BITRATE)
                            .setVideoBitrate(Publisher.Builder.DEFAULT_VIDEO_BITRATE)
                            .setDensity(mScreenDensity)
                            .setMediaProjection(mMediaProjection)
                            .setListener(this)
                            .build();
                    mPublisher.startPublishing();


                 } catch (final Exception e) {
                    Log.e(TAG, "startStreaming error:", e);
                    notifyStreamControllerAction(NOTIFY_MSG_ERROR);
                }
            }else{
                mPublisher.startPublishing();
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

    public void stopStreaming() {
        if(mPublisher!=null && mPublisher.isPublishing())
            mPublisher.stopPublishing();
    }

}
