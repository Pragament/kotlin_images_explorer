package com.pragament.kotlin_images_explorer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pragament.kotlin_images_explorer.data.local.entity.VideoFrameEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoFrameDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFrame(frame: VideoFrameEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoFrameEntity)

    @Query("SELECT * FROM video_frames ORDER BY dateAdded DESC")
    fun getAllFrames(): Flow<List<VideoFrameEntity>>

    @Query("SELECT * FROM video_frames WHERE videoId = :videoId ORDER BY frameTimestamp ASC")
    fun getFramesForVideo(videoId: Long): Flow<List<VideoFrameEntity>>

    @Query("SELECT * FROM video_frames WHERE extractedText LIKE '%' || :searchText || '%'")
    fun searchFrames(searchText: String): Flow<List<VideoFrameEntity>>
}