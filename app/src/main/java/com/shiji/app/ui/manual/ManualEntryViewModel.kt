package com.shiji.app.ui.manual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiji.core.ai.manager.AiServiceManager
import com.shiji.core.common.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the "AI 计算" button on the manual entry screen:
 * asks the configured chat model to estimate nutrition for one food item.
 */
@HiltViewModel
class ManualEntryViewModel @Inject constructor(
    private val aiServiceManager: AiServiceManager
) : ViewModel() {

    data class NutritionEstimate(
        val calories: Double,
        val protein: Double,
        val carbs: Double,
        val fat: Double
    )

    /**
     * @param onResult estimate on success, null when AI is unavailable/failed
     *                 (the caller then falls back to the offline heuristic).
     */
    fun estimate(
        foodName: String,
        portion: String,
        unit: String,
        onResult: (NutritionEstimate?) -> Unit
    ) {
        viewModelScope.launch {
            val query = "$portion$unit $foodName"
            when (val result = aiServiceManager.parseFoodDescription(query)) {
                is Result.Success -> {
                    val item = result.data.firstOrNull()
                    onResult(
                        item?.let {
                            NutritionEstimate(it.calories, it.proteinGrams, it.carbsGrams, it.fatGrams)
                        }
                    )
                }
                is Result.Error -> onResult(null)
            }
        }
    }
}
