package com.shiji.core.data.dao

import androidx.room.*
import com.shiji.core.data.entity.FoodRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodRecordDao {

    @Query("SELECT * FROM food_records WHERE recordDate = :date ORDER BY recordTime ASC")
    fun getRecordsByDate(date: String): Flow<List<FoodRecordEntity>>

    @Query("SELECT * FROM food_records WHERE recordDate BETWEEN :startDate AND :endDate ORDER BY recordDate ASC, recordTime ASC")
    fun getRecordsByDateRange(startDate: String, endDate: String): Flow<List<FoodRecordEntity>>

    @Query("SELECT * FROM food_records WHERE recordDate = :date AND mealType = :mealType ORDER BY recordTime ASC")
    fun getRecordsByDateAndMeal(date: String, mealType: String): Flow<List<FoodRecordEntity>>

    @Query("SELECT SUM(calories) FROM food_records WHERE recordDate = :date")
    fun getDailyCalories(date: String): Flow<Double?>

    @Query("SELECT * FROM food_records WHERE id = :id")
    suspend fun getById(id: Long): FoodRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: FoodRecordEntity): Long

    @Update
    suspend fun update(record: FoodRecordEntity)

    @Query("DELETE FROM food_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM food_records WHERE imageUri IS NOT NULL AND imageUri != '' AND createdAt < :beforeTimestamp")
    suspend fun getRecordsWithOldPhotos(beforeTimestamp: Long): List<FoodRecordEntity>

    /** ADR-23: photos are temporary — clear the reference (and file), keep the record itself. */
    @Query("UPDATE food_records SET imageUri = NULL WHERE imageUri IS NOT NULL AND imageUri != '' AND createdAt < :beforeTimestamp")
    suspend fun deleteRecordsWithOldPhotos(beforeTimestamp: Long): Int

    @Query("SELECT DISTINCT recordDate FROM food_records WHERE recordDate BETWEEN :start AND :end ORDER BY recordDate ASC")
    fun getDistinctDatesWithRecords(start: String, end: String): Flow<List<String>>
}
