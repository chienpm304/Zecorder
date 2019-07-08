package com.chienpm.zecorder.ui.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.chienpm.zecorder.ui.utils.UiUtils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RecordingUsingMuxerService extends Service {
    private final IBinder mIBinder = new RecordingUsingMuxerBinder();

    private static final String TAG = "chienpm";
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private MediaMuxer muxer;
    private Surface inputSurface;
    private MediaCodec videoEncoder;

    private boolean muxerStarted;
    private int trackIndex = -1;

    private static final String VIDEO_MIME_TYPE = "video/avc";

    private android.media.MediaCodec.Callback encoderCallback;



    private Intent mScreenCaptureIntent;
    private int mScreenCaptureResultCode;

    public class RecordingUsingMuxerBinder extends Binder{
        public RecordingUsingMuxerService getService(){
            return RecordingUsingMuxerService.this;
        }

    }

    public RecordingUsingMuxerService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaProjectionManager = (MediaProjectionManager) getSystemService(
                android.content.Context.MEDIA_PROJECTION_SERVICE);
        encoderCallback = new MediaCodec.Callback() {
            @Override
            public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                Log.d(TAG, "Input Buffer Avail");
            }

            @Override
            public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
                ByteBuffer encodedData = videoEncoder.getOutputBuffer(index);
                if (encodedData == null) {
                    throw new RuntimeException("couldn't fetch buffer at index " + index);
                }

                if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    info.size = 0;
                }

                if (info.size != 0) {
                    if (muxerStarted) {
                        encodedData.position(info.offset);          //update current video position
                        encodedData.limit(info.offset + info.size);
                        muxer.writeSampleData(trackIndex, encodedData, info);
                    }
                }

                videoEncoder.releaseOutputBuffer(index, false);

            }

            @Override
            public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
                Log.e(TAG, "MediaCodec " + codec.getName() + " onError:", e);
            }

            @Override
            public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
                Log.d(TAG, "Output Format changed");
                if (trackIndex >= 0) {
                    throw new RuntimeException("format changed twice");
                }
                trackIndex = muxer.addTrack(videoEncoder.getOutputFormat());
                if (!muxerStarted && trackIndex >= 0) {
                    muxer.start();
                    muxerStarted = true;
                }
            }
        };
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "RecordingService: onBind()");
        mScreenCaptureIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
        mScreenCaptureResultCode = mScreenCaptureIntent.getIntExtra(UiUtils.SCREEN_CAPTURE_INTENT_RESULT_CODE, UiUtils.RESULT_CODE_FAILED);
        Log.d(TAG, "onBind: "+ mScreenCaptureIntent);
        return mIBinder;
    }

    public void startRecording() {
        mediaProjection = mediaProjectionManager.getMediaProjection(mScreenCaptureResultCode, mScreenCaptureIntent);
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

        // Get the display size and density.
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;
        int screenDensity = metrics.densityDpi;

        prepareVideoEncoder(screenWidth, screenHeight);

        try {
            File outputFile = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES) + "/Zecorder", "Screen-record-" +
                    Long.toHexString(System.currentTimeMillis()) + ".mp4");
            if (!outputFile.getParentFile().exists()) {
                outputFile.getParentFile().mkdirs();
            }
            muxer = new MediaMuxer(outputFile.getCanonicalPath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException ioe) {
            throw new RuntimeException("MediaMuxer creation failed", ioe);
        }


        // Start the video input.
        mediaProjection.createVirtualDisplay("Recording Display", screenWidth,
                screenHeight, screenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR/* flags */, inputSurface,
                null /* callback */, null /* handler */);
    }

    private void prepareVideoEncoder(int width, int height) {
        MediaFormat format = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, width, height);
        int frameRate = 30; // 30 fps

        // Set some required properties. The media codec may fail if these aren't defined.
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 6000000); // 6Mbps
        format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        format.setInteger(MediaFormat.KEY_CAPTURE_RATE, frameRate);
        format.setInteger(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 1000000 / frameRate);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1); // 1 seconds between I-frames

        // Create a MediaCodec encoder and configure it. Get a Surface we can use for recording into.
        try {
            videoEncoder = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE);
            videoEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            inputSurface = videoEncoder.createInputSurface();
            videoEncoder.setCallback(encoderCallback);
            videoEncoder.start();
        } catch (IOException e) {
            releaseEncoders();
        }
    }

    public void stopRecording() {
        releaseEncoders();
    }


    private void releaseEncoders() {
        if (muxer != null) {
            if (muxerStarted) {
                muxer.stop();
            }
            muxer.release();
            muxer = null;
            muxerStarted = false;
        }
        if (videoEncoder != null) {
            videoEncoder.stop();
            videoEncoder.release();
            videoEncoder = null;
        }
        if (inputSurface != null) {
            inputSurface.release();
            inputSurface = null;
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
        trackIndex = -1;
    }




}
