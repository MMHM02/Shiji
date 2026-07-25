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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalSettingScreen(
    onBack: () -> Unit = {},
    onSave: (heightCm: Double, weightKg: Double, goalType: String) -> Unit = { _, _, _ -> }
) {
    var height by remember { mutableStateOf("175") }
    var weight by remember { mutableStateOf("72") }
    var goalType by remember { mutableStateOf("LOSE_SLOW") }

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
            Text("🎯 设定你的目标", style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold)
            Text("根据你的身体数据，自动计算每日热量和宏量营养素目标",
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

            Text("目标类型", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            val h = height.toDoubleOrNull() ?: 170.0
            val w = weight.toDoubleOrNull() ?: 70.0

            listOf(
                "LOSE_FAST" to "🔥 快速减脂 (每日 ${calcCals(h, w, "LOSE_FAST")} kcal)",
                "LOSE_SLOW" to "🥗 稳步减脂 (每日 ${calcCals(h, w, "LOSE_SLOW")} kcal)",
                "MAINTAIN" to "⚖️ 维持体重 (每日 ${calcCals(h, w, "MAINTAIN")} kcal)",
                "GAIN_SLOW" to "💪 缓慢增肌 (每日 ${calcCals(h, w, "GAIN_SLOW")} kcal)",
                "GAIN_FAST" to "🏋️ 快速增肌 (每日 ${calcCals(h, w, "GAIN_FAST")} kcal)"
            ).forEach { (type, label) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (goalType == type) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    ),
                    border = if (goalType == type) CardDefaults.outlinedCardBorder() else null,
                    onClick = { goalType = type }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, fontWeight = FontWeight.Medium)
                        RadioButton(selected = goalType == type, onClick = { goalType = type })
                    }
                }
            }

            // Summary card
            val targetCals = calcCals(h, w, goalType)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📊 每日目标概览", fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("热量"); Text("$targetCals kcal", fontWeight = FontWeight.Bold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("蛋白质"); Text("${(targetCals * 0.25 / 4).toInt()}g", fontWeight = FontWeight.Bold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("碳水"); Text("${(targetCals * 0.50 / 4).toInt()}g", fontWeight = FontWeight.Bold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("脂肪"); Text("${(targetCals * 0.25 / 9).toInt()}g", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Button(
                onClick = { onSave(h, w, goalType) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp)
            ) { Text("保存目标", style = MaterialTheme.typography.bodyLarge) }

            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun calcCals(heightCm: Double, weightKg: Double, goalType: String): Int {
    val bmr = 10 * weightKg + 6.25 * heightCm - 5 * 25 + 5 // Mifflin-St Jeor (男, 25岁)
    val tdee = (bmr * 1.55).toInt() // moderately active
    return when (goalType) {
        "LOSE_FAST" -> tdee - 750
        "LOSE_SLOW" -> tdee - 350
        "GAIN_SLOW" -> tdee + 350
        "GAIN_FAST" -> tdee + 700
        else -> tdee
    }
}
