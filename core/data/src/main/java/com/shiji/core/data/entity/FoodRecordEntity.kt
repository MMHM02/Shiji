package com.shiji.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_records")
data class FoodRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mealType: String,              // BREAKFAST, LUNCH, DINNER, SNACK
    val recordDate: String,            // "2026-07-14"
    val recordTime: String,            // "08:30"
    val foodName: String,
    val portion: Double,
    val portionUnit: String,           // g, ml, 份, 碗, 个
    val calories: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val fiberGrams: Double = 0.0,
    val imageUri: String? = null,
    val source: String,                // CAMERA, VOICE, MANUAL, AI_CHAT
    val aiModel: String? = null,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
