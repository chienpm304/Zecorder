package com.chienpm.zecorder.data.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import com.chienpm.zecorder.data.entities.Video;

import java.util.List;

@Dao
public interface VideoDAO {

    @Insert
    void insertVideo(Video video);

    @Query("Select * from videos where id = :videoId")
    Video getVideoById(int videoId);

    @Query("Select * from videos where tittle = :tittle Limit 1")
    Video getVideoByTitle(String tittle);

    @Query("select * from videos")
    List<Video> getAllVideo();

    @Update
    void updateVideo(Video video);

    @Delete
    void deleteVideo(Video video);
}
