package com.takusemba.rtmppublisher;

import android.media.projection.MediaProjection;
import android.util.Log;

import androidx.annotation.NonNull;

public class RtmpPublisher implements Publisher {


    private static final String TAG = "RtmpPublisher_chienpm";
//    private GLSurfaceView glView;
//    private CameraSurfaceRenderer renderer;
//    private CameraClient camera;
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

//        activity.getLifecycle().addObserver(this);

//        this.glView = glView;
        this.url = url;
        this.width = width;
        this.height = height;
        this.audioBitrate = audioBitrate;
        this.videoBitrate = videoBitrate;
        this.density = density;


//        this.camera = new CameraClient(activity, mode);
        this.streamer = new Streamer(mediaProjection);
        this.streamer.setMuxerListener(listener);

//        glView.setEGLContextClientVersion(2);
//        renderer = new CameraSurfaceRenderer();
//        renderer.addOnRendererStateChangedLister(streamer.getVideoHandlerListener());
//        renderer.addOnRendererStateChangedLister(this);

//        glView.setRenderer(renderer);
//        glView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

    }

//    @Override
//    public void switchCamera() {
//        camera.swap();
//    }

    @Override
    public void startPublishing() {
        Log.i(TAG, "startPublishing: called (clicked)");
        streamer.open(url, width, height);
        streamer.startStreaming(width, height, audioBitrate, videoBitrate, density);
//
//        glView.queueEvent(new Runnable() {
//            @Override
//            public void run() {
//                // EGL14.eglGetCurrentContext() should be called from glView thread.
//                final EGLContext context = EGL14.eglGetCurrentContext();
//                glView.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        // back to main thread
//                        streamer.startStreaming(context, width, height, audioBitrate, videoBitrate, density);
//                    }
//                });
//            }
//        });
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

//    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
//    public void onResume(LifecycleOwner owner) {
//        Camera.Parameters params = camera.open();
//        final Camera.Size size = params.getPreviewSize();
//        glView.onResume();
//        glView.queueEvent(new Runnable() {
//            @Override
//            public void run() {
//                renderer.setCameraPreviewSize(size.width, size.height);
//            }
//        });
//    }
//
//    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
//    public void onPause(LifecycleOwner owner) {
//        if (camera != null) {
//            camera.close();
//        }
//        glView.onPause();
//        glView.queueEvent(new Runnable() {
//            @Override
//            public void run() {
//                renderer.pause();
//            }
//        });
//        if (streamer.isStreaming()) {
//            streamer.stopStreaming();
//        }
//    }

//    @Override
//    public void onSurfaceCreated(SurfaceTexture surfaceTexture) {
//        surfaceTexture.setOnFrameAvailableListener(this);
//        camera.startPreview(surfaceTexture);
//    }
//
//    @Override
//    public void onFrameDrawn(int textureId, float[] transform, long timestamp) {
//        // no-op
//    }
//
//    @Override
//    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
//        glView.requestRender();
//    }
}
