package com.chienpm.zecorder.data.entities;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.chienpm.zecorder.controllers.settings.VideoSetting;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;

@Entity(tableName = "videos")
public class Video implements Cloneable {
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
    private long mCreateAt;

    @ColumnInfo(name = "synced")
    private Boolean mSynced;

    @ColumnInfo(name = "cloud_path")
    private String mCloudPath;

    public Video(){}

    public Video(String title, long duration, int bitrate, int fps, int width, int height, long size, String localPath, long createAt, Boolean isSynced, String cloudPath) {
        mTitle = title;
        mDuration = duration;
        mBitrate = bitrate;
        mFps = fps;
        mWidth = width;
        mHeight = height;
        mSize = size;
        mLocalPath = localPath;
        mCreateAt = getCurrentTime();
        mSynced = isSynced;
        mCloudPath = cloudPath;
    }

    public Video(Video video) {
        mId = video.mId;
        mTitle = video.mTitle;
        mDuration = video.mDuration;
        mBitrate = video.mBitrate;
        mFps = video.mBitrate;
        mWidth = video.mWidth;
        mHeight = video.mHeight;
        mSize = video.mSize;
        mLocalPath = video.mLocalPath;
        mCreateAt = video.mCreateAt;
        mSynced = video.mSynced;
        mCloudPath = video.mCloudPath;

    }

    private long getCurrentTime() {
        Calendar cal = Calendar.getInstance();
        return cal.getTimeInMillis();
    }

    public Video(VideoSetting setting) {
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

    public long getCreateAt() {
        return mCreateAt;
    }

    public String getFormattedDate(String pattern){
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        return sdf.format(new Date(mCreateAt));
    }

    public void setCreateAt(long createAt) {
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

    public String getNameOnly() {
        return mTitle.substring(0, mTitle.indexOf(".mp4"));
    }

    public void updateTitle(String newTitle, String newPath) {
        mTitle = newTitle;
        mLocalPath = newPath;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
