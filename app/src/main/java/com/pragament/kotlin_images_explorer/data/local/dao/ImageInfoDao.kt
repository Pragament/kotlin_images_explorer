package com.pragament.kotlin_images_explorer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pragament.kotlin_images_explorer.data.local.entity.ImageInfoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageInfoDao {
    @Query("SELECT * FROM images ORDER BY dateAdded DESC")
    fun getAllImages(): Flow<List<ImageInfoEntity>>

    @Query("""
        SELECT * FROM images 
        WHERE extractedText LIKE '%' || :tag || '%' 
        OR label = :tag 
        OR extractedText LIKE '%,' || :tag || ',%'
        OR extractedText LIKE :tag || ',%'
        OR extractedText LIKE '%,' || :tag
    """)
    fun getImagesByTag(tag: String): Flow<List<ImageInfoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: ImageInfoEntity)

    @Update
    suspend fun updateImage(image: ImageInfoEntity)

    @Query("SELECT * FROM images WHERE id = :id")
    suspend fun getImageById(id: Long): ImageInfoEntity?

    @Query("UPDATE images SET extractedText = :text WHERE id = :imageId")
    suspend fun updateImageText(imageId: Long, text: String)

    @Query("""
        WITH RECURSIVE split(word, rest) AS (
            SELECT '', extractedText || ','
            FROM images
            WHERE extractedText IS NOT NULL AND extractedText != ''
            UNION ALL
            SELECT
                TRIM(substr(rest, 0, instr(rest, ','))),
                TRIM(substr(rest, instr(rest, ',')+1))
            FROM split
            WHERE rest <> ''
        )
        SELECT TRIM(word) as word, COUNT(*) as frequency
        FROM split
        WHERE word <> '' AND length(TRIM(word)) > 2
        GROUP BY TRIM(word)
        ORDER BY frequency DESC, word
        LIMIT 100
    """)
    fun getAllTags(): Flow<List<TagCount>>
}

data class TagCount(
    val word: String,
    val frequency: Int
)