package com.shiji.app.domain.ai

import com.shiji.core.ai.api.AdvisorContext
import com.shiji.core.data.dao.HealthMetricDao
import com.shiji.core.data.dao.UserGoalDao
import com.shiji.core.data.entity.UserGoalEntity
import com.shiji.core.data.repository.FoodRepositoryImpl
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/**
 * Builds the advisor's system-prompt context from the user's real local data.
 * Read fresh for every message so the AI always talks about current numbers.
 */
class AdvisorContextBuilder @Inject constructor(
    private val foodRepository: FoodRepositoryImpl,
    private val userGoalDao: UserGoalDao,
    private val healthMetricDao: HealthMetricDao
) {

    suspend fun build(userName: String): AdvisorContext {
        val today = LocalDate.now()
        val todayStr = today.toString()
        val goal = userGoalDao.getGoal().first() ?: UserGoalEntity()

        val todayRecords = foodRepository.getRecordsByDate(todayStr).first()
        val weekRecords = foodRepository.getRecordsByDateRange(
            today.minusDays(6).toString(), todayStr
        ).first()

        val todayCal = todayRecords.sumOf { it.calories }
        val todayProtein = todayRecords.sumOf { it.proteinGrams }
        val todayCarbs = todayRecords.sumOf { it.carbsGrams }
        val todayFat = todayRecords.sumOf { it.fatGrams }

        val weekByDay = weekRecords.groupBy { it.recordDate }
            .mapValues { (_, rs) -> rs.sumOf { it.calories } }
        val weekAvg = if (weekByDay.isEmpty()) null else weekByDay.values.average()

        val latestWeight = healthMetricDao.getByType("WEIGHT").first().firstOrNull()

        val mealsText = if (todayRecords.isEmpty()) "暂无记录"
        else todayRecords.groupBy { it.mealType }.entries.joinToString("；") { (meal, rs) ->
            "${mealTypeLabel(meal)}: ${rs.joinToString("、") { it.foodName }}"
        }

        return AdvisorContext(
            userName = userName,
            todayCaloriesText = "${todayCal.toInt()} kcal",
            calorieGoalText = "${goal.dailyCalories.toInt()} kcal",
            todayProteinText = "${todayProtein.toInt()} g",
            proteinGoalText = "${goal.proteinTargetGrams.toInt()} g",
            todayCarbsText = "${todayCarbs.toInt()} g",
            carbsGoalText = "${goal.carbsTargetGrams.toInt()} g",
            todayFatText = "${todayFat.toInt()} g",
            fatGoalText = "${goal.fatTargetGrams.toInt()} g",
            weightText = latestWeight?.let { "${it.value} kg" } ?: "暂无记录",
            goalTypeText = goalTypeLabel(goal.goalType),
            weekAvgCaloriesText = weekAvg?.let { "${it.toInt()} kcal" } ?: "暂无数据",
            todayMealsText = mealsText
        )
    }

    /** True when the user has logged anything today — decides rich vs fallback prompt. */
    suspend fun hasAnyData(): Boolean {
        val today = LocalDate.now().toString()
        return foodRepository.getRecordsByDate(today).first().isNotEmpty()
    }

    private fun mealTypeLabel(mealType: String) = when (mealType) {
        "BREAKFAST" -> "早餐"
        "LUNCH" -> "午餐"
        "DINNER" -> "晚餐"
        "SNACK" -> "加餐"
        else -> mealType
    }

    private fun goalTypeLabel(goalType: String) = when (goalType) {
        "LOSE_FAST" -> "快速减脂"
        "LOSE_SLOW" -> "温和减脂"
        "GAIN_SLOW" -> "缓慢增肌"
        "GAIN_FAST" -> "快速增肌"
        else -> "保持健康"
    }
}
