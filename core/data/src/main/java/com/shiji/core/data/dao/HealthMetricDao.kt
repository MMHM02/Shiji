package com.shiji.core.data.dao

import androidx.room.*
import com.shiji.core.data.entity.HealthMetricEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthMetricDao {

    @Query("SELECT * FROM health_metrics WHERE metricType = :type ORDER BY recordDate DESC, recordTime DESC")
    fun getByType(type: String): Flow<List<HealthMetricEntity>>

    @Query("SELECT * FROM health_metrics WHERE metricType = :type AND recordDate = :date")
    suspend fun getByTypeAndDate(type: String, date: String): HealthMetricEntity?

    @Query("SELECT * FROM health_metrics WHERE recordDate BETWEEN :startDate AND :endDate ORDER BY recordDate ASC")
    fun getByDateRange(startDate: String, endDate: String): Flow<List<HealthMetricEntity>>

    @Query("SELECT * FROM health_metrics WHERE metricType = :type AND recordDate BETWEEN :startDate AND :endDate ORDER BY recordDate ASC")
    fun getByTypeAndDateRange(type: String, startDate: String, endDate: String): Flow<List<HealthMetricEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metric: HealthMetricEntity): Long

    @Update
    suspend fun update(metric: HealthMetricEntity)

    @Query("DELETE FROM health_metrics WHERE id = :id")
    suspend fun deleteById(id: Long)
}
