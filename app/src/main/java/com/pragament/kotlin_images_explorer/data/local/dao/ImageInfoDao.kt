package com.pragament.kotlin_images_explorer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pragament.kotlin_images_explorer.data.local.entity.ImageInfoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageInfoDao {
    @Query("SELECT * FROM images ORDER BY dateAdded DESC")
    fun getAllImages(): Flow<List<ImageInfoEntity>>

    @Query("SELECT * FROM images WHERE extractedText LIKE '%' || :tag || '%'")
    fun getImagesByTag(tag: String): Flow<List<ImageInfoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: ImageInfoEntity)

    @Query("UPDATE images SET extractedText = :text WHERE id = :imageId")
    suspend fun updateImageText(imageId: Long, text: String)

    @Query("""
        WITH RECURSIVE split(word, rest) AS (
            SELECT '', extractedText || ' '
            FROM images
            WHERE extractedText IS NOT NULL
            UNION ALL
            SELECT
                substr(rest, 0, instr(rest, ' ')),
                substr(rest, instr(rest, ' ')+1)
            FROM split
            WHERE rest <> ''
        )
        SELECT word, COUNT(*) as frequency
        FROM split
        WHERE length(word) > 2
        GROUP BY word
        ORDER BY frequency DESC, word
        LIMIT 100
    """)
    fun getAllTags(): Flow<List<TagCount>>
}

data class TagCount(
    val word: String,
    val frequency: Int
) 