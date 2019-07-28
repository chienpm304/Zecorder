package com.chienpm.zecorder.ui.encoder;


import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.graphics.SurfaceTexture.OnFrameAvailableListener;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import com.chienpm.zecorder.controllers.settings.VideoSetting;
import com.serenegiant.glutils.EGLBase;
import com.serenegiant.glutils.EglTask;
import com.serenegiant.glutils.GLDrawer2D;

import java.io.IOException;
import java.util.List;

public class MediaScreenEncoder extends MediaVideoEncoderBase {
	private static final boolean DEBUG = false;	// TODO set false on release
	private static final String TAG = "chienpm_record";

	private static final String MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;
    private static final int FRAME_RATE = 25;

	private MediaProjection mMediaProjection;
    private final int mDensity;
    private final int bitrate, fps;
    private Surface mSurface;
    private final Handler mHandler;
	private List<Bitmap> mBitmaps;

	public MediaScreenEncoder(MediaMuxerWrapper muxer, MediaEncoderListener listener, MediaProjection projection, VideoSetting videoSetting, int density, List<Bitmap> bitmaps) {
		super(muxer, listener, videoSetting.getWidth(), videoSetting.getHeight(), videoSetting.getOrientation());
		mMediaProjection = projection;
		mDensity = density;
		int _fps = videoSetting.getFPS();
		int _bitrate = videoSetting.getBirate();
		fps = (_fps > 0 && _fps <= 30) ? _fps : FRAME_RATE;
		bitrate = (_bitrate > 0) ? _bitrate : calcBitRate(_fps);

		final HandlerThread thread = new HandlerThread(TAG);
		thread.start();
		mHandler = new Handler(thread.getLooper());
		mBitmaps = bitmaps;
	}

	@Override
	protected void release() {
		mHandler.getLooper().quit();
		super.release();
	}

	@Override
	void prepare() throws IOException {
		if (DEBUG) Log.i(TAG, "prepare: ");
		mSurface = prepare_surface_encoder(MIME_TYPE, fps, bitrate);
        mMediaCodec.start();
        mIsRecording = true;
        new Thread(mScreenCaptureTask, "ScreenCaptureThread").start();
        if (DEBUG) Log.i(TAG, "prepare finishing");
        if (mListener != null) {
        	try {
        		mListener.onPrepared(this);
        	} catch (final Exception e) {
        		Log.e(TAG, "prepare:", e);
        	}
        }
	}


	@Override
	void stopRecording() {
		if (DEBUG) Log.v(TAG,  "stopRecording:");
		synchronized (mSync) {
			mIsRecording = false;
			mSync.notifyAll();
		}
		super.stopRecording();
	}


	private final Object mSync = new Object();
	private volatile boolean mIsRecording;

	private boolean requestDraw;
	private final DrawTask mScreenCaptureTask = new DrawTask(null, 0);

	private final class DrawTask extends EglTask {
		private VirtualDisplay display;
		private long intervals;
		private int mTexId;
		private SurfaceTexture mSourceTexture;
		private Surface mSourceSurface;
    	private EGLBase.IEglSurface mEncoderSurface;
    	private GLDrawer2D mDrawer;
    	private final float[] mTexMatrix = new float[16];

    	public DrawTask(final EGLBase.IContext sharedContext, final int flags) {
    		super(sharedContext, flags);
    	}

		@Override
		protected void onStart() {
		    if (DEBUG) Log.d(TAG,"mScreenCaptureTask#onStart:");
			mDrawer = new GLDrawer2D(true);
			mTexId = mDrawer.initTex();
			mSourceTexture = new SurfaceTexture(mTexId);
			mSourceTexture.setDefaultBufferSize(mWidth, mHeight);	// これを入れないと映像が取れない
			mSourceSurface = new Surface(mSourceTexture);
			mSourceTexture.setOnFrameAvailableListener(mOnFrameAvailableListener, mHandler);
			mEncoderSurface = getEgl().createFromSurface(mSurface);

	    	if (DEBUG) Log.d(TAG,"setup VirtualDisplay");
			intervals = (long)(1000f / fps);
		    display = mMediaProjection.createVirtualDisplay(
		    	"Capturing Display",
		    	mWidth, mHeight, mDensity,
		    	DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
		    	mSourceSurface, null, mHandler);
			if (DEBUG) Log.v(TAG,  "screen capture loop:display=" + display);

			queueEvent(mDrawTask);
		}

		@Override
		protected void onStop() {
			if (mDrawer != null) {
				mDrawer.release();
				mDrawer = null;
			}
			if (mSourceSurface != null) {
				mSourceSurface.release();
				mSourceSurface = null;
			}
			if (mSourceTexture != null) {
				mSourceTexture.release();
				mSourceTexture = null;
			}
			if (mEncoderSurface != null) {
				mEncoderSurface.release();
				mEncoderSurface = null;
			}
			makeCurrent();
			if (DEBUG) Log.v(TAG, "mScreenCaptureTask#onStop:");
			if (display != null) {
				if (DEBUG) Log.v(TAG,  "release VirtualDisplay");
				display.release();
			}
			if (DEBUG) Log.v(TAG,  "tear down MediaProjection");
		    if (mMediaProjection != null) {
	            mMediaProjection.stop();
	            mMediaProjection = null;
	        }
		}

		@Override
		protected boolean onError(final Exception e) {
			if (DEBUG) Log.w(TAG, "mScreenCaptureTask:", e);
			return false;
		}

		@Override
		protected Object processRequest(final int request, final int arg1, final int arg2, final Object obj) {
			return null;
		}

		private final OnFrameAvailableListener mOnFrameAvailableListener = new OnFrameAvailableListener() {
			@Override
			public void onFrameAvailable(final SurfaceTexture surfaceTexture) {
				if (mIsRecording) {
					synchronized (mSync) {
						requestDraw = true;
						mSync.notifyAll();
					}
				}
			}
		};
		int count1 = 0, count2 = 0;
		private final Runnable mDrawTask = new Runnable() {
			@Override
			public void run() {
				boolean local_request_draw;
				synchronized (mSync) {
					local_request_draw = requestDraw;
					if (!requestDraw) {
						try {
							mSync.wait(intervals);
							local_request_draw = requestDraw;
							requestDraw = false;
						} catch (final InterruptedException e) {
							return;
						}
					}
				}
				if (mIsRecording) {
					if (local_request_draw) {
						Log.d(TAG, "DrawTask run in local_request_draw: "+ count1++);
						//Todo: draw decorators here
						mSourceTexture.updateTexImage();
						mSourceTexture.getTransformMatrix(mTexMatrix);
					}

					Log.d(TAG, "DrawTask run outside local_request: "+ count2++);
					mEncoderSurface.makeCurrent();

					mDrawer.draw(mTexId, mTexMatrix, 0);
					Bitmap bmToUse; Bitmap modImage = null;
					for(Bitmap image: mBitmaps){
						if (image.getHeight() == mHeight && image.getWidth() == mWidth) {
							bmToUse = image;
						} else {
							modImage = RenderUtil.getResizedBitmap(image, mWidth, mHeight, modImage);
							bmToUse = modImage;
						}
						generateSurfaceFrame(bmToUse);
					}

			    	mEncoderSurface.swap();
					makeCurrent();
					GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
					GLES20.glFlush();
					frameAvailableSoon();
					queueEvent(this);
				} else {
					releaseSelf();
				}
			}
		};

		private void generateSurfaceFrame(Bitmap bitmap) {

			GLES20.glClearColor(0, 0, 0, 1);
			GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

			int texture = createTexture(bitmap);
			RenderUtil.renderTexture(texture, mWidth, mHeight);
		}

		public int createTexture(Bitmap bitmap) {
			int[] textures = new int[1];
			GLES20.glGenTextures(textures.length, textures, 0);
			int texture = textures[0];
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
			GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
			GLES20.glTexParameteri(
					GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
			GLES20.glTexParameteri(
					GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
			GLES20.glTexParameteri(
					GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
			GLES20.glTexParameteri(
					GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
			return texture;
		}
	}

}
