package com.shiji.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shiji.app.ui.theme.BrandGreenLight
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * Compact month calendar with green highlights on dates that have food records.
 * Shared by Home screen date picker and Diet Log calendar.
 */
@Composable
fun RecordCalendar(
    currentDate: LocalDate,
    datesWithRecords: Set<LocalDate>,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val yearMonth = YearMonth.from(currentDate)
    val calendarStart = yearMonth.atDay(1).with(DayOfWeek.MONDAY)
    val days = (0 until 42).map { calendarStart.plusDays(it.toLong()) }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(currentDate.format(DateTimeFormatter.ofPattern("yyyy年M月")),
                    fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach { d ->
                    Text(d, Modifier.weight(1f), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(4.dp))
            for (row in 0 until 6) {
                Row(Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val d = days[row * 7 + col]
                        val hasRecord = d in datesWithRecords
                        val isToday = d == today
                        val isSelected = d == currentDate
                        val inMonth = d.month == yearMonth.month

                        Box(
                            modifier = Modifier.weight(1f).aspectRatio(1f).padding(1.dp)
                                .clip(CircleShape)
                                .then(when {
                                    isSelected -> Modifier.background(MaterialTheme.colorScheme.primary)
                                    hasRecord && !isToday -> Modifier.background(BrandGreenLight)
                                    else -> Modifier
                                })
                                .then(if (isToday && !isSelected) Modifier.background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                    CircleShape
                                ) else Modifier)
                                .clickable(enabled = inMonth) { onSelectDate(d) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (inMonth) {
                                Text(d.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = when { isSelected -> MaterialTheme.colorScheme.onPrimary
                                        else -> MaterialTheme.colorScheme.onSurface })
                            }
                        }
                    }
                }
            }
            // Legend
            Row(Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(BrandGreenLight))
                Spacer(Modifier.width(6.dp))
                Text("有记录", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
