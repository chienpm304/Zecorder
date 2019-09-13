package com.chienpm.zecorder.ui.services.recording;

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
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.widget.Toast;

import com.chienpm.zecorder.R;
import com.chienpm.zecorder.controllers.encoder.MediaAudioEncoder;
import com.chienpm.zecorder.controllers.encoder.MediaEncoder;
import com.chienpm.zecorder.controllers.encoder.MediaMuxerWrapper;
import com.chienpm.zecorder.controllers.encoder.MediaScreenEncoder;
import com.chienpm.zecorder.controllers.encoder.MediaScreenEncoderHard;
import com.chienpm.zecorder.controllers.encoder.RenderUtil.CustomDecorator;
import com.chienpm.zecorder.controllers.settings.SettingManager;
import com.chienpm.zecorder.controllers.settings.VideoSetting;
import com.chienpm.zecorder.data.database.VideoDatabase;
import com.chienpm.zecorder.data.entities.Video;
import com.chienpm.zecorder.ui.activities.MainActivity;
import com.chienpm.zecorder.ui.services.BaseService;
import com.chienpm.zecorder.ui.utils.MyUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.chienpm.zecorder.ui.utils.MyUtils.DEBUG;

public class RecordingService extends BaseService {
    private final IBinder mIBinder = new RecordingBinder();

    private static final String TAG = RecordingService.class.getSimpleName();
    private MediaProjectionManager mMediaProjectionManager;
    private MediaProjection mMediaProjection;
    private Intent mScreenCaptureIntent;
    private int mScreenCaptureResultCode;
    private int mScreenWidth, mScreenHeight, mScreenDensity;
    private MediaMuxerWrapper mMuxer;
    private static final Object sSync = new Object();
    private VideoSetting mCurrentVideoSetting;
    private VideoSetting mResultVideo;

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
        Log.i(TAG, "RecordingService: onBind()");
        mScreenCaptureIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
        mScreenCaptureResultCode = mScreenCaptureIntent.getIntExtra(MyUtils.SCREEN_CAPTURE_INTENT_RESULT_CODE, MyUtils.RESULT_CODE_FAILED);
        Log.i(TAG, "onBind: "+ mScreenCaptureIntent);
        return mIBinder;
    }

    @Override
    public void startPerformService() {
        Log.i(TAG, "startPerformService: from RecordingService");
        startRecording();
    }

    @Override
    public void stopPerformService() {
        VideoSetting v = stopRecording();
        mResultVideo = v;
    }

    public VideoSetting getResultVideo(){return mResultVideo;}

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


                if (DEBUG) Log.i(TAG, "startStreaming:");
                try {
                    mMuxer = new MediaMuxerWrapper(this, ".mp4");    // if you record audio only, ".m4a" is also OK.
                    if (true) {
                        // for screen capturing
                        //todo: setting video parameter here
                        VideoSetting videoSetting = SettingManager.getVideoProfile(getApplicationContext());
                        mCurrentVideoSetting = videoSetting;

                        List<CustomDecorator> decors = createDecorators();

                        //todo: test HWencoder

//                         new TestMediaScreenEncoderHard(mMuxer, mMediaEncoderListener, mMediaProjection, mCurrentVideoSetting, mScreenDensity, decors);

                        if(MyUtils.isRunningOnEmulator()) {
                            new MediaScreenEncoder(mMuxer, mMediaEncoderListener, mMediaProjection, mCurrentVideoSetting, mScreenDensity, decors);
                        }
                        else {
                            new MediaScreenEncoderHard(mMuxer, mMediaEncoderListener, mMediaProjection, mCurrentVideoSetting, mScreenDensity, decors);
                        }
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

    private ArrayList<CustomDecorator> createDecorators() {
        ArrayList<CustomDecorator> list = new ArrayList<>();
        //main screen
//        list.add(new CustomDecorator(null, new Size(mCurrentVideoSetting.getWidth(), mCurrentVideoSetting.getHeight()), new Point(0,0)));

        //logo
//        list.add(new CustomDecorator( "/storage/emulated/0/Download/image.jpeg", new Size(30, 30), new Point(10, 10)));



        //watermask
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.wartermark_small);

//        Bitmap watermark = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
//        Canvas canvas = new Canvas(watermark);
//        Paint paint = new Paint();
//        canvas.drawBitmap(bitmap, 0, 0, paint);
//        bitmap.recycle();

        list.add(new CustomDecorator(bitmap, new Size(200, 200), new Point(0, 0)));

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

                outputFile = mMuxer.getOutputPath();
                mCurrentVideoSetting.setOutputPath(outputFile);
                mMuxer.stopRecording();
                mMuxer = null;
                // you should not wait here
            }
        }
        return mCurrentVideoSetting;
    }

    public void saveVideoToDatabase() {
        String outputFile = mResultVideo.getOutputPath();
        if(TextUtils.isEmpty(outputFile))
            return;
        MyUtils.toast(getApplicationContext(), "Recording Stopped"+outputFile, Toast.LENGTH_LONG);

        final Video mVideo = MyUtils.tryToExtractVideoInfoFile(getApplicationContext(), mResultVideo);

        new Thread(new Runnable() {
            @Override
            public void run() {
                if(mVideo !=null){
                    if(DEBUG) Log.i(TAG, "onSaveVideo: "+mVideo.toString());
                    synchronized (mVideo) {
                        VideoDatabase.getInstance(getApplicationContext()).getVideoDao().insertVideo(mVideo);
                    }
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setAction(MyUtils.ACTION_OPEN_VIDEO_MANAGER_ACTIVITY);
                    startActivity(intent);

                }
            }
        }).start();
    }





    private static final MediaEncoder.MediaEncoderListener mMediaEncoderListener = new MediaEncoder.MediaEncoderListener() {
        @Override
        public void onPrepared(final MediaEncoder encoder) {
            if (DEBUG) Log.i(TAG, "onPrepared:encoder=" + encoder);
        }

        @Override
        public void onStopped(final MediaEncoder encoder) {
            if (DEBUG) Log.i(TAG, "onStopped:encoder=" + encoder);
        }
    };
}
