package com.shiji.app.ui.onboarding

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(
    step: Int = 1,
    onStepChange: (Int) -> Unit = {},
    onComplete: (heightCm: Double, weightKg: Double, goalType: String) -> Unit = { _, _, _ -> },
    onSkip: () -> Unit = {},
    onNavigateToAiSettings: () -> Unit = {}
) {
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var selectedGoal by remember { mutableStateOf("LOSE_SLOW") }

    val h = height.toDoubleOrNull() ?: 170.0
    val w = weight.toDoubleOrNull() ?: 70.0
    val targetCals = calcCals(h, w, selectedGoal)

    Scaffold { innerPadding ->
        when (step) {
            1 -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 32.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Progress
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(3) { i ->
                            Surface(modifier = Modifier.size(if (i + 1 == step) 10.dp else 8.dp),
                                shape = RoundedCornerShape(50),
                                color = if (i + 1 <= step) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant) {}
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("1 / 3", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(24.dp))

                    Text("🎯", style = MaterialTheme.typography.displayLarge)
                    Spacer(Modifier.height(12.dp))
                    Text("设定你的目标", style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold)
                    Text("填写身体数据，自动计算每日营养目标",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(20.dp))

                    OutlinedTextField(
                        value = height, onValueChange = { height = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("身高 (cm)") }, placeholder = { Text("例如 175") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = weight, onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("当前体重 (kg)") }, placeholder = { Text("例如 72") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))

                    Spacer(Modifier.height(16.dp))
                    Text("目标类型", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth())

                    val presetTypes = listOf(
                        "LOSE_FAST" to "🔥 快速减脂",
                        "LOSE_SLOW" to "🥗 稳步减脂",
                        "MAINTAIN" to "⚖️ 维持体重",
                        "GAIN_SLOW" to "💪 缓慢增肌",
                        "GAIN_FAST" to "🏋️ 快速增肌",
                        "CUSTOM" to "🔧 自定义（后续在目标设定中调整）"
                    )
                    presetTypes.forEach { (type, label) ->
                        val cal = calcCals(h, w, type)
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedGoal == type) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            ),
                            border = if (selectedGoal == type) CardDefaults.outlinedCardBorder() else null,
                            onClick = { selectedGoal = type }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(if (type == "CUSTOM") "🔧 自定义" else "$label · $cal kcal",
                                    fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                                RadioButton(selected = selectedGoal == type, onClick = { selectedGoal = type })
                            }
                        }
                    }

                    // Summary card
                    if (height.isNotBlank() || weight.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("📊 每日目标: $targetCals kcal",
                                    fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Text("蛋白质 ${(targetCals * 0.25 / 4).toInt()}g · 碳水 ${(targetCals * 0.50 / 4).toInt()}g · 脂肪 ${(targetCals * 0.25 / 9).toInt()}g",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Next button
                    Button(
                        onClick = { onStepChange(2) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(26.dp)
                    ) { Text("下一步", style = MaterialTheme.typography.bodyLarge) }

                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onSkip) { Text("跳过") }
                    Spacer(Modifier.height(16.dp))
                }
            }

            2 -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(3) { i ->
                            Surface(modifier = Modifier.size(if (i + 1 == step) 10.dp else 8.dp),
                                shape = RoundedCornerShape(50),
                                color = if (i + 1 <= step) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant) {}
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("2 / 3", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(Modifier.weight(1f))

                    Text("🔑", style = MaterialTheme.typography.displayLarge)
                    Spacer(Modifier.height(16.dp))
                    Text("配置 AI", style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("接入你的 API Key\n拍照识食 + 文字记录 + AI 顾问",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = onNavigateToAiSettings,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Filled.Settings, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("现在配置 API Key")
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("也可以稍后在「我的 → AI 模型配置」中设置",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline)

                    Spacer(Modifier.weight(1f))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { onStepChange(1) }) { Text("← 上一步") }
                        TextButton(onClick = { onStepChange(3) }) { Text("下一步 →") }
                    }
                    TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) { Text("跳过") }
                }
            }

            3 -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(3) { i ->
                            Surface(modifier = Modifier.size(if (i + 1 == step) 10.dp else 8.dp),
                                shape = RoundedCornerShape(50),
                                color = if (i + 1 <= step) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant) {}
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("3 / 3", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(Modifier.weight(1f))

                    Text("✅", style = MaterialTheme.typography.displayLarge)
                    Spacer(Modifier.height(16.dp))
                    Text("一切就绪！", style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("开始记录你的饮食\n开启健康之旅",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center)

                    Spacer(Modifier.weight(1f))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { onStepChange(2) }) { Text("← 上一步") }
                        TextButton(onClick = onSkip) { Text("跳过") }
                    }
                    Button(
                        onClick = { onComplete(h, w, selectedGoal) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(26.dp)
                    ) { Text("开始使用", style = MaterialTheme.typography.bodyLarge) }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

private fun calcCals(heightCm: Double, weightKg: Double, goalType: String): Int {
    val bmr = 10 * weightKg + 6.25 * heightCm - 5 * 25 + 5
    val tdee = (bmr * 1.55).toInt()
    return when (goalType) {
        "LOSE_FAST" -> tdee - 750; "LOSE_SLOW" -> tdee - 350
        "GAIN_SLOW" -> tdee + 350; "GAIN_FAST" -> tdee + 700
        else -> tdee
    }
}
