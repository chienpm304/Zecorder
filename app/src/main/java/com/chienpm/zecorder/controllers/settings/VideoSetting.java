package com.chienpm.zecorder.controllers.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.util.DisplayMetrics;

public class VideoSetting {
    public static final int ORIENTATION_PORTRAIT = 0;
    public static final int ORIENTATION_LANDSCAPE = 1;

    int mWidth, mHeight, mFPS, mBirate, mOrientation;
//    public static VideoSetting VIDEO_PROFILE_SD = new VideoSetting(480, 360, 30, 1000 * 1024, ORIENTATION_LANDSCAPE);
    public static VideoSetting VIDEO_PROFILE_SD = new VideoSetting(640, 360, 30, 11200 * 1024, ORIENTATION_LANDSCAPE);
    public static VideoSetting VIDEO_PROFILE_HD = new VideoSetting(1280, 720, 30, 2000 * 1024, ORIENTATION_LANDSCAPE);
    public static VideoSetting VIDEO_PROFILE_FHD = new VideoSetting(1920, 1080, 30, 4000 * 1024, ORIENTATION_LANDSCAPE);
    private String mOutputPath;

    public static String getShortResolution(int width, int height){
        int factor = width;
        if(width<height)
            factor = height;
        switch (factor){
            case 1920: return "FHD";
            case 1280: return "HD";
            case 480: return "SD";
        }
        return "FIT";
    }

    public VideoSetting(int width, int height, int FPS, int bitrate, int orientation) {
        mWidth = width;
        mHeight = height;
        mFPS = FPS;
        mBirate = bitrate;
        mOrientation = orientation;
    }

    //Size: bytes
    @SuppressLint("DefaultLocale")
    public static String getFormattedSize(long bytes) {
        Boolean si = true;
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public static String getFormattedDuration(long duration) {
        long s = duration;
        return String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, (s % 60));
    }

    public static String getFormattedBitrate(int bitrate) {
        return bitrate/1024 + " Kbps";
    }

    public static VideoSetting getFitDeviceResolution(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();

        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;
        if(height<width){
            swap(width,height);
        }
        return new VideoSetting(width, height, 30, 4000 * 1024, ORIENTATION_LANDSCAPE);

    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public int getFPS() {
        return mFPS;
    }

    public int getBitrate() {
        return mBirate;
    }

    public void setFPS(int _fps) {
        mFPS = (_fps > 0 && _fps <= 30) ? _fps : 25;
    }

    public void setBitrate(int bitrate) {
        mBirate = bitrate;
    }

    public void setOrientation(int orientation){
        mOrientation = orientation;
    }

    public int getOrientation() {
        return mOrientation;
    }

    public String getOutputPath() {
        return mOutputPath;
    }

    public String getResolutionString() {
        return mWidth+"x"+mHeight;
    }

    public void setOutputPath(String outputFile) {
        mOutputPath = outputFile;
    }

    @Override
    public String toString() {
        return "VideoSetting{" +
                "mWidth=" + mWidth +
                ", mHeight=" + mHeight +
                ", mFPS=" + mFPS +
                ", mBirate=" + mBirate +
                ", mOrientation=" + mOrientation +
                '}';
    }


    public String getTitle() {
        if(!TextUtils.isEmpty(mOutputPath)){
            return mOutputPath.substring(mOutputPath.lastIndexOf('/'));
        }
        else return "Title ERROR";
    }

    public void swapResolutionMatchToOrientation() {
        int x = mWidth; int y = mHeight;
        //default is landscape
        if((mOrientation == VideoSetting.ORIENTATION_PORTRAIT && mWidth>mHeight) ||
            (mOrientation == VideoSetting.ORIENTATION_LANDSCAPE && mWidth<mHeight)){
            //swap width and height
            mWidth = mWidth + mHeight;
            mHeight = mWidth - mHeight;
            mWidth = mWidth - mHeight;
        }
    }

    private static void swap(int x, int y) {

    }
}
