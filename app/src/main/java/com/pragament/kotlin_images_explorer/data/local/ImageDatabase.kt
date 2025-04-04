package com.pragament.kotlin_images_explorer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pragament.kotlin_images_explorer.data.local.dao.ImageInfoDao
import com.pragament.kotlin_images_explorer.data.local.dao.VideoFrameDao
import com.pragament.kotlin_images_explorer.data.local.entity.ImageInfoEntity
import com.pragament.kotlin_images_explorer.data.local.entity.VideoFrameEntity

@Database(
    entities = [ImageInfoEntity::class, VideoFrameEntity::class],
    version = 1 // Increment the version
)
abstract class ImageDatabase : RoomDatabase() {
    abstract val imageDao: ImageInfoDao
    abstract val videoFrameDao: VideoFrameDao
}