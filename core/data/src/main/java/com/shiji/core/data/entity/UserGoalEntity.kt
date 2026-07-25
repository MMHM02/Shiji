package com.shiji.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_goals")
data class UserGoalEntity(
    @PrimaryKey
    val id: Long = 1,                  // Singleton record
    val heightCm: Double? = null,
    val currentWeightKg: Double? = null,
    val targetWeightKg: Double? = null,
    val dailyCalories: Double = 2000.0,
    val proteinTargetGrams: Double = 60.0,
    val carbsTargetGrams: Double = 250.0,
    val fatTargetGrams: Double = 65.0,
    val goalType: String = "MAINTAIN", // LOSE_FAST, LOSE_SLOW, MAINTAIN, GAIN_SLOW, GAIN_FAST
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * Derive daily calorie + macro targets from body data and goal type.
         * Calorie factors (kcal per kg of body weight) match the profile screen:
         * lose-fast ×22, lose-slow ×25, maintain ×30, gain-slow ×33, gain-fast ×36.
         */
        fun calculate(heightCm: Double, weightKg: Double, goalType: String): UserGoalEntity {
            val factor = when (goalType) {
                "LOSE_FAST" -> 22.0
                "LOSE_SLOW" -> 25.0
                "GAIN_SLOW" -> 33.0
                "GAIN_FAST" -> 36.0
                else -> 30.0 // MAINTAIN
            }
            val calories = weightKg * factor

            // Higher protein when changing weight, moderate for maintenance.
            val protein = weightKg * if (goalType == "MAINTAIN") 1.2 else 1.8
            val fat = (calories * 0.25) / 9.0
            val carbs = ((calories - protein * 4.0 - fat * 9.0) / 4.0).coerceAtLeast(0.0)

            return UserGoalEntity(
                heightCm = heightCm,
                currentWeightKg = weightKg,
                dailyCalories = calories,
                proteinTargetGrams = protein,
                carbsTargetGrams = carbs,
                fatTargetGrams = fat,
                goalType = goalType
            )
        }
    }
}
