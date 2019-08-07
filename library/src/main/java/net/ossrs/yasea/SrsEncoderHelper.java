package net.ossrs.yasea;

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
}
