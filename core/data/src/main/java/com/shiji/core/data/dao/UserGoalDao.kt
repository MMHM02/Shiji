package com.shiji.core.data.dao

import androidx.room.*
import com.shiji.core.data.entity.UserGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserGoalDao {

    @Query("SELECT * FROM user_goals WHERE id = 1")
    fun getGoal(): Flow<UserGoalEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: UserGoalEntity)

    @Query("DELETE FROM user_goals WHERE id = 1")
    suspend fun delete()
}
