package com.shiji.core.common.util

import java.time.LocalTime

/** Portion unit conversion utility. Reference values only — AI/model provides real estimates. */
object PortionConverter {

    enum class PortionUnit(val display: String) {
        GRAMS("g"), MILLILITERS("ml"), SERVING("份"), BOWL("碗"), PIECE("个"), CUP("杯");

        companion object {
            fun fromString(s: String): PortionUnit = entries.find {
                it.name.equals(s, ignoreCase = true) || it.display == s
            } ?: GRAMS
        }
    }

    /** Reference kcal per typical portion (per 100g or per unit) — approximate only */
    private val referenceDensity = mapOf(
        "米饭" to 1.16, "面条" to 1.38, "馒头" to 2.21,
        "水" to 1.0, "牛奶" to 1.03, "豆浆" to 1.02,
        "鸡蛋" to 0.55, "苹果" to 0.85, "香蕉" to 0.95
    )

    /** Estimate grams from a non-weight unit. Very rough — user should verify. */
    fun estimateGrams(foodName: String, amount: Double, unit: PortionUnit): Double = when (unit) {
        PortionUnit.GRAMS -> amount
        PortionUnit.MILLILITERS -> amount * (referenceDensity[foodName] ?: 1.0)
        PortionUnit.SERVING -> amount * 200.0  // 1 份 ≈ 200g
        PortionUnit.BOWL -> amount * 250.0     // 1 碗 ≈ 250g
        PortionUnit.PIECE -> amount * 100.0    // 1 个 ≈ 100g
        PortionUnit.CUP -> amount * 240.0      // 1 杯 ≈ 240ml ≈ 240g
    }

    /** Auto-infer meal type from current time */
    fun inferMealType(): String {
        val h = LocalTime.now().hour
        val m = LocalTime.now().minute
        val minutes = h * 60 + m
        return when {
            minutes in 300..630 -> "BREAKFAST"   // 05:00-10:30
            minutes in 631..840 -> "LUNCH"       // 10:31-14:00
            minutes in 841..1020 -> "SNACK"      // 14:01-17:00
            else -> "DINNER"
        }
    }

    fun mealTypeDisplay(type: String): String = when (type) {
        "BREAKFAST" -> "🥣 早餐"
        "LUNCH" -> "🍱 午餐"
        "DINNER" -> "🍽️ 晚餐"
        "SNACK" -> "🍎 加餐"
        else -> "🍽️ 餐食"
    }

    fun mealTypeEmoji(type: String): String = when (type) {
        "BREAKFAST" -> "🥣"
        "LUNCH" -> "🍱"
        "DINNER" -> "🍽️"
        "SNACK" -> "🍎"
        else -> "🍽️"
    }
}
