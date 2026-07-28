package com.shiji.app.ui.data

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shiji.app.ui.theme.*
import com.shiji.core.data.entity.FoodRecordEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Data dashboard: real weekly trends from Room + data-driven AI suggestion cards.
 * Suggestions are computed from actual records; tapping "追问" hands a
 * pre-filled question to the AI advisor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen(
    modifier: Modifier = Modifier,
    weekRecords: List<FoodRecordEntity> = emptyList(),
    todayRecords: List<FoodRecordEntity> = emptyList(),
    calorieTarget: Double = 2000.0,
    proteinTarget: Double = 60.0,
    fatTarget: Double = 65.0,
    onNavigateToDietLog: () -> Unit = {},
    onNavigateToWeight: () -> Unit = {},
    onNavigateToAIChat: (String) -> Unit = {}
) {
    val today = LocalDate.now()

    // ---- real aggregations ----
    val dailyCalories = todayRecords.sumOf { it.calories }.toInt()
    val dailyProtein = todayRecords.sumOf { it.proteinGrams }.toInt()
    val dailyCarbs = todayRecords.sumOf { it.carbsGrams }.toInt()
    val dailyFat = todayRecords.sumOf { it.fatGrams }.toInt()

    val weekByDay: List<Pair<LocalDate, Double>> = (6 downTo 0).map { offset ->
        val date = today.minusDays(offset.toLong())
        date to weekRecords.filter { it.recordDate == date.toString() }.sumOf { it.calories }
    }
    val daysLogged = weekByDay.count { it.second > 0 }
    val weekAvgCalories = if (daysLogged > 0) weekByDay.filter { it.second > 0 }
        .map { it.second }.average().toInt() else 0
    val weekAvgProtein = if (daysLogged > 0) {
        weekRecords.groupBy { it.recordDate }
            .filter { it.value.isNotEmpty() }
            .map { (_, rs) -> rs.sumOf { it.proteinGrams } }
            .average().toInt()
    } else 0

    // ---- data-driven suggestions ----
    val suggestions = buildSuggestions(
        daysLogged = daysLogged,
        weekAvgCalories = weekAvgCalories,
        weekAvgProtein = weekAvgProtein,
        calorieTarget = calorieTarget.toInt(),
        proteinTarget = proteinTarget.toInt(),
        todayCalories = dailyCalories
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("数据看板", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Section: Today
            item {
                Text("📊 今日概览", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 8.dp))
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCard("今日热量", "$dailyCalories", if (dailyCalories == 0) "开始记录" else "已摄入",
                            if (dailyCalories == 0) StatStatus.WARN else StatStatus.OK, modifier = Modifier.weight(1f))
                        StatCard("今日蛋白", "${dailyProtein}g", if (dailyProtein == 0) "开始记录" else "已摄入",
                            if (dailyProtein == 0) StatStatus.WARN else StatStatus.OK, modifier = Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCard("今日碳水", "${dailyCarbs}g", if (dailyCarbs == 0) "开始记录" else "已摄入",
                            if (dailyCarbs == 0) StatStatus.WARN else StatStatus.OK, modifier = Modifier.weight(1f))
                        StatCard("今日脂肪", "${dailyFat}g", if (dailyFat == 0) "开始记录" else "已摄入",
                            if (dailyFat == 0) StatStatus.WARN else StatStatus.OK, modifier = Modifier.weight(1f))
                    }
                }
            }

            // Section: calorie trend (real data)
            item { Spacer(Modifier.height(16.dp)) }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📈 近7天热量", style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold)
                            if (daysLogged > 0) {
                                Text("日均 $weekAvgCalories kcal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        if (daysLogged < 2) {
                            Box(modifier = Modifier.fillMaxWidth().height(120.dp),
                                contentAlignment = Alignment.Center) {
                                Text("记录 2 天以上后显示趋势图",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            SimpleLineChart(
                                points = weekByDay.map { (date, cal) ->
                                    ChartPoint(date.format(DateTimeFormatter.ofPattern("M/d")), cal.toFloat())
                                },
                                color = MaterialTheme.colorScheme.primary,
                                targetLine = calorieTarget.toFloat(),
                                modifier = Modifier.fillMaxWidth().height(140.dp)
                            )
                        }
                    }
                }
            }

            // Section: AI suggestions (data-driven)
            item { Spacer(Modifier.height(8.dp)) }
            item {
                Text("🤖 AI 建议", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 8.dp))
            }

            if (suggestions.isEmpty()) {
                item {
                    AiSuggestionCard(
                        header = "🌱 开始记录吧",
                        body = "记录几天饮食后，这里会出现基于你真实数据的个性化建议。",
                        actionLabel = "去记录 →",
                        onAction = onNavigateToDietLog
                    )
                }
            } else {
                items(suggestions.size) { i ->
                    val s = suggestions[i]
                    AiSuggestionCard(
                        header = s.header,
                        body = s.body,
                        actionLabel = "追问 AI →",
                        onAction = { onNavigateToAIChat(s.followUp) }
                    )
                }
            }

            // Deep AI insight entry
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    onClick = { onNavigateToAIChat("请全面分析我最近的饮食情况，指出优点和最需要改进的2个问题") }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✨", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("AI 深度分析", fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyLarge)
                            Text("让 AI 顾问结合你的记录做全面复盘",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("→", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Quick links
            item { Spacer(Modifier.height(8.dp)) }
            item {
                Column {
                    TextButton(onClick = onNavigateToDietLog) { Text("📋 饮食日志 →", color = MaterialTheme.colorScheme.primary) }
                    TextButton(onClick = onNavigateToWeight) { Text("⚖️ 体重详情 →", color = MaterialTheme.colorScheme.primary) }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ==================== suggestion engine ====================

private data class Suggestion(val header: String, val body: String, val followUp: String)

private fun buildSuggestions(
    daysLogged: Int,
    weekAvgCalories: Int,
    weekAvgProtein: Int,
    calorieTarget: Int,
    proteinTarget: Int,
    todayCalories: Int
): List<Suggestion> {
    val list = mutableListOf<Suggestion>()

    if (daysLogged >= 3 && weekAvgProtein > 0 && weekAvgProtein < proteinTarget * 0.8) {
        list += Suggestion(
            "💡 蛋白质摄入偏低",
            "近一周日均蛋白质约 ${weekAvgProtein}g，低于目标 ${proteinTarget}g。建议每餐加入鸡蛋、瘦肉或豆制品。",
            "我蛋白质摄入不足，推荐一些方便的高蛋白食物"
        )
    }
    if (daysLogged >= 3 && weekAvgCalories > calorieTarget * 1.15) {
        list += Suggestion(
            "🔥 热量持续超标",
            "近一周日均摄入约 ${weekAvgCalories} kcal，超出目标 ${calorieTarget} kcal。建议减少高糖饮品和油炸食品。",
            "我总是热量超标，有什么控制热量的实用技巧？"
        )
    }
    if (daysLogged >= 3 && weekAvgCalories > 0 && weekAvgCalories < calorieTarget * 0.6) {
        list += Suggestion(
            "⚠️ 热量摄入过低",
            "近一周日均摄入约 ${weekAvgCalories} kcal，远低于目标。长期过低会影响代谢，建议规律三餐。",
            "吃得太少有什么危害？如何健康地吃够热量？"
        )
    }
    if (todayCalories > calorieTarget * 1.2) {
        list += Suggestion(
            "🍽️ 今日热量已超标",
            "今天已摄入 $todayCalories kcal，晚餐建议清淡，如蔬菜沙拉或清蒸类。",
            "今天热量超了，晚餐怎么补救？"
        )
    }
    if (list.isEmpty() && daysLogged >= 3) {
        list += Suggestion(
            "✅ 饮食节奏不错",
            "近一周摄入整体在目标范围内，继续保持！可以试试多样化蛋白质来源。",
            "我的饮食目前不错，还能怎么优化？"
        )
    }
    return list.take(3)
}

// ==================== shared pieces ====================

data class ChartPoint(val day: String, val value: Float)

enum class StatStatus { OK, WARN }

@Composable
private fun StatCard(label: String, value: String, status: String, statusType: StatStatus, modifier: Modifier = Modifier) {
    val statusColor = when (statusType) {
        StatStatus.OK -> Success
        StatStatus.WARN -> Warning
    }
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(value, fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(status, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = statusColor)
        }
    }
}

@Composable
private fun AiSuggestionCard(header: String, body: String, actionLabel: String, onAction: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(header, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onAction, modifier = Modifier.align(Alignment.End)) {
                Text(actionLabel, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun SimpleLineChart(
    points: List<ChartPoint>,
    color: Color,
    targetLine: Float?,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val padLeft = 12.dp.toPx()
        val padRight = 12.dp.toPx()
        val padBottom = 8.dp.toPx()
        val padTop = 8.dp.toPx()

        val chartW = w - padLeft - padRight
        val chartH = h - padTop - padBottom

        val maxVal = (points.maxOf { it.value } * 1.15f).coerceAtLeast(targetLine ?: 0f).coerceAtLeast(1f)
        val minVal = 0f

        // Grid lines
        for (i in 0..3) {
            val y = padTop + chartH * i / 3
            drawLine(Color.LightGray.copy(alpha = 0.4f), Offset(padLeft, y), Offset(w - padRight, y), strokeWidth = 1f)
        }

        fun yOf(v: Float) = padTop + chartH * (1 - (v - minVal) / (maxVal - minVal))

        // Target line
        if (targetLine != null) {
            drawLine(
                Color.Gray.copy(alpha = 0.6f), Offset(padLeft, yOf(targetLine)), Offset(w - padRight, yOf(targetLine)),
                strokeWidth = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f))
            )
        }

        if (points.size >= 2) {
            // Area fill
            val areaPath = Path().apply {
                moveTo(padLeft, padTop + chartH)
                points.forEachIndexed { i, pt ->
                    lineTo(padLeft + chartW * i / (points.size - 1), yOf(pt.value))
                }
                lineTo(padLeft + chartW, padTop + chartH)
                close()
            }
            drawPath(areaPath, color.copy(alpha = 0.12f))

            // Line
            val linePath = Path()
            points.forEachIndexed { i, pt ->
                val x = padLeft + chartW * i / (points.size - 1)
                if (i == 0) linePath.moveTo(x, yOf(pt.value)) else linePath.lineTo(x, yOf(pt.value))
            }
            drawPath(linePath, color, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        // Dots
        points.forEachIndexed { i, pt ->
            drawCircle(color, 4.dp.toPx(), Offset(padLeft + chartW * i / (points.size - 1), yOf(pt.value)))
        }
    }
}
