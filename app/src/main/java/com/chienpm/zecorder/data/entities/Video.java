package com.chienpm.zecorder.data.entities;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

import com.chienpm.zecorder.controllers.settings.VideoSetting;
import com.chienpm.zecorder.ui.utils.GoogleDriveFileHolder;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Entity(tableName = "videos")
public class Video implements Parcelable {
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

    @ColumnInfo(name = "cloud_path")
    private String mCloudPath;

    @Ignore
    private String mThumbnailLink;

    public Video(){}

    public Video(String title, long duration, int bitrate, int fps, int width, int height, long size, String localPath, long createAt, String cloudPath, String thumbnailLink) {
        mTitle = title;
        mDuration = duration;
        mBitrate = bitrate;
        mFps = fps;
        mWidth = width;
        mHeight = height;
        mSize = size;
        mLocalPath = localPath;
        mCreateAt = getCurrentTime();
        mCloudPath = cloudPath;
        mThumbnailLink = thumbnailLink;
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
        mCloudPath = video.mCloudPath;
        mThumbnailLink = video.mThumbnailLink;
    }

    protected Video(Parcel in) {
        mId = in.readInt();
        mTitle = in.readString();
        mDuration = in.readLong();
        mBitrate = in.readInt();
        mFps = in.readInt();
        mWidth = in.readInt();
        mHeight = in.readInt();
        mSize = in.readLong();
        mLocalPath = in.readString();
        mCreateAt = in.readLong();
        mCloudPath = in.readString();
        mThumbnailLink = in.readString();
    }

    public static final Creator<Video> CREATOR = new Creator<Video>() {
        @Override
        public Video createFromParcel(Parcel in) {
            return new Video(in);
        }

        @Override
        public Video[] newArray(int size) {
            return new Video[size];
        }
    };

    public static ArrayList<Video> createTempVideoFromGoogleDriveData(List<GoogleDriveFileHolder> files) {
        ArrayList<Video> videos = new ArrayList<>();
        for(GoogleDriveFileHolder file: files){
            videos.add(new Video(file.getName(), 0, 0, 0, 0,0, file.getSize(), "", file.getModifiedTime().getValue(), file.getId(), file.getThumbnailLink()));
        }
        return videos;
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

    public boolean isLocalVideo() {
        return !TextUtils.isEmpty(mLocalPath);
    }

    public String getThumbnailLink() {
        if(isLocalVideo())
            return mLocalPath;
        else
            return mThumbnailLink;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mId);
        dest.writeString(mTitle);
        dest.writeLong(mDuration);
        dest.writeInt(mBitrate);
        dest.writeInt(mFps);
        dest.writeInt(mWidth);
        dest.writeInt(mHeight);
        dest.writeLong(mSize);
        dest.writeString(mLocalPath);
        dest.writeLong(mCreateAt);
        dest.writeString(mCloudPath);
        dest.writeString(mThumbnailLink);
    }
}
