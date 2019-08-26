package com.takusemba.rtmppublisher;

import android.media.projection.MediaProjection;
import android.util.Log;

import androidx.annotation.NonNull;

class Streamer
        implements VideoHandler.OnVideoEncoderStateListener, AudioHandler.OnAudioEncoderStateListener {

    private static final String TAG = "Streamer_chienpm_log";

    private static final boolean DEBUG = false;

    private VideoHandler videoHandler;
    private AudioHandler audioHandler;
    private Muxer muxer = new Muxer();

    Streamer(@NonNull MediaProjection mediaProjection) {
        this.videoHandler = new VideoHandler(mediaProjection);
        this.audioHandler = new AudioHandler();
    }

    void open(String url, int width, int height) {
        if(DEBUG) Log.i(TAG, "open: "+url);
        muxer.open(url, width, height);
    }

    void startStreaming(int width, int height, int audioBitrate,
                        int videoBitrate, int density) {
        int t = 0;
        while (!muxer.isConnected()){
            try {
                t+=100;
                Thread.sleep(100);
                if(t>5000)
                    break;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if(muxer.isConnected()) {
            if(DEBUG) Log.i(TAG, "start Streaming: connected");
            long startStreamingAt = System.currentTimeMillis();
            videoHandler.setOnVideoEncoderStateListener(this);
            audioHandler.setOnAudioEncoderStateListener(this);
            videoHandler.start(width, height, videoBitrate, startStreamingAt, density);
            audioHandler.start(audioBitrate, startStreamingAt);
        }
        else{
           Log.e(TAG, "startStreaming: failed coz muxer is not connected");
        }
    }

    void stopStreaming() {
        videoHandler.stop();
        audioHandler.stop();
        muxer.close();
    }

    boolean isStreaming() {
        return muxer.isConnected();
    }

    @Override
    public void onVideoDataEncoded(byte[] data, int size, int timestamp) {
        if(DEBUG) Log.i(TAG, "onVideoDataEncoded: "+size+"byte, tsp: "+timestamp);
        muxer.sendVideo(data, size, timestamp);
    }

    @Override
    public void onAudioDataEncoded(byte[] data, int size, int timestamp) {
        if(DEBUG) Log.i(TAG, "onAudioDataEncoded: "+size+"byte, tsp: "+timestamp);
        muxer.sendAudio(data, size, timestamp);
    }

    void setMuxerListener(PublisherListener listener) {
        muxer.setOnMuxerStateListener(listener);
    }

}
