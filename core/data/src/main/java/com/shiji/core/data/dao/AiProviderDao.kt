package com.shiji.core.data.dao

import androidx.room.*
import com.shiji.core.data.entity.AiProviderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiProviderDao {

    @Query("SELECT * FROM ai_providers ORDER BY displayName ASC")
    fun getAll(): Flow<List<AiProviderEntity>>

    @Query("SELECT * FROM ai_providers WHERE isEnabled = 1 ORDER BY displayName ASC")
    fun getEnabled(): Flow<List<AiProviderEntity>>

    @Query("SELECT * FROM ai_providers WHERE id = :id")
    suspend fun getById(id: String): AiProviderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(provider: AiProviderEntity)

    @Query("UPDATE ai_providers SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)

    @Query("DELETE FROM ai_providers WHERE id = :id")
    suspend fun deleteById(id: String)
}
