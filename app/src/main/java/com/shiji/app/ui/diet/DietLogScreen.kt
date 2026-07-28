package com.shiji.app.ui.diet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shiji.core.common.util.PortionConverter
import com.shiji.core.data.entity.FoodRecordEntity
import com.shiji.app.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietLogScreen(
    records: List<FoodRecordEntity> = emptyList(),
    date: LocalDate = LocalDate.now(),
    onDateChange: (LocalDate) -> Unit = {},
    onDeleteRecord: (Long) -> Unit = {},
    onBack: () -> Unit = {},
    waterMl: Int = 0,
    waterGoalMl: Int = 2000,
    datesWithRecords: Set<LocalDate> = emptySet()
) {
    var showCalendar by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("饮食日志", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "返回") } },
                actions = {
                    IconButton(onClick = { showCalendar = !showCalendar }) {
                        Icon(Icons.Filled.CalendarMonth, "日历")
                    }
                    IconButton(onClick = { onDateChange(date.minusDays(1)) }) { Icon(Icons.Filled.ChevronLeft, "前一天") }
                    IconButton(onClick = { onDateChange(date.plusDays(1)) }) { Icon(Icons.Filled.ChevronRight, "后一天") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Calendar dropdown
            if (showCalendar) {
                DietCalendar(
                    currentDate = date,
                    datesWithRecords = datesWithRecords,
                    onSelectDate = { onDateChange(it); showCalendar = false }
                )
            }

            // Date display
            Text(
                text = date.format(DateTimeFormatter.ofPattern("M月d日 EEEE")),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Summary calories bar
            val totalCal = records.sumOf { it.calories }.toInt()
            if (records.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("🔥 $totalCal kcal", style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium, color = Calories)
                    Text("🥩 ${records.sumOf { it.proteinGrams }.toInt()}g 蛋白",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("💧 ${records.sumOf { it.carbsGrams }.toInt()}g 碳水",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (records.isEmpty() && waterMl == 0) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📋", style = MaterialTheme.typography.displayLarge)
                        Spacer(Modifier.height(12.dp))
                        Text("今天还没有记录", style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                val mealOrder = listOf("BREAKFAST", "LUNCH", "SNACK", "DINNER")
                val grouped = records.groupBy { it.mealType }
                val hasFood = records.isNotEmpty()

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (hasFood) {
                        mealOrder.forEach { mealType ->
                            val meals = grouped[mealType] ?: return@forEach
                            val subtotal = meals.sumOf { it.calories }
                            item {
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(PortionConverter.mealTypeDisplay(mealType),
                                        style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                    Text("小计: ${subtotal.toInt()} kcal",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            items(meals, key = { it.id }) { record ->
                                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(PortionConverter.mealTypeEmoji(record.mealType),
                                            style = MaterialTheme.typography.titleLarge)
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(record.foodName, fontWeight = FontWeight.SemiBold,
                                                style = MaterialTheme.typography.bodyLarge)
                                            Text(
                                                "${if (record.portion == record.portion.toLong().toDouble()) record.portion.toLong() else String.format("%.1f", record.portion)}${record.portionUnit} · ${record.recordTime}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text("${record.calories.toInt()}", fontFamily = FontFamily.Monospace,
                                            style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold,
                                            color = Calories)
                                        Text(" kcal", style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        IconButton(onClick = { onDeleteRecord(record.id) }) {
                                            Icon(Icons.Filled.Delete, "删除", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                        // Food total
                        item {
                            HorizontalDivider()
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Text("食物总摄入", fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.titleMedium)
                                Text("${records.sumOf { it.calories }.toInt()} kcal",
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Water intake row — always visible on the selected date
                    item {
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("💧", style = MaterialTheme.typography.titleLarge)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("饮水", fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.titleMedium)
                                    Text("目标 $waterGoalMl ml", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text(
                                "$waterMl / ${waterGoalMl}ml",
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Fat
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (waterMl.toFloat() / waterGoalMl.coerceAtLeast(1)).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = Fat,
                            trackColor = Fat.copy(alpha = 0.15f)
                        )
                    }

                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

// ==================== calendar ====================

@Composable
private fun DietCalendar(
    currentDate: LocalDate,
    datesWithRecords: Set<LocalDate>,
    onSelectDate: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val yearMonth = YearMonth.from(currentDate)
    val startOfMonth = yearMonth.atDay(1)
    // Start from the Monday of the week containing day 1
    val calendarStart = startOfMonth.with(DayOfWeek.MONDAY)
    // 6 weeks for full coverage
    val days = (0 until 42).map { calendarStart.plusDays(it.toLong()) }

    val fmt = DateTimeFormatter.ofPattern("M/d")
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Month header
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(
                    currentDate.format(DateTimeFormatter.ofPattern("yyyy年M月")),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Spacer(Modifier.height(8.dp))
            // Day-of-week headers
            Row(Modifier.fillMaxWidth()) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach { d ->
                    Text(d, Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(4.dp))
            // 6 rows
            for (row in 0 until 6) {
                Row(Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val d = days[row * 7 + col]
                        val hasRecord = d in datesWithRecords
                        val isToday = d == today
                        val isSelected = d == currentDate
                        val inMonth = d.month == yearMonth.month

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(1.dp)
                                .clip(CircleShape)
                                .then(
                                    when {
                                        isSelected -> Modifier.background(MaterialTheme.colorScheme.primary)
                                        hasRecord -> Modifier.background(BrandGreenLight)
                                        else -> Modifier
                                    }
                                )
                                .clickable(enabled = inMonth) { onSelectDate(d) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (inMonth) {
                                Text(
                                    d.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 2.dp),
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            // Legend
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(BrandGreenLight))
                Spacer(Modifier.width(6.dp))
                Text("有记录", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
