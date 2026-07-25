package com.shiji.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.shiji.core.data.dao.*
import com.shiji.core.data.entity.*

@Database(
    entities = [
        FoodRecordEntity::class,
        HealthMetricEntity::class,
        UserGoalEntity::class,
        AiProviderEntity::class,
        CachedFoodItemEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class ShiJiDatabase : RoomDatabase() {
    abstract fun foodRecordDao(): FoodRecordDao
    abstract fun healthMetricDao(): HealthMetricDao
    abstract fun userGoalDao(): UserGoalDao
    abstract fun aiProviderDao(): AiProviderDao
    abstract fun cachedFoodDao(): CachedFoodDao
}
