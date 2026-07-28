package com.shiji.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shiji.app.ui.theme.BrandGreen
import com.shiji.app.ui.theme.Fat

@Composable
fun WaterProgressBar(
    waterMl: Int,
    goalMl: Int,
    onAdd: (Int) -> Unit,
    onSetGoal: (Int) -> Unit,
    height: Dp = 200.dp,
    modifier: Modifier = Modifier
) {
    val progress = (waterMl.toFloat() / goalMl.coerceAtLeast(1)).coerceIn(0f, 1f)
    var showPopup by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var confirmMl by remember { mutableIntStateOf(0) }
    var showConfirm by remember { mutableStateOf(false) }

    val quickOptions = listOf(150, 200, 300, 550)

    Column(
        modifier = modifier.width(52.dp).height(height),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Box(modifier = Modifier.weight(1f).width(20.dp).clip(RoundedCornerShape(10.dp))) {
            Surface(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(10.dp),
                color = Fat.copy(alpha = 0.15f)) {}
            Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(progress)
                .clip(RoundedCornerShape(10.dp)).align(Alignment.BottomCenter)) {
                Surface(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(10.dp), color = Fat) {}
            }
            if (progress > 0.05f) {
                Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.06f).padding(horizontal = 2.dp)
                    .clip(RoundedCornerShape(10.dp)).align(Alignment.BottomCenter)) {
                    Surface(modifier = Modifier.fillMaxSize(), color = Color.White.copy(alpha = 0.3f)) {}
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("💧", style = MaterialTheme.typography.bodySmall)
        Text("$waterMl", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Fat)
        Text("/${goalMl}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = { showPopup = true }, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
            Text("+", style = MaterialTheme.typography.labelMedium, color = Fat, fontWeight = FontWeight.Bold)
        }
    }

    // Pick amount popup
    if (showPopup) {
        var customMl by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPopup = false },
            title = { Text("记录饮水") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        quickOptions.forEach { ml ->
                            FilterChip(
                                selected = false,
                                onClick = { confirmMl = ml; showPopup = false; showConfirm = true },
                                label = { Text("${ml}ml", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.height(32.dp)
                            )
                        }
                    }
                    OutlinedTextField(
                        value = customMl, onValueChange = { customMl = it.filter { c -> c.isDigit() } },
                        label = { Text("自定义 (ml)") }, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandGreen,
                            cursorColor = BrandGreen
                        )
                    )
                    Button(
                        onClick = {
                            (customMl.toIntOrNull() ?: 0).let { if (it > 0) { onAdd(it); showPopup = false } }
                        },
                        enabled = customMl.toIntOrNull()?.let { it > 0 } == true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                    ) { Text("确认添加", color = Color.White) }
                }
            },
            confirmButton = { TextButton(onClick = { showGoalDialog = true; showPopup = false }) { Text("修改目标") } },
            dismissButton = { TextButton(onClick = { showPopup = false }) { Text("关闭") } }
        )
    }

    // Confirm popup (when quick chip is tapped)
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
                Button(onClick = { onAdd(confirmMl); showConfirm = false },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                    Text("确定", color = Color.White)
                }
            },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("取消") } }
        )
    }

    // Goal dialog
    if (showGoalDialog) {
        var goalInput by remember { mutableStateOf(goalMl.toString()) }
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text("每日饮水目标") },
            text = {
                OutlinedTextField(value = goalInput,
                    onValueChange = { goalInput = it.filter { c -> c.isDigit() } },
                    label = { Text("目标 (ml)") }, singleLine = true, shape = RoundedCornerShape(12.dp))
            },
            confirmButton = { TextButton(onClick = {
                (goalInput.toIntOrNull() ?: goalMl).let { if (it > 0) onSetGoal(it) }; showGoalDialog = false
            }) { Text("保存") } },
            dismissButton = { TextButton(onClick = { showGoalDialog = false }) { Text("取消") } }
        )
    }
}
