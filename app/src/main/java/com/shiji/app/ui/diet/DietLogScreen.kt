package com.shiji.app.ui.diet

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shiji.core.common.util.PortionConverter
import com.shiji.core.data.entity.FoodRecordEntity
import com.shiji.app.ui.theme.Calories
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietLogScreen(
    records: List<FoodRecordEntity> = emptyList(),
    date: LocalDate = LocalDate.now(),
    onDateChange: (LocalDate) -> Unit = {},
    onEditRecord: (FoodRecordEntity) -> Unit = {},
    onDeleteRecord: (Long) -> Unit = {},
    onBack: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("饮食日志", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "返回") } },
                actions = {
                    IconButton(onClick = { onDateChange(date.minusDays(1)) }) { Icon(Icons.Filled.ChevronLeft, "前一天") }
                    IconButton(onClick = { onDateChange(date.plusDays(1)) }) { Icon(Icons.Filled.ChevronRight, "后一天") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Date display
            Text(
                text = date.format(DateTimeFormatter.ofPattern("M月d日 EEEE")),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (records.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📋", style = MaterialTheme.typography.displayLarge)
                        Spacer(Modifier.height(12.dp))
                        Text("今天还没有记录", style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("点击首页 + 添加食物", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                // Group by meal type
                val mealOrder = listOf("BREAKFAST", "LUNCH", "SNACK", "DINNER")
                val grouped = records.groupBy { it.mealType }

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    mealOrder.forEach { mealType ->
                        val meals = grouped[mealType] ?: return@forEach
                        val subtotal = meals.sumOf { it.calories }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    PortionConverter.mealTypeDisplay(mealType),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "小计: ${subtotal.toInt()} kcal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        items(meals, key = { it.id }) { record ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(PortionConverter.mealTypeEmoji(record.mealType),
                                        style = MaterialTheme.typography.titleLarge)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(record.foodName, fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodyLarge)
                                        Text(
                                            "${record.portion.toInt()}${record.portionUnit} · ${record.recordTime}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        "${record.calories.toInt()}",
                                        fontFamily = FontFamily.Monospace,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Calories
                                    )
                                    Text(" kcal", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    IconButton(onClick = { onDeleteRecord(record.id) }) {
                                        Icon(Icons.Filled.Delete, "删除", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }

                    // Total summary
                    item {
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("今日总摄入", fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${records.sumOf { it.calories }.toInt()} kcal",
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
