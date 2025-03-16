package com.pragament.kotlin_images_explorer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pragament.kotlin_images_explorer.data.local.entity.VideoFrameEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoFrameDao {
    @Query("SELECT * FROM video_frames ORDER BY timestamp DESC")
    fun getAllFrames(): Flow<List<VideoFrameEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFrame(frame: VideoFrameEntity)

    @Query("UPDATE video_frames SET extractedText = :text WHERE id = :frameId")
    suspend fun updateFrameText(frameId: Long, text: String)
}