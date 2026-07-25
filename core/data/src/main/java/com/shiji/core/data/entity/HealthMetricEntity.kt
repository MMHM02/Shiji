package com.shiji.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "health_metrics")
data class HealthMetricEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val metricType: String,            // WEIGHT, EXERCISE_MINUTES, WATER_ML, BODY_FAT_PCT, WAIST_CM
    val value: Double,
    val unit: String,
    val recordDate: String,
    val recordTime: String,
    val source: String = "MANUAL",     // MANUAL, HEALTH_CONNECT
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
