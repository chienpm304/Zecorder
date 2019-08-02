package com.chienpm.zecorder.controllers.streaming;
/*
 * ScreenRecordingSample
 * Sample project to cature and save audio from internal and video from screen as MPEG4 file.
 *
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 *
 * File name: StreamMuxerWrapper.java
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * All files in the folder are under this Apache License, Version 2.0.
*/

import android.content.Context;
import android.media.MediaCodec;
import android.os.AsyncTask;
import android.util.Log;

import com.chienpm.zecorder.controllers.settings.VideoSetting;
import com.octiplex.android.rtmp.RtmpConnectionListener;
import com.octiplex.android.rtmp.RtmpMuxer;
import com.octiplex.android.rtmp.Time;


import java.io.IOException;
import java.nio.ByteBuffer;

public class StreamMuxerWrapper {
	private static final boolean DEBUG = true;	// TODO set false on release
	private static final String TAG = "chienpm_log_stream";
	private final VideoSetting mVideoSetting;
	private StreamProfile mStreamProfile;

	private String mOutputPath = "";
//	private final MediaMuxer mMediaMuxer;	// API >= 18
	private RtmpMuxer mMuxer;
	private int mEncoderCount, mStatredCount;
	private boolean mIsStarted;
	private volatile boolean mIsPaused;
	private StreamEncoder mVideoEncoder, mAudioEncoder;
	private RtmpConnectionListener listener = new RtmpConnectionListener() {
		@Override
		public void onConnected() {

		}

		@Override
		public void onReadyToPublish() {

		}

		@Override
		public void onConnectionError(IOException e) {

		}
	};

	public StreamMuxerWrapper(final Context context, final StreamProfile streamProfile, VideoSetting videoSetting) throws IOException {
		mStreamProfile = streamProfile;
		mVideoSetting = videoSetting;

//		Log.d(TAG, "StreamMuxerWrapper: open stream+" + streamProfile.toString());

		initMuxer();

		mEncoderCount = mStatredCount = 0;
		mIsStarted = false;


	}

	private void initMuxer()
	{
		mMuxer = new RtmpMuxer(mStreamProfile.getHost(), mStreamProfile.getPort(), new Time()
		{
			@Override
			public long getCurrentTimestamp()
			{
				return System.currentTimeMillis();
			}
		});

		// Always call start method from a background thread.
		new AsyncTask<Void, Void, Void>()
		{
			@Override
			protected Void doInBackground(Void... params)
			{
				mMuxer.start(listener, "app", null, null);
				return null;
			}
		}.execute();
	}

	public synchronized void prepare() throws IOException {
		if (mVideoEncoder != null)
			mVideoEncoder.prepare();
		if (mAudioEncoder != null)
			mAudioEncoder.prepare();
	}

	public synchronized void startRecording() {
//		if(!isStreamConnected()){
//			int res = mMuxer.open(mStreamProfile.getStreamUrl(), mVideoSetting.getWidth(), mVideoSetting.getHeight());
//			if(res <=0 )
//			{
//				Log.d(TAG, "startRecording: FATAL ERROR: cannot connect to stream");
//			}
//		}
			
		if (mVideoEncoder != null)
			mVideoEncoder.startRecording();
		if (mAudioEncoder != null)
			mAudioEncoder.startRecording();
	}

	public synchronized void stopRecording() {
//		mMuxer.close();

		if (mVideoEncoder != null)
			mVideoEncoder.stopRecording();
		mVideoEncoder = null;
		if (mAudioEncoder != null)
			mAudioEncoder.stopRecording();
		mAudioEncoder = null;
	}

	public synchronized boolean isStarted() {
		return mIsStarted;
	}

	public synchronized void pauseRecording() {
		mIsPaused = true;
		if (mVideoEncoder != null)
			mVideoEncoder.pauseRecording();
		if (mAudioEncoder != null)
			mAudioEncoder.pauseRecording();
	}

	public synchronized void resumeRecording() {
		if (mVideoEncoder != null)
			mVideoEncoder.resumeRecording();
		if (mAudioEncoder != null)
			mAudioEncoder.resumeRecording();
		mIsPaused = false;
	}

	public synchronized boolean isPaused() {
		return mIsPaused;
	}

//**********************************************************************
//**********************************************************************
	/**
	 * assign encoder to this calss. this is called from encoder.
	 * @param encoder instance of StreamVideoEncoderBase
	 */
	/*package*/
	void addEncoder(final StreamEncoder encoder) {
		if (encoder instanceof StreamVideoEncoderBase) {
			if (mVideoEncoder != null)
				throw new IllegalArgumentException("Video encoder already added.");

			mVideoEncoder = encoder;
		} else if (encoder instanceof StreamAudioEncoder) {
			if (mAudioEncoder != null)
				throw new IllegalArgumentException("Video encoder already added.");
			mAudioEncoder = encoder;
		} else
			throw new IllegalArgumentException("unsupported encoder");

		mEncoderCount = (mVideoEncoder != null ? 1 : 0) + (mAudioEncoder != null ? 1 : 0);
	}

	/**
	 * request start recording from encoder
	 * @return true when muxer is ready to write
	 */
	/*package*/
	synchronized boolean start() {
		if (DEBUG) Log.v(TAG,  "start:");
		mStatredCount++;
		if ((mEncoderCount > 0) && (mStatredCount == mEncoderCount)) {
//			mMediaMuxer.start();
			mIsStarted = true;
			notifyAll();
			if (DEBUG) Log.v(TAG,  "MediaMuxer started:");
		}
		return mIsStarted;
	}

	/**
	 * request stop recording from encoder when encoder received EOS
	*/
	/*package*/ synchronized void stop() {
		if (DEBUG) Log.v(TAG,  "stop:mStatredCount=" + mStatredCount);
		mStatredCount--;
		if ((mEncoderCount > 0) && (mStatredCount <= 0)) {
			mMuxer.stop();
			mIsStarted = false;
			if (DEBUG) Log.v(TAG,  "MediaMuxer stopped:");
		}
	}

	/**
	 * assign encoder to muxer
	 * @param format
	 * @return minus value indicate error
	 */
	/*package*/
//	synchronized int addTrack(final MediaFormat format) {
//		if (mIsStarted)
//			throw new IllegalStateException("muxer already started");
//		final int trackIx = mMediaMuxer.addTrack(format);
//		if (DEBUG) Log.i(TAG, "addTrack:trackNum=" + mEncoderCount + ",trackIx=" + trackIx + ",format=" + format);
//		return trackIx;
//	}

	/**
	 * write encoded data to muxer
	 * @param byteBuf
	 * @param bufferInfo
	 */
	/*package*/
	synchronized void writeSampleData(StreamEncoder encoder, final ByteBuffer byteBuf, final MediaCodec.BufferInfo bufferInfo) {
		if(mMuxer.isStarted()){
			Log.d(TAG, "writeSampleData: FALTAL ERROR stream is offline");
		}
		else if (mStatredCount > 0) {
			if(encoder instanceof StreamVideoEncoderBase) {
//				mMuxer.(byteBuf.array(), bufferInfo.offset, bufferInfo.size, (int) bufferInfo.presentationTimeUs);
				Log.d(TAG, "writeVIDEO: offset:"+bufferInfo.offset+"  -size:"+bufferInfo.size);
			}
			else if(encoder instanceof StreamAudioEncoder) {
//				mMuxer.writeAudio(byteBuf.array(), bufferInfo.offset, bufferInfo.size, (int) bufferInfo.presentationTimeUs);
				Log.d(TAG, "writeAudio: offset:"+bufferInfo.offset+"  -size:"+bufferInfo.size);
			}
			else {
				Log.d(TAG, "writeSampleData: WRONG ENCODER TYPE");
			}
//			mMediaMuxer.writeSampleData(trackIndex, byteBuf, bufferInfo);
		}
	}

//**********************************************************************
//**********************************************************************
}
