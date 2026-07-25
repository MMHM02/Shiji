package com.shiji.core.data

import com.shiji.core.data.repository.*
import org.junit.Assert.*
import org.junit.Test

class DataExportTest {

    private val manager = DataExportManager()

    @Test
    fun `serialize and deserialize fitness data`() {
        val original = FitnessExportData(
            version = 1,
            exportDate = "2026-07-14",
            userProfile = UserProfileData(
                heightCm = 175.0, weightKg = 72.0, goalType = "LOSE_SLOW",
                dailyCalories = 1800.0, proteinTarget = 120.0, carbsTarget = 200.0, fatTarget = 60.0
            ),
            foodRecords = listOf(
                FoodRecordExportItem(
                    mealType = "BREAKFAST", recordDate = "2026-07-14", recordTime = "08:30",
                    foodName = "牛奶面包", portion = 200.0, portionUnit = "g",
                    calories = 350.0, proteinGrams = 12.0, carbsGrams = 45.0, fatGrams = 8.0,
                    source = "MANUAL", note = null
                )
            ),
            healthMetrics = listOf(
                HealthMetricExportItem("WEIGHT", 72.0, "kg", "2026-07-14", "08:00", "MANUAL")
            )
        )

        val json = manager.toJson(original)
        assertTrue(json.contains("LOSE_SLOW"))
        assertTrue(json.contains("牛奶面包"))

        val restored = manager.fromJson(json)
        assertEquals(1, restored.version)
        assertEquals("2026-07-14", restored.exportDate)
        assertEquals(175.0, restored.userProfile?.heightCm ?: 0.0, 0.01)
        assertEquals(1, restored.foodRecords.size)
        assertEquals("牛奶面包", restored.foodRecords[0].foodName)
        assertEquals(1, restored.healthMetrics.size)
        assertEquals(72.0, restored.healthMetrics[0].value, 0.01)
    }

    @Test
    fun `empty export data serializes correctly`() {
        val data = FitnessExportData(exportDate = "2026-01-01")
        val json = manager.toJson(data)
        val restored = manager.fromJson(json)
        assertEquals(1, restored.version)
        assertNull(restored.userProfile)
        assertTrue(restored.foodRecords.isEmpty())
    }
}
