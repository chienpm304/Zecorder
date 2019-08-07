package net.ossrs.yasea;

import android.media.MediaCodec;

import java.nio.ByteBuffer;

public class SrsEncoderHelper {
    private static final SrsEncoderHelper ourInstance = new SrsEncoderHelper();

    public static SrsEncoderHelper getInstance() {
        return ourInstance;
    }

    private SrsEncoderHelper() {
    }
    public native void setEncoderResolution(int outWidth, int outHeight);
    public native void setEncoderFps(int fps);
    public native void setEncoderGop(int gop);
    public native void setEncoderBitrate(int bitrate);
    public native void setEncoderPreset(String preset);
    public native byte[] RGBAToI420(byte[] frame, int width, int height, boolean flip, int rotate);
    public native byte[] RGBAToNV12(byte[] frame, int width, int height, boolean flip, int rotate);
    public native byte[] ARGBToI420Scaled(int[] frame, int width, int height, boolean flip, int rotate, int crop_x, int crop_y,int crop_width, int crop_height);
    public native byte[] ARGBToNV12Scaled(int[] frame, int width, int height, boolean flip, int rotate, int crop_x, int crop_y,int crop_width, int crop_height);
    public native byte[] ARGBToI420(int[] frame, int width, int height, boolean flip, int rotate);
    public native byte[] ARGBToNV12(int[] frame, int width, int height, boolean flip, int rotate);
    public native byte[] NV21ToNV12Scaled(byte[] frame, int width, int height, boolean flip, int rotate, int crop_x, int crop_y,int crop_width, int crop_height);
    public native byte[] NV21ToI420Scaled(byte[] frame, int width, int height, boolean flip, int rotate, int crop_x, int crop_y,int crop_width, int crop_height);
    public native int RGBASoftEncode(byte[] frame, int width, int height, boolean flip, int rotate, long pts);
    public native boolean openSoftEncoder();
    public native void closeSoftEncoder();

    static {
        System.loadLibrary("yuv");
        System.loadLibrary("enc");
    }

    public void swRgbaFrame(byte[] data, int width, int height, long pts) {
        RGBASoftEncode(data, width, height, true, 180, pts);
    }

    private void onSoftEncodedData(byte[] es, long pts, boolean isKeyFrame) {
        ByteBuffer bb = ByteBuffer.wrap(es);
        MediaCodec.BufferInfo vebi = new MediaCodec.BufferInfo();
        vebi.offset = 0;
        vebi.size = es.length;
        vebi.presentationTimeUs = pts;
        vebi.flags = isKeyFrame ? MediaCodec.BUFFER_FLAG_KEY_FRAME : 0;
        onEncodedAnnexbFrame(bb, vebi);
    }

    // when got encoded h264 es stream.
    private void onEncodedAnnexbFrame(ByteBuffer es, MediaCodec.BufferInfo bi) {
//        mp4Muxer.writeSampleData(videoMp4Track, es.duplicate(), bi);
        flvMuxer.writeSampleData(videoFlvTrack, es, bi);
    }
}
