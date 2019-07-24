package com.chienpm.zecorder.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

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

    @Delete
    public void deleteVideos(Video... videos);

}
