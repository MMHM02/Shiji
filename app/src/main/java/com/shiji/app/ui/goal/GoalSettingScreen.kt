package com.shiji.app.ui.goal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.shiji.core.data.entity.UserGoalEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalSettingScreen(
    onBack: () -> Unit = {},
    existingGoal: UserGoalEntity? = null,
    latestWeight: Double? = null,
    onSave: (UserGoalEntity) -> Unit = {}
) {
    // Pre-fill from existing data or current weight tracking
    val savedWeight = existingGoal?.currentWeightKg ?: latestWeight
    var height by remember { mutableStateOf(existingGoal?.heightCm?.let { if (it % 1 == 0.0) it.toInt().toString() else it.toString() } ?: "175") }
    var weight by remember(savedWeight) { mutableStateOf(savedWeight?.let { if (it % 1 == 0.0) it.toInt().toString() else it.toString() } ?: "72") }
    var goalType by remember { mutableStateOf(existingGoal?.goalType ?: "CUSTOM") }
    var showAdvanced by remember { mutableStateOf(goalType == "CUSTOM") }

    val h = height.toDoubleOrNull() ?: 170.0
    val w = weight.toDoubleOrNull() ?: 70.0
    val autoCals = calcCals(h, w, goalType)
    val autoProtein = (autoCals * 0.25 / 4).toInt()
    val autoCarbs = (autoCals * 0.50 / 4).toInt()
    val autoFat = (autoCals * 0.25 / 9).toInt()

    var customCals by remember { mutableStateOf((existingGoal?.dailyCalories?.toInt()?.toString() ?: autoCals.toString())) }
    var customProtein by remember { mutableStateOf((existingGoal?.proteinTargetGrams?.toInt()?.toString() ?: autoProtein.toString())) }
    var customCarbs by remember { mutableStateOf((existingGoal?.carbsTargetGrams?.toInt()?.toString() ?: autoCarbs.toString())) }
    var customFat by remember { mutableStateOf((existingGoal?.fatTargetGrams?.toInt()?.toString() ?: autoFat.toString())) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("目标设定", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("🎯 设定你的目标", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("体重与体重追踪同步，修改后两边数据保持一致",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = height, onValueChange = { height = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("身高 (cm)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            OutlinedTextField(
                value = weight, onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("当前体重 (kg)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            if (savedWeight != null && savedWeight != w) {
                Text("📌 最新记录体重: ${savedWeight}kg",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
            }

            Text("目标类型", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            val presetTypes = listOf(
                "LOSE_FAST" to "🔥 快速减脂",
                "LOSE_SLOW" to "🥗 稳步减脂",
                "MAINTAIN" to "⚖️ 维持体重",
                "GAIN_SLOW" to "💪 缓慢增肌",
                "GAIN_FAST" to "🏋️ 快速增肌",
                "CUSTOM" to "🔧 自定义"
            )

            presetTypes.forEach { (type, label) ->
                val targetCal = calcCals(h, w, type)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (goalType == type) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    ),
                    border = if (goalType == type) CardDefaults.outlinedCardBorder() else null,
                    onClick = {
                        goalType = type
                        showAdvanced = type == "CUSTOM"
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (type == "CUSTOM") "🔧 自定义（手动设置所有指标）"
                        else "$label · $targetCal kcal",
                            fontWeight = FontWeight.Medium)
                        RadioButton(selected = goalType == type, onClick = {
                            goalType = type
                            showAdvanced = type == "CUSTOM"
                        })
                    }
                }
            }

            // Always show editable fields (expanded for CUSTOM)
            if (goalType == "CUSTOM" || showAdvanced) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    onClick = { showAdvanced = !showAdvanced }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("📊 营养目标${if (goalType != "CUSTOM") "（可手动覆盖）" else ""}",
                                fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                            if (goalType != "CUSTOM")
                                Icon(if (showAdvanced) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null)
                        }
                        if (goalType == "CUSTOM" || showAdvanced) {
                            Spacer(Modifier.height(12.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = customCals,
                                    onValueChange = { customCals = it.filter { c -> c.isDigit() } },
                                    label = { Text("热量 kcal") }, modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp), singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                                OutlinedTextField(value = customProtein,
                                    onValueChange = { customProtein = it.filter { c -> c.isDigit() } },
                                    label = { Text("蛋白质 g") }, modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp), singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = customCarbs,
                                    onValueChange = { customCarbs = it.filter { c -> c.isDigit() } },
                                    label = { Text("碳水 g") }, modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp), singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                                OutlinedTextField(value = customFat,
                                    onValueChange = { customFat = it.filter { c -> c.isDigit() } },
                                    label = { Text("脂肪 g") }, modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp), singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            }
                            if (goalType != "CUSTOM") {
                                Spacer(Modifier.height(8.dp))
                                TextButton(onClick = {
                                    customCals = autoCals.toString()
                                    customProtein = autoProtein.toString()
                                    customCarbs = autoCarbs.toString()
                                    customFat = autoFat.toString()
                                }) { Text("重置为自动计算") }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    onSave(UserGoalEntity(
                        heightCm = h,
                        currentWeightKg = w,
                        dailyCalories = (customCals.toIntOrNull() ?: autoCals).toDouble(),
                        proteinTargetGrams = (customProtein.toIntOrNull() ?: autoProtein).toDouble(),
                        carbsTargetGrams = (customCarbs.toIntOrNull() ?: autoCarbs).toDouble(),
                        fatTargetGrams = (customFat.toIntOrNull() ?: autoFat).toDouble(),
                        goalType = goalType,
                        updatedAt = System.currentTimeMillis()
                    ))
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp)
            ) { Text("保存目标", style = MaterialTheme.typography.bodyLarge) }

            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun calcCals(heightCm: Double, weightKg: Double, goalType: String): Int {
    val bmr = 10 * weightKg + 6.25 * heightCm - 5 * 25 + 5
    val tdee = (bmr * 1.55).toInt()
    return when (goalType) {
        "LOSE_FAST" -> tdee - 750
        "LOSE_SLOW" -> tdee - 350
        "GAIN_SLOW" -> tdee + 350
        "GAIN_FAST" -> tdee + 700
        else -> tdee // MAINTAIN and CUSTOM default to TDEE
    }
}
