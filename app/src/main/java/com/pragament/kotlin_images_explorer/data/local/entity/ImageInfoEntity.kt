package com.pragament.kotlin_images_explorer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "images")
data class ImageInfoEntity(
    @PrimaryKey val id: Long,
    val uri: String,
    val displayName: String,
    val dateAdded: Long,
    val extractedText: String?
) 