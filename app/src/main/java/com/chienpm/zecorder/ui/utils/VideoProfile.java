package com.chienpm.zecorder.ui.utils;

public class VideoProfile {
    int mWidth, mHeight, mFPS, mBirate;
    public static VideoProfile VIDEO_PROFILE_SD = new VideoProfile(480, 360, 30, 1000 * 1024);
    public static VideoProfile VIDEO_PROFILE_HD = new VideoProfile(1280, 720, 30, 2000 * 1024);
    public static VideoProfile VIDEO_PROFILE_FHD = new VideoProfile(1920, 1080, 30, 4000 * 1024);

    public VideoProfile(int width, int height, int FPS, int birate) {
        mWidth = width;
        mHeight = height;
        mFPS = FPS;
        mBirate = birate;
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

    @Override
    public String toString() {
        return "VideoProfile{" +
                "mWidth=" + mWidth +
                ", mHeight=" + mHeight +
                ", mFPS=" + mFPS +
                ", mBirate=" + mBirate +
                '}';
    }
}
