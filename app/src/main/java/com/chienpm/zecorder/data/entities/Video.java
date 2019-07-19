package com.chienpm.zecorder.data.entities;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import com.chienpm.zecorder.controllers.settings.VideoSetting;

@Entity(tableName = "videos")
public class Video {
    @NonNull
    @PrimaryKey( autoGenerate = true)
    @ColumnInfo(name = "id")
    private int mId;

    @ColumnInfo(name = "tittle")
    private String mTitle;

    @ColumnInfo(name = "duration")
    private long mDuration;

    @ColumnInfo(name = "bitrate")
    private int mBitrate;

    @ColumnInfo(name = "fps")
    private int mFps;

    @ColumnInfo(name = "width")
    private int mWidth;

    @ColumnInfo(name = "height")
    private int mHeight;

    @ColumnInfo(name = "size")
    private long mSize;

    @ColumnInfo(name = "local_path")
    private String mLocalPath;

    @ColumnInfo(name = "createdAt")
    private String mCreateAt;

    @ColumnInfo(name = "synced")
    private Boolean mSynced;

    @ColumnInfo(name = "cloud_path")
    private String mCloudPath;

    public Video(){}

    public Video(String title, long duration, int bitrate, int fps, int width, int height, long size, String localPath, String createAt, Boolean isSynced, String cloudPath) {
        mTitle = title;
        mDuration = duration;
        mBitrate = bitrate;
        mFps = fps;
        mWidth = width;
        mHeight = height;
        mSize = size;
        mLocalPath = localPath;
        mCreateAt = createAt;
        mSynced = isSynced;
        mCloudPath = cloudPath;
    }

    public Video(VideoSetting setting) {
//        mTitle = "";
//        mDuration = 0; //todo: update duration
//        mBitrate = setting.getBirate();
//        mFps = setting.getFPS();
//        mResolution = setting.getResolutionString();
//        mSize = 0; //todo: update duration
//        mLocalPath = localPath;
//        mCreateAt = createAt;
//        mSynced = isSynced;
//        mCloudPath = cloudPath;
    }

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        mId = id;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public int getWidth() {
        return mWidth;
    }

    public void setDuration(long duration) {
        mDuration = duration;
    }

    public int getBitrate() {
        return mBitrate;
    }

    public void setBitrate(int bitrate) {
        mBitrate = bitrate;
    }

    public int getFps() {
        return mFps;
    }

    public void setFps(int fps) {
        mFps = fps;
    }

    public String getResolution() {
        return mWidth+"x"+mHeight;
    }

    public long getDuration() {
        return mDuration;
    }

    public void setWidth(int width) {
        mWidth = width;
    }

    public int getHeight() {
        return mHeight;
    }

    public void setHeight(int height) {
        mHeight = height;
    }

    public long getSize() {
        return mSize;
    }

    public void setSize(long size) {
        mSize = size;
    }

    public String getLocalPath() {
        return mLocalPath;
    }

    public void setLocalPath(String localPath) {
        mLocalPath = localPath;
    }

    public String getCreateAt() {
        return mCreateAt;
    }

    public void setCreateAt(String createAt) {
        mCreateAt = createAt;
    }

    public Boolean getSynced() {
        return mSynced;
    }

    public void setSynced(Boolean synced) {
        mSynced = synced;
    }

    public String getCloudPath() {
        return mCloudPath;
    }

    public void setCloudPath(String cloudPath) {
        mCloudPath = cloudPath;
    }

    @Override
    public String toString() {
        return "Video{" +
                "mId=" + mId +
                ", mTitle='" + mTitle + '\'' +
                ", mDuration=" + mDuration +
                ", mBitrate=" + mBitrate +
                ", mFps=" + mFps +
                ", mResolution='" + getResolution() + '\'' +
                ", mSize=" + mSize +
                ", mLocalPath='" + mLocalPath + '\'' +
                ", mCreateAt='" + mCreateAt + '\'' +
                ", mSynced=" + mSynced +
                ", mCloudPath='" + mCloudPath + '\'' +
                '}';
    }
}
