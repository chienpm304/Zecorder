package com.chienpm.zecorder.data.entities;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

@Entity(tableName = "videos")
public class Video {
    @NonNull
    @PrimaryKey( autoGenerate = true)
    @ColumnInfo(name = "id")
    private int mId;

    @ColumnInfo(name = "tittle")
    private String mTitle;

    @ColumnInfo(name = "duration")
    private int mDuration;

    @ColumnInfo(name = "bitrate")
    private int mBitrate;

    @ColumnInfo(name = "fps")
    private int mFps;

    @ColumnInfo(name = "resolution")
    private String mResolution;

    @ColumnInfo(name = "weight")
    private int mWeight;

    @ColumnInfo(name = "local_path")
    private String mLocalPath;

    @ColumnInfo(name = "createdAt")
    private String mCreateAt;

    @ColumnInfo(name = "synced")
    private Boolean mSynced;

    @ColumnInfo(name = "cloud_path")
    private String mCloudPath;

    public Video(){}

    public Video(String title, int duration, int bitrate, int fps, String resolution, int weight, String localPath, String createAt, Boolean isSynced, String cloudPath) {
        mTitle = title;
        mDuration = duration;
        mBitrate = bitrate;
        mFps = fps;
        mResolution = resolution;
        mWeight = weight;
        mLocalPath = localPath;
        mCreateAt = createAt;
        mSynced = isSynced;
        mCloudPath = cloudPath;
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

    public int getDuration() {
        return mDuration;
    }

    public void setDuration(int duration) {
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
        return mResolution;
    }

    public void setResolution(String resolution) {
        mResolution = resolution;
    }

    public int getWeight() {
        return mWeight;
    }

    public void setWeight(int weight) {
        mWeight = weight;
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
                ", mResolution='" + mResolution + '\'' +
                ", mWeight=" + mWeight +
                ", mLocalPath='" + mLocalPath + '\'' +
                ", mCreateAt='" + mCreateAt + '\'' +
                ", mSynced=" + mSynced +
                ", mCloudPath='" + mCloudPath + '\'' +
                '}';
    }
}
