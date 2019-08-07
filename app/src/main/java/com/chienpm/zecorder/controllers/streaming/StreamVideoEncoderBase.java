package com.chienpm.zecorder.controllers.streaming;
/*
 * ScreenRecordingSample
 * Sample project to capture and save audio from internal and video from screen as MPEG4 file.
 *
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 *
 * File name: StreamVideoEncoderBase.java
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

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import com.chienpm.zecorder.controllers.settings.VideoSetting;

import net.ossrs.yasea.SrsEncoderHelper;

import java.io.IOException;
import java.lang.annotation.Native;

import static net.ossrs.yasea.SrsEncoder.VCODEC;
import static net.ossrs.yasea.SrsEncoder.VFPS;
import static net.ossrs.yasea.SrsEncoder.VGOP;
import static net.ossrs.yasea.SrsEncoder.vBitrate;
import static net.ossrs.yasea.SrsEncoder.x264Preset;


public abstract class StreamVideoEncoderBase extends StreamEncoder {

	private static final boolean DEBUG = false;	// TODO set false on release
	private static final String TAG = StreamVideoEncoderBase.class.getSimpleName();

	// parameters for recording
    private static final float BPP = 0.25f;

    protected final int mWidth;
    protected final int mHeight;
	private boolean useSoftEncoder = true;
	private MediaCodecInfo vmci;
	private boolean canSoftEncode = false;
	private int mVideoColorFormat;
	private StreamMuxerWrapper mMuxer;

	public StreamVideoEncoderBase(StreamMuxerWrapper muxer, StreamEncoderListener listener, VideoSetting videoSetting) {
		super(muxer, listener);
		mMuxer = muxer;
		int width = videoSetting.getWidth();
		int height = videoSetting.getHeight();
		if(videoSetting.getOrientation() == VideoSetting.ORIENTATION_PORTRAIT) {
			mWidth = height;
			mHeight = width;
		}
		else{
			mWidth = width;
			mHeight = height;
		}
		mVideoColorFormat = chooseVideoEncoder();
	}


	/**
	 * エンコーダー用のMediaFormatを生成する。prepare_surface_encoder内から呼び出される
	 * @param mime
	 * @param frame_rate
	 * @param bitrate
	 * @return
	 */
	protected MediaFormat create_encoder_format(final String mime, final int frame_rate, final int bitrate) {
		if (DEBUG) Log.v(TAG, String.format("create_encoder_format:(%d,%d),mime=%s,frame_rate=%d,bitrate=%d", mWidth, mHeight, mime, frame_rate, bitrate));
//        final MediaFormat format = MediaFormat.createVideoFormat(mime, mWidth, mHeight);
//        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);	// API >= 18
//        format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate > 0 ? bitrate : calcBitRate(frame_rate));
//        format.setInteger(MediaFormat.KEY_FRAME_RATE, frame_rate);
//        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);
//		return format;
		MediaFormat videoFormat = MediaFormat.createVideoFormat(VCODEC, mWidth, mHeight);
		videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, mVideoColorFormat);
		videoFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0);
		videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, vBitrate);
		videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, VFPS);
		videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, VGOP / VFPS);
		return videoFormat;
	}

	protected Surface prepare_surface_encoder(final String mime, final int frame_rate, final int bitrate)
		throws IOException, IllegalArgumentException {

		if (DEBUG) Log.v(TAG, String.format("prepare_surface_encoder:(%d,%d),mime=%s,frame_rate=%d,bitrate=%d", mWidth, mHeight, mime, frame_rate, bitrate));

		if (!useSoftEncoder && (mWidth % 32 != 0 || mHeight % 32 != 0)) {
			if (vmci.getName().contains("MTK")) {
				//throw new AssertionError("MTK encoding revolution stride must be 32x");
			}
		}

		SrsEncoderHelper.getInstance().setEncoderResolution(mWidth, mHeight);
		SrsEncoderHelper.getInstance().setEncoderFps(VFPS);
		SrsEncoderHelper.getInstance().setEncoderGop(VGOP);
		SrsEncoderHelper.getInstance().setEncoderBitrate(vBitrate);
		SrsEncoderHelper.getInstance().setEncoderPreset(x264Preset);

		if (useSoftEncoder) {
			canSoftEncode = SrsEncoderHelper.getInstance().openSoftEncoder();
			if (!canSoftEncode) {
				Log.d(TAG, "prepare_surface_encoder: CANNOT OPEN SOFTENCODER");
				return null;
			}
		}


		mTrackIndex = -1;
        mMuxerStarted = mIsEOS = false;

		try {
			mMediaCodec = MediaCodec.createByCodecName(vmci.getName());
		} catch (IOException e) {
			Log.e(TAG, "create vencoder failed.");
			e.printStackTrace();
			return null;
		}

		final MediaFormat format = create_encoder_format(mime, frame_rate, bitrate);

        mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mTrackIndex = mMuxer.addTrack(format);
        // get Surface for encoder input
        // this method only can call between #configure and #start
        return mMediaCodec.createInputSurface();	// API >= 18
	}

	protected int calcBitRate(final int frameRate) {
		final int bitrate = (int)(BPP * frameRate * mWidth * mHeight);
		Log.i(TAG, String.format("bitrate=%5.2f[Mbps]", bitrate / 1024f / 1024f));
		return bitrate;
	}

    /**
     * select the first codec that match a specific MIME type
     * @param mimeType
     * @return null if no codec matched
     */
	protected static final MediaCodecInfo selectVideoCodec(final String mimeType) {
    	if (DEBUG) Log.v(TAG, "selectVideoCodec:");

    	// get the list of available codecs
        final int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
        	final MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder()) {	// skipp decoder
                continue;
            }
            // select first codec that match a specific MIME type and color format
            final String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                	if (DEBUG) Log.i(TAG, "codec:" + codecInfo.getName() + ",MIME=" + types[j]);
            		final int format = selectColorFormat(codecInfo, mimeType);
                	if (format > 0) {
                		return codecInfo;
                	}
                }
            }
        }
        return null;
    }

    /**
     * select color format available on specific codec and we can use.
     * @return 0 if no colorFormat is matched
     */
    protected static final int selectColorFormat(final MediaCodecInfo codecInfo, final String mimeType) {
		if (DEBUG) Log.i(TAG, "selectColorFormat: ");
    	int result = 0;
    	final MediaCodecInfo.CodecCapabilities caps;
    	try {
    		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
    		caps = codecInfo.getCapabilitiesForType(mimeType);
    	} finally {
    		Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
    	}
        int colorFormat;
        for (int i = 0; i < caps.colorFormats.length; i++) {
        	colorFormat = caps.colorFormats[i];
            if (isRecognizedViewoFormat(colorFormat)) {
            	if (result == 0)
            		result = colorFormat;
                break;
            }
        }
        if (result == 0)
        	Log.e(TAG, "couldn't find a good color format for " + codecInfo.getName() + " / " + mimeType);
        return result;
    }

	/**
	 * color formats that we can use in this class
	 */
    protected static int[] recognizedFormats;
	static {
		recognizedFormats = new int[] {
//        	MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar,
//        	MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
//        	MediaCodecInfo.CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar,
        	MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface,
		};
	}

    protected static final boolean isRecognizedViewoFormat(final int colorFormat) {
		if (DEBUG) Log.i(TAG, "isRecognizedViewoFormat:colorFormat=" + colorFormat);
    	final int n = recognizedFormats != null ? recognizedFormats.length : 0;
    	for (int i = 0; i < n; i++) {
    		if (recognizedFormats[i] == colorFormat) {
    			return true;
    		}
    	}
    	return false;
    }

    @Override
    protected void signalEndOfInputStream() {
		if (DEBUG) Log.d(TAG, "sending EOS to encoder");
		mMediaCodec.signalEndOfInputStream();	// API >= 18
		mIsEOS = true;
	}

	// choose the right supported color format. @see below:
	private int chooseVideoEncoder() {
		// choose the encoder "video/avc":
		//      1. select default one when type matched.
		//      2. google avc is unusable.
		//      3. choose qcom avc.
		vmci = chooseVideoEncoder(null);
		//vmci = chooseVideoEncoder("google");
		//vmci = chooseVideoEncoder("qcom");

		int matchedColorFormat = 0;
		MediaCodecInfo.CodecCapabilities cc = vmci.getCapabilitiesForType(VCODEC);
		for (int i = 0; i < cc.colorFormats.length; i++) {
			int cf = cc.colorFormats[i];
			Log.i(TAG, String.format("vencoder %s supports color fomart 0x%x(%d)", vmci.getName(), cf, cf));

			// choose YUV for h.264, prefer the bigger one.
			// corresponding to the color space transform in onPreviewFrame
			if (cf >= cc.COLOR_FormatYUV420Planar && cf <= cc.COLOR_FormatYUV420SemiPlanar) {
				if (cf > matchedColorFormat) {
					matchedColorFormat = cf;
				}
			}
		}

		for (int i = 0; i < cc.profileLevels.length; i++) {
			MediaCodecInfo.CodecProfileLevel pl = cc.profileLevels[i];
			Log.i(TAG, String.format("vencoder %s support profile %d, level %d", vmci.getName(), pl.profile, pl.level));
		}

		Log.i(TAG, String.format("vencoder %s choose color format 0x%x(%d)", vmci.getName(), matchedColorFormat, matchedColorFormat));
		return matchedColorFormat;
	}

	// choose the video encoder by name.
	private MediaCodecInfo chooseVideoEncoder(String name) {
		int nbCodecs = MediaCodecList.getCodecCount();
		for (int i = 0; i < nbCodecs; i++) {
			MediaCodecInfo mci = MediaCodecList.getCodecInfoAt(i);
			if (!mci.isEncoder()) {
				continue;
			}

			String[] types = mci.getSupportedTypes();
			for (int j = 0; j < types.length; j++) {
				if (types[j].equalsIgnoreCase(VCODEC)) {
					Log.i(TAG, String.format("vencoder %s types: %s", mci.getName(), types[j]));
					if (name == null) {
						return mci;
					}

					if (mci.getName().contains(name)) {
						return mci;
					}
				}
			}
		}

		return null;
	}
}
