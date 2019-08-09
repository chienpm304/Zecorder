package com.chienpm.zecorder.controllers.streaming;


import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.graphics.SurfaceTexture.OnFrameAvailableListener;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import com.chienpm.zecorder.controllers.encoder.RenderUtil;
import com.chienpm.zecorder.controllers.settings.VideoSetting;
import com.chienpm.zecorder.ui.activities.MainActivity;
import com.chienpm.zecorder.ui.utils.CameraPreview;
import com.chienpm.zecorder.ui.utils.MyUtils;
import com.serenegiant.glutils.EGLBase;
import com.serenegiant.glutils.EglTask;
import com.serenegiant.glutils.GLDrawer2D;

import net.ossrs.yasea.SrsEncoder;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


public class StreamScreenEncoder extends StreamVideoEncoderBase {
	private static final boolean DEBUG = false;	// TODO set false on release
	private static final String TAG = "chienpm_record";

	private static final String MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;
    private static final int FRAME_RATE = 25;

	private MediaProjection mMediaProjection;
    private final int mDensity;
    private final int bitrate, fps;
    private Surface mSurface;
    private final Handler mHandler;
	private List<RenderUtil.CustomDecorator> mDecors;

	public StreamScreenEncoder(StreamMuxerWrapper muxer, StreamEncoderListener listener, MediaProjection projection, VideoSetting videoSetting, int density, List<RenderUtil.CustomDecorator> decorators) {
		super(muxer, listener, videoSetting);
		mMediaProjection = projection;
		mDensity = density;
		int _fps = videoSetting.getFPS();
		int _bitrate = videoSetting.getBirate();
		fps = (_fps > 0 && _fps <= 30) ? _fps : FRAME_RATE;
		bitrate = (_bitrate > 0) ? _bitrate : calcBitRate(_fps);

		final HandlerThread thread = new HandlerThread(TAG);
		thread.start();
		mHandler = new Handler(thread.getLooper());
		mDecors = decorators;
	}

	@Override
	protected void release() {
		mHandler.getLooper().quit();
		super.release();
	}

	@Override
	public void prepare() throws IOException {
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
	public void stopStreaming() {
		if (DEBUG) Log.v(TAG,  "stopStreaming:");
		synchronized (mSync) {
			mIsRecording = false;
			mSync.notifyAll();
		}
		super.stopStreaming();
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

			prepareTextures();

			mSourceTexture = new SurfaceTexture(mTexId);
			mSourceTexture.setDefaultBufferSize(mWidth, mHeight);	// これを入れないと映像が取れない

			mSourceSurface = new Surface(mSourceTexture);

			mSourceTexture.setOnFrameAvailableListener(mOnFrameAvailableListener, mHandler);

			mEncoderSurface = getEgl().createFromSurface(mSurface);

	    	if (DEBUG)
	    		Log.d(TAG,"setup VirtualDisplay");
			intervals = (long)(1000f / fps);
		    display = mMediaProjection.createVirtualDisplay(
		    	"Capturing Display",
		    	mWidth, mHeight, mDensity,
		    	DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
		    	mSourceSurface, null, mHandler);
			if (DEBUG) Log.v(TAG,  "screen capture loop:display=" + display);

			queueEvent(mDrawTask);
		}

		private void prepareTextures() {
			RenderUtil.CustomDecorator mScreen = new RenderUtil.CustomDecorator(null, new Size(mWidth, mHeight), new Point(0,0));
			mScreen.setTextureId(mTexId);
			mDecors.add(0, mScreen);
//			Log.d(TAG, "prepareTextures: ");
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
		int x = 0, y = 0;
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
						//Todo: draw decorators here
						mSourceTexture.updateTexImage();
						mSourceTexture.getTransformMatrix(mTexMatrix);
					}

					mEncoderSurface.makeCurrent();


					for(RenderUtil.CustomDecorator decor: mDecors)
						decor.updateTexId();

					RenderUtil.renderTextures(mDecors);
					mEncoderSurface.swap();

					makeCurrent();


					/**START ENCODE**/
					AtomicInteger videoFrameCacheNumber = mMuxerWrapper.getVideoFrameCacheNumber();
					if (videoFrameCacheNumber != null && videoFrameCacheNumber.get() < SrsEncoder.VGOP) {


						final long pts = getPresentTimeUS();
						ByteBuffer buffer = getCurrentByteBuffer();
						byte[] bytes = buffer.array();
						MyUtils.logBytes("before decode", bytes);

//					if(useSoftEncoder)
						swRgbaFrame(bytes, mWidth, mHeight, pts);
//					else {
//						byte[] processedData = hwRgbaFrame(bytes, mWidth, mHeight);
//						if (processedData != null) {
//							onProcessedYuvFrame(processedData, pts);
//						} else {
//							Log.e(TAG, "on encode buffer", new IllegalArgumentException("libyuv failure"));
//						}
//					}
//					shootPicture(buffer);
						/**END ENCODE**/

						frameAvailableSoon();


						queueEvent(this);
					}
				} else {
					releaseSelf();
				}
			}
		};


	}

	private void shootPicture(ByteBuffer buf) {

		Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.PNG;

		File captureFile = new File(MyUtils.getBaseStorageDirectory(), MyUtils.createFileName(".jpg"));

		if (!captureFile.getParentFile().exists()) {
			captureFile.getParentFile().mkdirs();
		}

		if (captureFile.toString().endsWith(".jpg")) {
			compressFormat = Bitmap.CompressFormat.JPEG;
		}
		BufferedOutputStream os = null;
		try {
			try {
				os = new BufferedOutputStream(new FileOutputStream(captureFile));
				final Bitmap bmp = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
				buf.clear();
				bmp.copyPixelsFromBuffer(buf);
				bmp.compress(compressFormat, 100, os);
				bmp.recycle();
				os.flush();
			} finally {
				if (os != null) os.close();
			}
		} catch (final FileNotFoundException e) {
			Log.w(TAG, "failed to save file", e);
		} catch (final IOException e) {
			Log.w(TAG, "failed to save file", e);
		}
	}

	private ByteBuffer getCurrentByteBuffer() {
		ByteBuffer buf = null;
		buf = ByteBuffer.allocateDirect(mWidth * mHeight * 4);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		GLES20.glReadPixels(0, 0, mWidth, mHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf);
		return buf;
	}
}
