package com.takusemba.rtmppublisher;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

abstract class VideoEncoder implements Encoder {

    // H.264 Advanced Video Coding
    protected static final String MIME_TYPE = "video/avc";
    // 5 seconds between I-frames
    protected static final int IFRAME_INTERVAL = 5;
    protected static final String TAG = "VideoEnoder_chienpm";

    protected volatile boolean isEncoding = false;

    protected static final int TIMEOUT_USEC = 10000;

    protected Surface inputSurface;
    protected MediaCodec encoder;
    protected MediaCodec.BufferInfo bufferInfo;
    protected VideoHandler.OnVideoEncoderStateListener listener;
    protected long lastFrameEncodedAt = 0;
    protected long startStreamingAt = 0;
    protected int mWidth, mHeight, mFps, mDensity;

    void setOnVideoEncoderStateListener(VideoHandler.OnVideoEncoderStateListener listener) {
        this.listener = listener;
    }

    long getLastFrameEncodedAt() {
        return lastFrameEncodedAt;
    }

    Surface getInputSurface() {
        return inputSurface;
    }

    /**
     * prepare the Encoder. call this before start the encoder.
     */
     void prepare(int width, int height, int bitRate, int frameRate, long startStreamingAt, int density)
            throws IOException {
        this.mWidth = width;
        this.mHeight = height;
        this.mFps = frameRate;
        this.mDensity = density;
        this.startStreamingAt = startStreamingAt;
        this.bufferInfo = new MediaCodec.BufferInfo();
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);

        encoder = MediaCodec.createEncoderByType(MIME_TYPE);
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        inputSurface = encoder.createInputSurface();
        Log.i(TAG, "prepare: video encoder");
    }

    @Override
    public void start() {
        Log.i(TAG, "start: video encoder");
        encoder.start();
        synchronized (mSync) {
            isEncoding = true;
            mRequestStop = false;
            mSync.notifyAll();
        }
        startEncode();
    }

    public void requestStop(){
        synchronized (mSync) {
            if (!isEncoding || mRequestStop) {
                return;
            }
            mRequestStop = true;	// for rejecting newer frame
            mSync.notifyAll();
            // We can not know when the encoding and writing finish.
            // so we return immediately after request to avoid delay of caller thread
        }
    }

    @Override
    public void stop() {
        if (isEncoding()) {
            Log.i(TAG, "stop: video encoder");
            encoder.signalEndOfInputStream();
        }
    }

    @Override
    public boolean isEncoding() {
        return encoder != null && isEncoding;
    }

    void startEncode() {
        HandlerThread handlerThread = new HandlerThread("VideoEncoder-startEncode");
        handlerThread.start();
        Handler handler = new Handler(handlerThread.getLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {

                synchronized (mSync) {
                    mRequestStop = false;
                    mRequestDrain = 0;
                    mSync.notify();
                }
                final boolean isRunning = true;
                boolean localRequestStop;
                boolean localRequestDrain;
                while (isRunning) {
                    synchronized (mSync) {
                        localRequestStop = mRequestStop;
                        localRequestDrain = (mRequestDrain > 0);
                        if (localRequestDrain)
                            mRequestDrain--;
                    }
                    if (localRequestStop) {
                        drain();
                        // request stop recording
                        stop();
                        // process output data again for EOS signale
                        drain();
                        // release all related objects
                        release();
                        break;
                    }
                    if (localRequestDrain) {
                        drain();
                    } else {
                        synchronized (mSync) {
                            try {
                                mSync.wait();
                            } catch (final InterruptedException e) {
                                break;
                            }
                        }
                    }
                } // end of while
                synchronized (mSync) {
                    mRequestStop = true;
                    isEncoding = false;
                }
            }
        });
    }

    private void drain() {
        while (isEncoding) {
            if (encoder == null) return;
            ByteBuffer[] encoderOutputBuffers = encoder.getOutputBuffers();
            int inputBufferId = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
            if (inputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {

                MediaFormat newFormat = encoder.getOutputFormat();
                ByteBuffer sps = newFormat.getByteBuffer("csd-0");
                ByteBuffer pps = newFormat.getByteBuffer("csd-1");
                byte[] config = new byte[sps.limit() + pps.limit()];
                sps.get(config, 0, sps.limit());
                pps.get(config, sps.limit(), pps.limit());

                listener.onVideoDataEncoded(config, config.length, 0);
            } else {
                if (inputBufferId > 0) {
                    ByteBuffer encodedData = encoderOutputBuffers[inputBufferId];
                    if (encodedData == null) {
                        continue;
                    }

                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        bufferInfo.size = 0;
                    }

                    if (bufferInfo.size != 0) {

                        encodedData.position(bufferInfo.offset);
                        encodedData.limit(bufferInfo.offset + bufferInfo.size);

                        long currentTime = System.currentTimeMillis();
                        int timestamp = (int) (currentTime - startStreamingAt);
                        byte[] data = new byte[bufferInfo.size];
                        encodedData.get(data, 0, bufferInfo.size);
                        encodedData.position(bufferInfo.offset);

                        Log.i(TAG, "prepare to invoke to onVideoDataEncoded: "+bufferInfo.size+" bytes");
                        listener.onVideoDataEncoded(data, bufferInfo.size, timestamp);
                        lastFrameEncodedAt = currentTime;
                    }
                    encoder.releaseOutputBuffer(inputBufferId, false);
                } else if (inputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    continue;
                }

                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break;
                }
            }
        }
    }

    protected final Object mSync = new Object();

    private int mRequestDrain;
    /**
     * Flag to request stop capturing
     */
    protected volatile boolean mRequestStop;

    public boolean frameAvailableSoon() {
//    	if (DEBUG) Log.v(TAG, "frameAvailableSoon");
        synchronized (mSync) {
            if (!isEncoding || mRequestStop) {
                return false;
            }
            mRequestDrain++;
            mSync.notifyAll();
        }
        return true;
    }

    private void release() {
        if (encoder != null) {
            isEncoding = false;
            encoder.stop();
            encoder.release();
            encoder = null;
        }
    }
}
