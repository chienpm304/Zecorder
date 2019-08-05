package com.chienpm.zecorder.controllers.streaming;
/*
 * ScreenRecordingSample
 * Sample project to cature and save audio from internal and video from screen as MPEG4 file.
 *
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 *
 * File name: MediaMuxerWrapper.java
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
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.text.TextUtils;
import android.util.Log;

import com.chienpm.zecorder.controllers.settings.VideoSetting;
import com.chienpm.zecorder.ui.utils.MyUtils;

import net.ossrs.rtmp.ConnectCheckerRtmp;
import net.ossrs.rtmp.SrsFlvMuxer;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class StreamMuxerWrapper {
	private static final boolean DEBUG = false;    // TODO set false on release
	private static final String TAG = "chienpm_log_stream";
	private final StreamProfile mStreamProfile;
	private final VideoSetting mVideoSetting;

	private final SrsFlvMuxer mMuxer;    // API >= 18
	private int mEncoderCount, mStatredCount;
	private boolean mIsStarted;
	private volatile boolean mIsPaused;
	private StreamEncoder mVideoEncoder, mAudioEncoder;
	private boolean mStreamConnected = false;


	/**
	 * Constructor
	 *
	 * @param _ext extension of output file
	 * @throws IOException
	 */
	public StreamMuxerWrapper(final Context context, final StreamProfile streamProfile, VideoSetting videoSetting) throws IOException {

		mStreamProfile = streamProfile;

		mVideoSetting = videoSetting;

		mMuxer = initMuxer();
	}

	private SrsFlvMuxer initMuxer() {
		SrsFlvMuxer mMuxer;
		mMuxer = new SrsFlvMuxer(mConnectCheckerRtmp);

		mMuxer.setVideoResolution(mVideoSetting.getWidth(), mVideoSetting.getHeight());

		mEncoderCount = mStatredCount = 0;
		mIsStarted = false;
		//Todo: test strem
		mMuxer.start("rtmp://ingest-seo.mixer.com:1935/beam/93296292-9wazrlzmhpoypk8bl5r3p89isx3wut7e");

		return mMuxer;
	}

	public synchronized void prepare() throws IOException {
		if (mVideoEncoder != null)
			mVideoEncoder.prepare();
		if (mAudioEncoder != null)
			mAudioEncoder.prepare();
	}

	public synchronized void startRecording() {
		if (mVideoEncoder != null)
			mVideoEncoder.startRecording();
		if (mAudioEncoder != null)
			mAudioEncoder.startRecording();
	}

	public synchronized void stopRecording() {
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
	 *
	 * @param encoder instance of MediaVideoEncoderBase
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
	 *
	 * @return true when muxer is ready to write
	 */
	/*package*/
	synchronized boolean start() {
		if (DEBUG) Log.v(TAG, "start:");
		mStatredCount++;
		if ((mEncoderCount > 0) && (mStatredCount == mEncoderCount)) {

			mIsStarted = true;
			notifyAll();
			if (DEBUG) Log.v(TAG, "MediaMuxer started:");

		}
		return mIsStarted;
	}

	/**
	 * request stop recording from encoder when encoder received EOS
	 */
	/*package*/
	synchronized void stop() {
		if (DEBUG) Log.v(TAG, "stop:mStatredCount=" + mStatredCount);
		mStatredCount--;
		if ((mEncoderCount > 0) && (mStatredCount <= 0)) {
			mMuxer.stop();
//			mMuxer.release();
			mIsStarted = false;
			mStreamConnected = false;
			if (DEBUG) Log.v(TAG, "MediaMuxer stopped:");
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
//		final int trackIx = mMuxer.addTrack(format);
//		if (DEBUG) Log.i(TAG, "addTrack:trackNum=" + mEncoderCount + ",trackIx=" + trackIx + ",format=" + format);
//		return trackIx;
//	}

	/**
	 * write encoded data to muxer
	 *
	 * @param trackIndex
	 * @param byteBuf
	 * @param bufferInfo
	 */
	/*package*/
	synchronized void writeSampleData(StreamEncoder encoder, final ByteBuffer byteBuf, final MediaCodec.BufferInfo bufferInfo) {
		if (mStatredCount > 0 && mStreamConnected) {
			if (encoder instanceof StreamAudioEncoder) {
				mMuxer.sendAudio(byteBuf, bufferInfo);
				Log.d(TAG, "write AUDIO offset: " + bufferInfo.presentationTimeUs + " size: " + bufferInfo.size);
			} else if (encoder instanceof StreamVideoEncoderBase) {
				mMuxer.sendVideo(byteBuf, bufferInfo);
				Log.d(TAG, "write VIDEO offset: " + bufferInfo.presentationTimeUs + " size: " + bufferInfo.size);
			} else {
				Log.d(TAG, "writeSampleData: error");
			}
		}

	}


	private final ConnectCheckerRtmp mConnectCheckerRtmp = new ConnectCheckerRtmp() {
		@Override
		public void onConnectionSuccessRtmp() {
			Log.d(TAG, "ConnectCheckerRtmp: CONNECTION Success");
			mStreamConnected = true;
		}

		@Override
		public void onConnectionFailedRtmp(String reason) {
			Log.d(TAG, "ConnectCheckerRtmp: CONNECTION Failed");
			mStreamConnected = false;
			mMuxer.reConnect(1000);
		}

		@Override
		public void onNewBitrateRtmp(long bitrate) {
//			Log.d(TAG, "ConnectCheckerRtmp: new Bitrate "+bitrate);
		}

		@Override
		public void onDisconnectRtmp() {
			Log.d(TAG, "ConnectCheckerRtmp: Disconnected");
			mStreamConnected = false;
		}

		@Override
		public void onAuthErrorRtmp() {
			Log.d(TAG, "ConnectCheckerRtmp: Auth Error");
		}

		@Override
		public void onAuthSuccessRtmp() {
			Log.d(TAG, "ConnectCheckerRtmp: Auth Success");
		}
	};
//**********************************************************************
//**********************************************************************
}
