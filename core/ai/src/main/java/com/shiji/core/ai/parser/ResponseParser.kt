package com.shiji.core.ai.parser

import com.shiji.core.ai.api.FoodAnalysisResult
import kotlinx.serialization.json.*

/**
 * Parses AI food-analysis responses.
 * Tolerant of markdown fences, prose around the JSON, and both
 * camelCase / snake_case field names (different models emit either).
 */
object ResponseParser {

    fun parseFoodAnalysis(rawJson: String): Result<FoodAnalysisResult> = runCatching {
        val cleaned = extractJson(rawJson)
        val json = Json.parseToJsonElement(cleaned).jsonObject

        val items = json["items"]?.jsonArray?.mapNotNull { element ->
            val obj = element.jsonObject
            val name = obj.str("name", "food_name", "foodName") ?: return@mapNotNull null
            FoodAnalysisResult.FoodItem(
                name = name,
                portion = obj.num("portion", "amount", "quantity") ?: 1.0,
                portionUnit = obj.str("portionUnit", "portion_unit", "unit") ?: "份",
                calories = obj.num("calories", "kcal", "calorie") ?: 0.0,
                proteinGrams = obj.num("proteinGrams", "protein_grams", "protein") ?: 0.0,
                carbsGrams = obj.num("carbsGrams", "carbs_grams", "carbs", "carbohydrates") ?: 0.0,
                fatGrams = obj.num("fatGrams", "fat_grams", "fat") ?: 0.0,
                confidence = obj.num("confidence", "score")?.toFloat() ?: 0.5f
            )
        } ?: emptyList()

        FoodAnalysisResult(
            items = items,
            totalCalories = json.num("totalCalories", "total_calories", "total_kcal")
                ?: items.sumOf { it.calories },
            confidence = (json.num("confidence", "general_confidence", "overall_confidence")
                ?: 0.5).toFloat().coerceIn(0f, 1f),
            rawResponse = rawJson
        )
    }

    /** Extract JSON from potentially messy AI output (may contain markdown fences / prose). */
    fun extractJson(raw: String): String {
        var text = raw.trim()
        if (text.contains("```")) {
            text = text.substringAfter("```json").substringAfter("```").substringBeforeLast("```")
            if (text.isBlank()) text = raw.substringAfter("```").substringBeforeLast("```")
        }
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        return if (start >= 0 && end > start) text.substring(start, end + 1) else text
    }

    /** Quick check if AI thinks there's no food in the image. */
    fun hasNoFood(result: FoodAnalysisResult): Boolean =
        result.items.isEmpty() || result.confidence < 0.1f

    // ---- tolerant field readers ----

    private fun JsonObject.str(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key -> this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } }

    private fun JsonObject.num(vararg keys: String): Double? =
        keys.firstNotNullOfOrNull { key ->
            this[key]?.jsonPrimitive?.let { p ->
                p.doubleOrNull ?: p.contentOrNull?.toDoubleOrNull()
            }
        }
}
