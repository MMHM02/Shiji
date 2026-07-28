package com.shiji.core.data.repository

import android.content.Context
import android.net.Uri
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import android.os.Environment
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
    fun fromJson(jsonString: String): FitnessExportData = json.decodeFromString(jsonString)

    fun readFromUri(context: Context, uri: Uri): FitnessExportData {
        val reader = BufferedReader(InputStreamReader(context.contentResolver.openInputStream(uri)!!))
        val content = reader.readText()
        reader.close()
        return fromJson(content)
    }

    /** Backup folder — inside Android/data, accessible via file manager. */
    fun backupDir(context: Context): File {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        return File(dir, "ShiJi").also { it.mkdirs() }
    }

    /**
     * Write export to the backup folder. Deletes any existing .fitness file first
     * (only one backup is kept at a time).
     * @return the written file, or null on failure.
     */
    fun exportToFile(context: Context, data: FitnessExportData): File? {
        val dir = backupDir(context)
        // Remove old backups
        dir.listFiles { f -> f.extension == "fitness" }?.forEach { it.delete() }
        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val file = File(dir, "shiji_backup_$stamp.fitness")
        return try {
            file.writeText(toJson(data))
            file
        } catch (_: Exception) { null }
    }

    /** List .fitness files in the backup folder (typically 0 or 1). */
    fun listBackupFiles(context: Context): List<File> =
        backupDir(context).listFiles { f -> f.extension == "fitness" }?.toList() ?: emptyList()

    /** Read a .fitness file from the backup folder. */
    fun readBackupFile(file: File): FitnessExportData =
        fromJson(file.readText())
}
