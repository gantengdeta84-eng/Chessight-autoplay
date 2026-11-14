package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedPositionDao {
    @Query("SELECT * FROM saved_positions ORDER BY createdAt DESC LIMIT 40")
    fun observeRecentPositions(): Flow<List<SavedPositionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosition(entity: SavedPositionEntity)

    @Query("DELETE FROM saved_positions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM saved_positions")
    suspend fun clearAll()
}
