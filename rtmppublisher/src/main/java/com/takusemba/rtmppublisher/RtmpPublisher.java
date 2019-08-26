package com.takusemba.rtmppublisher;

import android.media.projection.MediaProjection;
import android.util.Log;

import androidx.annotation.NonNull;

import static com.takusemba.rtmppublisher.helper.MyUtils.DEBUG;

public class RtmpPublisher implements Publisher {

    private static final String TAG = RtmpPublisher.class.getSimpleName();

    private Streamer streamer;

    private String url;
    private int width;
    private int height;
    private int audioBitrate;
    private int videoBitrate;
    private int density;

    RtmpPublisher(
                  String url,
                  int width,
                  int height,
                  int audioBitrate,
                  int videoBitrate,
                  int density,
                  PublisherListener listener,
                  @NonNull MediaProjection mediaProjection) {

        this.url = url;
        this.width = width;
        this.height = height;
        this.audioBitrate = audioBitrate;
        this.videoBitrate = videoBitrate;
        this.density = density;
        this.streamer = new Streamer(mediaProjection);
        this.streamer.setMuxerListener(listener);
    }

    @Override
    public void startPublishing() {
        if(DEBUG) Log.i(TAG, "startPublishing: called (clicked)");
        streamer.open(url, width, height);
        streamer.startStreaming(width, height, audioBitrate, videoBitrate, density);
    }

    @Override
    public void stopPublishing() {
        if (streamer.isStreaming()) {
            streamer.stopStreaming();
        }
    }

    @Override
    public boolean isPublishing() {
        return streamer.isStreaming();
    }

}
