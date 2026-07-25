package com.shiji.core.data.repository

import android.content.Context
import android.net.Uri
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * .fitness file format — standard JSON, no password, human-readable.
 * Contains all user data except photos (photos are temporary).
 */
@Serializable
data class FitnessExportData(
    val version: Int = 1,
    val exportDate: String = "",
    val userProfile: UserProfileData? = null,
    val foodRecords: List<FoodRecordExportItem> = emptyList(),
    val healthMetrics: List<HealthMetricExportItem> = emptyList(),
    val cachedFoods: List<CachedFoodExportItem> = emptyList(),
    val aiProviders: List<AiProviderExportItem> = emptyList()
)

@Serializable data class UserProfileData(
    val heightCm: Double, val weightKg: Double, val goalType: String,
    val dailyCalories: Double, val proteinTarget: Double, val carbsTarget: Double, val fatTarget: Double
)
@Serializable data class FoodRecordExportItem(
    val mealType: String, val recordDate: String, val recordTime: String,
    val foodName: String, val portion: Double, val portionUnit: String,
    val calories: Double, val proteinGrams: Double, val carbsGrams: Double, val fatGrams: Double,
    val source: String, val note: String?
)
@Serializable data class HealthMetricExportItem(
    val metricType: String, val value: Double, val unit: String,
    val recordDate: String, val recordTime: String, val source: String
)
@Serializable data class CachedFoodExportItem(
    val name: String, val caloriesPer100g: Double,
    val proteinPer100g: Double, val carbsPer100g: Double, val fatPer100g: Double
)
@Serializable data class AiProviderExportItem(
    val id: String, val displayName: String, val baseUrl: String,
    val isEnabled: Boolean, val defaultVisionModel: String?, val defaultChatModel: String?
)

class DataExportManager {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    fun toJson(data: FitnessExportData): String = json.encodeToString(data)

    fun fromJson(jsonString: String): FitnessExportData =
        json.decodeFromString<FitnessExportData>(jsonString)

    fun readFromUri(context: Context, uri: Uri): FitnessExportData {
        val reader = BufferedReader(InputStreamReader(context.contentResolver.openInputStream(uri)!!))
        val content = reader.readText()
        reader.close()
        return fromJson(content)
    }

    fun writeToFile(context: Context, data: FitnessExportData): Uri? {
        val fileName = "shiji_backup_${data.exportDate.replace("-", "")}.fitness"
        val content = toJson(data)
        context.contentResolver.openOutputStream(
            Uri.parse("content://com.shiji.app.debug/external_files/$fileName")
        )?.use { it.write(content.toByteArray()) }
        return null // Simplified — real implementation uses SAF
    }
}
