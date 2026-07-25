package com.shiji.core.data.repository

import com.shiji.core.data.dao.CachedFoodDao
import com.shiji.core.data.dao.FoodRecordDao
import com.shiji.core.data.entity.FoodRecordEntity
import kotlinx.coroutines.flow.Flow

class FoodRepositoryImpl(
    private val foodRecordDao: FoodRecordDao,
    private val cachedFoodDao: CachedFoodDao
) {
    fun getRecordsByDate(date: String): Flow<List<FoodRecordEntity>> =
        foodRecordDao.getRecordsByDate(date)

    fun getRecordsByDateRange(startDate: String, endDate: String): Flow<List<FoodRecordEntity>> =
        foodRecordDao.getRecordsByDateRange(startDate, endDate)

    fun getDailyCalories(date: String): Flow<Double?> =
        foodRecordDao.getDailyCalories(date)

    suspend fun saveRecord(record: FoodRecordEntity): Long =
        foodRecordDao.insert(record)

    suspend fun updateRecord(record: FoodRecordEntity) =
        foodRecordDao.update(record)

    suspend fun deleteRecord(id: Long) =
        foodRecordDao.deleteById(id)

    suspend fun getRecordById(id: Long): FoodRecordEntity? =
        foodRecordDao.getById(id)

    // Cached food items
    fun getAllCachedFoods(): Flow<List<com.shiji.core.data.entity.CachedFoodItemEntity>> =
        cachedFoodDao.getAll()

    fun searchCachedFoods(query: String): Flow<List<com.shiji.core.data.entity.CachedFoodItemEntity>> =
        cachedFoodDao.search(query)

    suspend fun addToCache(item: com.shiji.core.data.entity.CachedFoodItemEntity): Long =
        cachedFoodDao.upsert(item)

    suspend fun incrementFoodUseCount(id: Long) =
        cachedFoodDao.incrementUseCount(id)

    suspend fun deleteCachedFood(id: Long) =
        cachedFoodDao.deleteById(id)

    // Photo cleanup (3-day auto cleanup)
    suspend fun getRecordsWithOldPhotos(beforeTimestamp: Long): List<FoodRecordEntity> =
        foodRecordDao.getRecordsWithOldPhotos(beforeTimestamp)

    suspend fun cleanOldPhotos(beforeTimestamp: Long): Int =
        foodRecordDao.deleteRecordsWithOldPhotos(beforeTimestamp)
}
