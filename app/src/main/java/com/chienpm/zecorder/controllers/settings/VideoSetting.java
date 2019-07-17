package com.chienpm.zecorder.controllers.settings;

public class VideoSetting {
    public static final int ORIENTATION_PORTRAIT = 0;
    public static final int ORIENTATION_LANDSCAPE = 1;

    int mWidth, mHeight, mFPS, mBirate, mOrientation;
    public static VideoSetting VIDEO_PROFILE_SD = new VideoSetting(480, 360, 30, 1000 * 1024, ORIENTATION_LANDSCAPE);
    public static VideoSetting VIDEO_PROFILE_HD = new VideoSetting(1280, 720, 30, 2000 * 1024, ORIENTATION_LANDSCAPE);
    public static VideoSetting VIDEO_PROFILE_FHD = new VideoSetting(1920, 1080, 30, 4000 * 1024, ORIENTATION_LANDSCAPE);

    public VideoSetting(int width, int height, int FPS, int birate, int orientation) {
        mWidth = width;
        mHeight = height;
        mFPS = FPS;
        mBirate = birate;
        mOrientation = orientation;
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

    public int getBirate() {
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

    public String getResolutionString() {
        return mWidth+"x"+mHeight;
    }
}
