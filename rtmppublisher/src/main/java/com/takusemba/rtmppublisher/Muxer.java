package com.takusemba.rtmppublisher;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import net.butterflytv.rtmp_client.RTMPMuxer;

class Muxer {

    private static final int MSG_OPEN = 0;
    private static final int MSG_CLOSE = 1;
    private static final int MSG_SEND_VIDEO = 2;
    private static final int MSG_SEND_AUDIO = 3;
    private static final String TAG = "Muxer_chienpm";

    private Handler handler;

    private RTMPMuxer rtmpMuxer = new RTMPMuxer();
    private PublisherListener listener;

    private boolean disconnected = false;
    private boolean closed = false;

    void setOnMuxerStateListener(PublisherListener listener) {
        this.listener = listener;
    }

    Muxer() {
        final Handler uiHandler = new Handler(Looper.getMainLooper());
        HandlerThread handlerThread = new HandlerThread("Muxer");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_OPEN:
                        Log.i(TAG, "handleMessage: open");
                        rtmpMuxer.open((String) msg.obj, msg.arg1, msg.arg2);
                        if (listener != null) {
                            uiHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (isConnected()) {
                                        listener.onStarted();
                                        disconnected = false;
                                        closed = false;
                                        Log.i(TAG, "opened url: connected");
                                    } else {
                                        Log.i(TAG, "opened url: not connected");
                                        listener.onFailedToConnect();
                                    }
                                }
                            });
                        }
                        break;
                    case MSG_CLOSE:
                        rtmpMuxer.close();
                        if (listener != null) {
                            uiHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    listener.onStopped();
                                    closed = true;
                                }
                            });
                        }
                        break;
                    case MSG_SEND_VIDEO: {
                        if (isConnected()) {
                            byte[]data = (byte[]) msg.obj;
                            int res = rtmpMuxer.writeVideo(data, 0, msg.arg1, msg.arg2);
                            Log.i(TAG, "write video (handle MSG): "+data.length+", at: "+msg.arg2);
                            Log.i(TAG, "write video res: "+res);
                        } else {
                            Log.i(TAG, "handle writeVideo: muxer is not connected");
                            if (listener != null) {
                                uiHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (closed || disconnected) return;
                                        listener.onDisconnected();
                                        disconnected = true;
                                    }
                                });
                            }
                        }
                        break;
                    }
                    case MSG_SEND_AUDIO: {
                        if (isConnected()) {
                            rtmpMuxer.writeAudio((byte[]) msg.obj, 0, msg.arg1, msg.arg2);
                        } else {
                            if (listener != null) {
                                uiHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (closed || disconnected) return;
                                        listener.onDisconnected();
                                        disconnected = true;
                                    }
                                });
                            }
                        }
                        break;
                    }
                }
                return false;
            }
        });
    }

    void open(String url, int width, int height) {
        Log.i(TAG, "open (obtain Msg): "+url);
        Message message = handler.obtainMessage(MSG_OPEN, url);
        message.arg1 = width;
        message.arg2 = height;
        handler.sendMessage(message);
    }

    void sendVideo(byte[] data, int length, int timestamp) {
        Log.i(TAG, "sendVideo (obtain Msg): "+length+"byte, tsp: "+timestamp);
        Message message = handler.obtainMessage(MSG_SEND_VIDEO, data);
        message.arg1 = length;
        message.arg2 = timestamp;
        handler.sendMessage(message);
    }

    void sendAudio(final byte[] data, final int length, final int timestamp) {
        Message message = handler.obtainMessage(MSG_SEND_AUDIO, data);
        message.arg1 = length;
        message.arg2 = timestamp;
        handler.sendMessage(message);
    }

    void close() {
        handler.sendEmptyMessage(MSG_CLOSE);
    }

    boolean isConnected() {
        return rtmpMuxer.isConnected();
    }
}
