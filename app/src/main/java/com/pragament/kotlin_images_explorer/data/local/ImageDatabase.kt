package com.pragament.kotlin_images_explorer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pragament.kotlin_images_explorer.data.local.dao.ImageInfoDao
import com.pragament.kotlin_images_explorer.data.local.entity.ImageInfoEntity

@Database(
    entities = [ImageInfoEntity::class ], version = 1
)
abstract class ImageDatabase : RoomDatabase() {
    abstract val imageDao: ImageInfoDao
} 