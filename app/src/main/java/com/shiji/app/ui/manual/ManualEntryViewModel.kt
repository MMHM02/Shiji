package com.shiji.app.ui.manual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiji.core.ai.manager.AiServiceManager
import com.shiji.core.common.result.Result
import com.shiji.core.data.entity.CachedFoodItemEntity
import com.shiji.core.data.repository.FoodRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManualEntryViewModel @Inject constructor(
    private val aiServiceManager: AiServiceManager,
    private val foodRepository: FoodRepositoryImpl
) : ViewModel() {

    data class NutritionEstimate(
        val calories: Double, val protein: Double, val carbs: Double, val fat: Double)

    fun estimate(foodName: String, portion: String, unit: String,
                 onResult: (NutritionEstimate?) -> Unit) {
        viewModelScope.launch {
            val query = "$portion$unit $foodName"
            when (val result = aiServiceManager.parseFoodDescription(query)) {
                is Result.Success -> {
                    val item = result.data.firstOrNull()
                    onResult(item?.let { NutritionEstimate(it.calories, it.proteinGrams, it.carbsGrams, it.fatGrams) })
                }
                is Result.Error -> onResult(null)
            }
        }
    }

    fun addToLibrary(name: String, portion: Double, unit: String,
                     calories: Double, protein: Double, carbs: Double, fat: Double) {
        viewModelScope.launch {
            val factor = 100.0 / portion.coerceAtLeast(1.0)
            foodRepository.addToCache(CachedFoodItemEntity(
                name = name, caloriesPer100g = calories * factor,
                proteinPer100g = protein * factor, carbsPer100g = carbs * factor,
                fatPer100g = fat * factor, defaultPortion = portion, defaultUnit = unit))
        }
    }
}
