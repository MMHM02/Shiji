package com.shiji.app.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
    var height by remember { mutableStateOf("175") }
    var weight by remember { mutableStateOf("72") }
    var goalType by remember { mutableStateOf("LOSE_SLOW") }
    var selectedGoal by remember { mutableStateOf("LOSE_SLOW") }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress dots
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { i ->
                    Surface(
                        modifier = Modifier.size(if (i + 1 == step) 10.dp else 8.dp),
                        shape = RoundedCornerShape(50),
                        color = if (i + 1 <= step) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    ) {}
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("${step} / 3", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.weight(1f))

            when (step) {
                1 -> {
                    Text("🎯", style = MaterialTheme.typography.displayLarge)
                    Spacer(Modifier.height(16.dp))
                    Text("设定你的目标", style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text("告诉我们你的身体数据\n自动计算每日营养目标",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center)
                    Spacer(Modifier.height(32.dp))
                    OutlinedTextField(value = height, onValueChange = { height = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("身高 (cm)") }, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp))
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = weight, onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("体重 (kg)") }, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("目标", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(4.dp))
                    listOf(
                        "LOSE_FAST" to "🔥 快速减脂",
                        "LOSE_SLOW" to "🥗 稳步减脂",
                        "MAINTAIN" to "⚖️ 维持体重",
                        "GAIN_SLOW" to "💪 缓慢增肌"
                    ).forEach { (type, label) ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedGoal == type, onClick = { selectedGoal = type })
                            Spacer(Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                2 -> {
                    Text("🔑", style = MaterialTheme.typography.displayLarge)
                    Spacer(Modifier.height(16.dp))
                    Text("配置 AI", style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text("接入你的 AI API Key\n拍照识食 + 语音记录 + AI 顾问",
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
                }
                3 -> {
                    Text("✅", style = MaterialTheme.typography.displayLarge)
                    Spacer(Modifier.height(16.dp))
                    Text("一切就绪！", style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text("开始记录你的饮食\n开启健康之旅",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center)
                }
            }

            Spacer(Modifier.weight(1f))

            // Bottom: back + skip + next
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button (visible when step > 1)
                if (step > 1) {
                    TextButton(onClick = { onStepChange(step - 1) }) { Text("← 上一步") }
                } else {
                    Spacer(Modifier.width(72.dp))
                }
                // Skip
                TextButton(onClick = onSkip) { Text("跳过") }
            }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
                    if (step < 3) onStepChange(step + 1)
                    else onComplete(height.toDoubleOrNull() ?: 175.0, weight.toDoubleOrNull() ?: 72.0, selectedGoal)
                },
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(if (step < 3) "下一步" else "开始使用")
            }
        }
    }
}
