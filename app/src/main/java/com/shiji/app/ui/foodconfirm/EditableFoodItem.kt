package com.shiji.app.ui.foodconfirm

import com.shiji.core.ai.api.FoodAnalysisResult
import java.util.UUID

/**
 * An AI-recognized food item in editable form.
 * Strings for numeric fields keep text editing natural; validated at save time.
 * Shared by camera / voice / text record flows.
 */
data class EditableFoodItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val portion: String,
    val portionUnit: String,
    val calories: String,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val confidence: Float = 1f
) {
    fun portionValue(): Double = portion.toDoubleOrNull() ?: 1.0
    fun caloriesValue(): Double = calories.toDoubleOrNull() ?: 0.0
    fun displayName(): String = name.trim().ifBlank { "未命名食物" }

    companion object {
        fun from(item: FoodAnalysisResult.FoodItem): EditableFoodItem = EditableFoodItem(
            name = item.name,
            portion = if (item.portion % 1.0 == 0.0) item.portion.toInt().toString() else item.portion.toString(),
            portionUnit = item.portionUnit,
            calories = item.calories.toInt().toString(),
            proteinGrams = item.proteinGrams,
            carbsGrams = item.carbsGrams,
            fatGrams = item.fatGrams,
            confidence = item.confidence
        )
    }
}

/** Meal suggestion from the current hour. */
fun guessMealType(): String = when (java.time.LocalTime.now().hour) {
    in 5..9 -> "BREAKFAST"
    in 10..14 -> "LUNCH"
    in 15..16 -> "SNACK"
    in 17..21 -> "DINNER"
    else -> "SNACK"
}

val MEAL_TYPE_OPTIONS = listOf(
    "BREAKFAST" to "🥣 早",
    "LUNCH" to "🍱 午",
    "DINNER" to "🍽️ 晚",
    "SNACK" to "🍎 加"
)
