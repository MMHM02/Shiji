package com.shiji.app.ui.weight

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shiji.app.ui.theme.BrandGreen
import com.shiji.app.ui.theme.Fat
import com.shiji.core.data.entity.HealthMetricEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightScreen(
    onBack: () -> Unit = {},
    onSaveWeight: (Double) -> Unit = {},
    weightHistory: Flow<List<HealthMetricEntity>> = kotlinx.coroutines.flow.flowOf(emptyList())
) {
    var selectedRange by remember { mutableIntStateOf(7) }
    var weightInput by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    // Real Room data
    val weightData by weightHistory.collectAsStateWithLifecycle(emptyList())

    val latestWeight = weightData.lastOrNull()?.value
    val isEmpty = weightData.isEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("体重追踪", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "返回") } },
                actions = { IconButton(onClick = { showAddDialog = true }) { Icon(Icons.Filled.Add, "记录") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandGreen),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("当前体重", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (isEmpty) "--" else "${latestWeight!!}",
                            fontFamily = FontFamily.Monospace, fontSize = 48.sp, fontWeight = FontWeight.Bold,
                            color = Color.White, letterSpacing = (-0.02).em
                        )
                        Text("kg", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }

            // Chart
            if (!isEmpty && weightData.size >= 2) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("📈 体重趋势", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(7, 30, 90).forEach { days ->
                                        FilterChip(selected = selectedRange == days, onClick = { selectedRange = days },
                                            label = { Text("${days}天") })
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                                val w = size.width; val h = size.height
                                val pL = 40.dp.toPx(); val pR = 16.dp.toPx()
                                val pT = 12.dp.toPx(); val pB = 24.dp.toPx()
                                val cW = w - pL - pR; val cH = h - pT - pB
                                val pts = weightData.takeLast(selectedRange.coerceAtMost(weightData.size))
                                if (pts.size >= 2) {
                                    val minW = (pts.minOf { it.value } - 0.5).toFloat()
                                    val maxW = (pts.maxOf { it.value } + 0.5).toFloat()
                                    for (i in 0..3) {
                                        val y = pT + cH * i / 3f
                                        drawLine(Color.Gray.copy(alpha = 0.2f), Offset(pL, y), Offset(w - pR, y), 1f)
                                    }
                                    for (i in 0 until pts.size - 1) {
                                        val x1 = pL + cW * i / (pts.size - 1).toFloat()
                                        val y1 = pT + cH * (1f - (pts[i].value.toFloat() - minW) / (maxW - minW))
                                        val x2 = pL + cW * (i + 1) / (pts.size - 1).toFloat()
                                        val y2 = pT + cH * (1f - (pts[i + 1].value.toFloat() - minW) / (maxW - minW))
                                        drawLine(Fat, Offset(x1, y1), Offset(x2, y2), 2.5.dp.toPx())
                                    }
                                    pts.forEachIndexed { i, pt ->
                                        drawCircle(Fat, 4.dp.toPx(),
                                            Offset(pL + cW * i / (pts.size - 1).toFloat(),
                                                pT + cH * (1f - (pt.value.toFloat() - minW) / (maxW - minW))))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // History
            item { Text("历史记录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            if (isEmpty) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("还没有体重记录\n点击右上角 + 添加第一条", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(weightData.reversed()) { entry ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(entry.recordDate, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${entry.value} kg", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("记录体重") },
            text = {
                OutlinedTextField(
                    value = weightInput, onValueChange = { weightInput = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("体重 (kg)") }, shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(onClick = {
                    val w = weightInput.toDoubleOrNull()
                    if (w != null && w > 0) {
                        onSaveWeight(w)
                    }
                    showAddDialog = false; weightInput = ""
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("取消") } }
        )
    }
}
