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
import com.shiji.app.ui.theme.BrandGreen
import com.shiji.app.ui.theme.Fat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterScreen(
    onBack: () -> Unit = {},
    waterMl: Int = 0,
    waterGoalMl: Int = 2000,
    onAddWater: (Int) -> Unit = {},
    onSetWaterGoal: (Int) -> Unit = {}
) {
    var showGoalDialog by remember { mutableStateOf(false) }
    var confirmMl by remember { mutableIntStateOf(0) }
    var showConfirm by remember { mutableStateOf(false) }
    var showCustom by remember { mutableStateOf(false) }
    val progress = (waterMl.toFloat() / waterGoalMl.coerceAtLeast(1)).coerceIn(0f, 1f)
    val quickOptions = listOf(150, 200, 300, 550)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("水分摄入", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "返回") } },
                actions = { IconButton(onClick = { showGoalDialog = true }) { Icon(Icons.Filled.Settings, "目标") } },
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
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Fat),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                    Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("💧", style = MaterialTheme.typography.displayLarge)
                        Spacer(Modifier.height(8.dp))
                        Text("$waterMl", fontFamily = FontFamily.Monospace, fontSize = 48.sp,
                            fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
                        Text("/ $waterGoalMl ml", color = androidx.compose.ui.graphics.Color.White.copy(0.7f),
                            style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = androidx.compose.ui.graphics.Color.White,
                            trackColor = androidx.compose.ui.graphics.Color.White.copy(0.3f))
                        if (progress >= 1f) {
                            Spacer(Modifier.height(8.dp))
                            Text("✅ 今日目标达成！", color = androidx.compose.ui.graphics.Color.White,
                                fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Quick add
            item {
                Text("快速加水", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth())
                Text("点击水量后确认，避免误触", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    quickOptions.forEach { ml ->
                        OutlinedButton(
                            onClick = { confirmMl = ml; showConfirm = true },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) { Text("${ml}ml", style = MaterialTheme.typography.labelSmall) }
                    }
                    OutlinedButton(
                        onClick = { showCustom = true },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandGreen)
                    ) { Text("自定义", style = MaterialTheme.typography.labelSmall, color = BrandGreen) }
                }
            }

            item { HorizontalDivider() }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("💡", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.width(12.dp))
                        Text("每日建议饮水 ${waterGoalMl}ml\n运动后适当增加 500-1000ml",
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    // Confirm popup
    if (showConfirm && confirmMl > 0) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("确认加水量", textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("${confirmMl}ml", style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold, color = Fat)
                }
            },
            confirmButton = {
                Button(onClick = { onAddWater(confirmMl); showConfirm = false },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                    Text("确定", color = androidx.compose.ui.graphics.Color.White)
                }
            },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("取消") } }
        )
    }

    // Custom amount dialog
    if (showCustom) {
        var customMl by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCustom = false },
            title = { Text("自定义水量") },
            text = {
                OutlinedTextField(value = customMl,
                    onValueChange = { customMl = it.filter { c -> c.isDigit() } },
                    label = { Text("水量 (ml)") }, singleLine = true, shape = RoundedCornerShape(12.dp))
            },
            confirmButton = {
                Button(onClick = {
                    (customMl.toIntOrNull() ?: 0).let { if (it > 0) { onAddWater(it); showCustom = false } }
                }, enabled = customMl.toIntOrNull()?.let { it > 0 } == true,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)) {
                    Text("确认添加", color = androidx.compose.ui.graphics.Color.White)
                }
            },
            dismissButton = { TextButton(onClick = { showCustom = false }) { Text("取消") } }
        )
    }

    // Goal dialog
    if (showGoalDialog) {
        var goalInput by remember { mutableStateOf(waterGoalMl.toString()) }
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text("每日饮水目标") },
            text = {
                OutlinedTextField(value = goalInput,
                    onValueChange = { goalInput = it.filter { c -> c.isDigit() } },
                    label = { Text("目标 (ml)") }, singleLine = true, shape = RoundedCornerShape(12.dp))
            },
            confirmButton = { TextButton(onClick = {
                (goalInput.toIntOrNull() ?: waterGoalMl).let { if (it > 0) onSetWaterGoal(it) }; showGoalDialog = false
            }) { Text("保存") } },
            dismissButton = { TextButton(onClick = { showGoalDialog = false }) { Text("取消") } }
        )
    }
}
