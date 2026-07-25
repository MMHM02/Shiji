package com.shiji.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_food_items")
data class CachedFoodItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,                  // 食物名称
    val caloriesPer100g: Double,       // 每100g热量
    val proteinPer100g: Double = 0.0,
    val carbsPer100g: Double = 0.0,
    val fatPer100g: Double = 0.0,
    val defaultPortion: Double = 100.0, // 默认份量(g)
    val defaultUnit: String = "g",
    val useCount: Int = 0,             // 使用次数（排序用）
    val createdAt: Long = System.currentTimeMillis()
)
