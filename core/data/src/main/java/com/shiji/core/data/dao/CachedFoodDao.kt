package com.shiji.core.data.dao

import androidx.room.*
import com.shiji.core.data.entity.CachedFoodItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedFoodDao {

    @Query("SELECT * FROM cached_food_items ORDER BY useCount DESC, name ASC")
    fun getAll(): Flow<List<CachedFoodItemEntity>>

    @Query("SELECT * FROM cached_food_items WHERE name LIKE '%' || :query || '%' ORDER BY useCount DESC")
    fun search(query: String): Flow<List<CachedFoodItemEntity>>

    @Query("SELECT * FROM cached_food_items WHERE id = :id")
    suspend fun getById(id: Long): CachedFoodItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: CachedFoodItemEntity): Long

    @Query("UPDATE cached_food_items SET useCount = useCount + 1 WHERE id = :id")
    suspend fun incrementUseCount(id: Long)

    @Query("DELETE FROM cached_food_items WHERE id = :id")
    suspend fun deleteById(id: Long)
}
