package com.shiji.app.ui.water

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shiji.app.ui.theme.Fat
import com.shiji.app.ui.theme.Success

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterScreen(
    onBack: () -> Unit = {},
    waterMl: Int = 0,
    waterGoalMl: Int = 2000,
    onAddWater: (Int) -> Unit = {},
    onSetWaterGoal: (Int) -> Unit = {}
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    val progress = (waterMl.toFloat() / waterGoalMl.coerceAtLeast(1)).coerceIn(0f, 1f)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("水分摄入", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "返回") } },
                actions = {
                    IconButton(onClick = { showGoalDialog = true }) {
                        Icon(Icons.Filled.Settings, "目标")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Water progress
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Fat),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("💧", style = MaterialTheme.typography.displayLarge)
                        Spacer(Modifier.height(8.dp))
                        Text("$waterMl", fontFamily = FontFamily.Monospace,
                            fontSize = 48.sp, fontWeight = FontWeight.Bold,
                            color = androidx.compose.ui.graphics.Color.White)
                        Text("/ $waterGoalMl ml",
                            color = androidx.compose.ui.graphics.Color.White.copy(0.7f),
                            style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = androidx.compose.ui.graphics.Color.White,
                            trackColor = androidx.compose.ui.graphics.Color.White.copy(0.3f)
                        )
                        if (progress >= 1f) {
                            Spacer(Modifier.height(8.dp))
                            Text("✅ 今日目标达成！", color = androidx.compose.ui.graphics.Color.White,
                                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Quick add with confirmation
            item {
                Text("快速加水", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth())
                Text("点击按钮后在弹窗中确认，避免误触", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(100 to "100ml", 200 to "200ml", 300 to "300ml", 500 to "500ml").forEach { (ml, label) ->
                        OutlinedButton(
                            onClick = { showAddDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text(label) }
                    }
                }
            }

            item { HorizontalDivider() }

            // Tips
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💡", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.width(12.dp))
                            Text("每日建议饮水 ${waterGoalMl}ml\n运动后适当增加 500-1000ml",
                                style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    // --- Confirmation popup ---
    if (showAddDialog) {
        var customMl by remember { mutableStateOf("250") }
        var selectedMl by remember { mutableIntStateOf(250) }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("确认加水量") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("选择或输入水量：", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(100, 200, 300, 500, 800).forEach { ml ->
                            FilterChip(
                                selected = selectedMl == ml,
                                onClick = { selectedMl = ml },
                                label = { Text("${ml}ml") }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = customMl,
                        onValueChange = { customMl = it.filter { c -> c.isDigit() } },
                        label = { Text("自定义 (ml)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    val toAdd = customMl.toIntOrNull() ?: selectedMl
                    Text("总共加入 ${toAdd}ml，今日将达 ${waterMl + toAdd}ml",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amount = customMl.toIntOrNull() ?: selectedMl
                    if (amount > 0) onAddWater(amount)
                    showAddDialog = false
                }) { Text("确认添加") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("取消") } }
        )
    }

    // --- Goal setting ---
    if (showGoalDialog) {
        var goalInput by remember { mutableStateOf(waterGoalMl.toString()) }
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text("每日饮水目标 (ml)") },
            text = {
                OutlinedTextField(
                    value = goalInput,
                    onValueChange = { goalInput = it.filter { c -> c.isDigit() } },
                    label = { Text("目标水量") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    (goalInput.toIntOrNull() ?: waterGoalMl).let { if (it > 0) onSetWaterGoal(it) }
                    showGoalDialog = false
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showGoalDialog = false }) { Text("取消") } }
        )
    }
}
