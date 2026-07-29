package com.shiji.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.shiji.app.ui.components.*
import com.shiji.app.ui.theme.*
import com.shiji.core.common.util.PortionConverter
import com.shiji.core.data.entity.FoodRecordEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    todayRecords: List<FoodRecordEntity> = emptyList(),
    calorieTarget: Float = 2000f,
    proteinTarget: Float = 60f,
    carbsTarget: Float = 250f,
    fatTarget: Float = 65f,
    waterMl: Int = 0,
    waterGoalMl: Int = 2000,
    selectedDate: LocalDate = LocalDate.now(),
    datesWithRecords: Set<LocalDate> = emptySet(),
    onDateChange: (LocalDate) -> Unit = {},
    onAddWater: (Int) -> Unit = {},
    onSetWaterGoal: (Int) -> Unit = {},
    onNavigateToCamera: () -> Unit = {},
    onNavigateToTextRecord: () -> Unit = {},
    onNavigateToManual: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToDietLog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val isToday = selectedDate == today
    val totalCal = todayRecords.sumOf { it.calories }.toFloat()
    val totalProtein = todayRecords.sumOf { it.proteinGrams }.toFloat()
    val totalCarbs = todayRecords.sumOf { it.carbsGrams }.toFloat()
    val totalFat = todayRecords.sumOf { it.fatGrams }.toFloat()
    var showDatePicker by remember { mutableStateOf(false) }

    // Calendar popup
    if (showDatePicker) {
        AlertDialog(
            onDismissRequest = { showDatePicker = false },
            title = { Text("选择日期", textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()) },
            text = {
                RecordCalendar(
                    currentDate = selectedDate,
                    datesWithRecords = datesWithRecords,
                    onSelectDate = { onDateChange(it); showDatePicker = false }
                )
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("关闭") }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        modifier = Modifier.clickable { showDatePicker = true }
                    ) {
                        Text(if (isToday) "今天" else "补签",
                            style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (isToday) formatDateLabel(selectedDate)
                                else selectedDate.format(DateTimeFormatter.ofPattern("M月d日 EEEE")),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold, letterSpacing = (-0.01).em
                            )
                            Icon(Icons.Filled.ArrowDropDown, null, modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (!isToday) {
                            TextButton(
                                onClick = { onDateChange(today) },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("← 回到今天", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Calories Ring + Water Bar (side by side)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CaloriesRing(intake = totalCal, target = calorieTarget, size = 200.dp)
                    Spacer(Modifier.width(20.dp))
                    WaterProgressBar(
                        waterMl = waterMl,
                        goalMl = waterGoalMl,
                        onAdd = onAddWater,
                        onSetGoal = onSetWaterGoal,
                        height = 200.dp
                    )
                }
            }

            // Nutrient Cards
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NutrientCard(label = "蛋白质", value = totalProtein, unit = "g", target = proteinTarget,
                        color = Protein, modifier = Modifier.weight(1f))
                    NutrientCard(label = "碳水", value = totalCarbs, unit = "g", target = carbsTarget,
                        color = Carbs, modifier = Modifier.weight(1f))
                    NutrientCard(label = "脂肪", value = totalFat, unit = "g", target = fatTarget,
                        color = Fat, modifier = Modifier.weight(1f))
                }
            }

            // Quick Entry — 3 buttons (no voice)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickEntryCard(icon = Icons.Filled.CameraAlt, label = "拍照识食",
                        onClick = onNavigateToCamera, modifier = Modifier.weight(1f))
                    QuickEntryCard(icon = Icons.Outlined.Create, label = "文字记录",
                        onClick = onNavigateToTextRecord, modifier = Modifier.weight(1f))
                    QuickEntryCard(icon = Icons.Filled.EditNote, label = "手动记录",
                        onClick = onNavigateToManual, modifier = Modifier.weight(1f))
                }
            }

            // Section Header
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("今日饮食", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = onNavigateToDietLog) {
                        Text("查看全部 →", color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Food list
            if (todayRecords.isEmpty()) {
                item {
                    EmptyState(icon = "🍽️", title = "还没有记录",
                        description = "点击上方按钮开始记录你的第一餐",
                        actionLabel = "文字记录", onAction = onNavigateToTextRecord)
                }
            } else {
                val mealOrder = listOf("BREAKFAST", "LUNCH", "SNACK", "DINNER")
                val grouped = todayRecords.groupBy { it.mealType }
                mealOrder.forEach { mealType ->
                    val meals = grouped[mealType] ?: return@forEach
                    val subtotal = meals.sumOf { it.calories }.toInt()
                    item {
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(PortionConverter.mealTypeDisplay(mealType), style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${meals.first().recordTime} · ${subtotal} kcal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    items(meals) { record ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Text(PortionConverter.mealTypeEmoji(record.mealType),
                                    style = MaterialTheme.typography.titleLarge)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(record.foodName, fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyLarge)
                                    Text(formatPortion(record.portion, record.portionUnit),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("${record.calories.toInt()}", fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold,
                                    color = Calories)
                                Text(" kcal", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

private fun formatPortion(portion: Double, unit: String): String {
    val p = if (portion == portion.toLong().toDouble()) portion.toLong().toString()
    else String.format("%.1f", portion)
    return "$p$unit"
}

private fun formatDateLabel(date: LocalDate): String {
    val dayOfWeek = when (date.dayOfWeek) {
        java.time.DayOfWeek.MONDAY -> "周一"
        java.time.DayOfWeek.TUESDAY -> "周二"
        java.time.DayOfWeek.WEDNESDAY -> "周三"
        java.time.DayOfWeek.THURSDAY -> "周四"
        java.time.DayOfWeek.FRIDAY -> "周五"
        java.time.DayOfWeek.SATURDAY -> "周六"
        else -> "周日"
    }
    return "${date.monthValue}月${date.dayOfMonth}日 $dayOfWeek"
}
