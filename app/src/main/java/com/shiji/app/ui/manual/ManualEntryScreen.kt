package com.shiji.app.ui.manual

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.shiji.core.common.util.PortionConverter
import com.shiji.core.common.util.PortionConverter.PortionUnit
import com.shiji.core.data.entity.CachedFoodItemEntity
import com.shiji.core.data.entity.FoodRecordEntity
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryScreen(
    onSaved: (FoodRecordEntity) -> Unit = {},
    onBack: () -> Unit = {},
    selectedDate: LocalDate = LocalDate.now(),
    cachedFoods: List<CachedFoodItemEntity> = emptyList(),
    onSearchFoods: (String) -> List<CachedFoodItemEntity> = { emptyList() },
    viewModel: ManualEntryViewModel = hiltViewModel()
) {
    var foodName by remember { mutableStateOf("") }
    var portion by remember { mutableStateOf("100") }
    var selectedUnit by remember { mutableStateOf(PortionUnit.GRAMS) }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var selectedMeal by remember { mutableStateOf(PortionConverter.inferMealType()) }
    var showUnitPicker by remember { mutableStateOf(false) }
    var showFoodSuggestions by remember { mutableStateOf(false) }

    val digitFilter = { c: Char -> c.isDigit() || c == '.' }

    // Auto-suggest from food library
    val matches = remember(foodName, cachedFoods) {
        if (foodName.length >= 1) cachedFoods.filter { it.name.contains(foodName, ignoreCase = true) }.take(5)
        else emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("手动添加食物", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Food name with search
            item {
                OutlinedTextField(
                    value = foodName, onValueChange = { foodName = it; showFoodSuggestions = it.isNotEmpty() },
                    label = { Text("食物名称") }, placeholder = { Text("如：宫保鸡丁盖饭") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { Icon(Icons.Filled.Search, "搜索") },
                    shape = RoundedCornerShape(12.dp)
                )
            }
            if (showFoodSuggestions && matches.isNotEmpty()) {
                items(matches) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            foodName = item.name
                            portion = item.defaultPortion.toInt().toString()
                            selectedUnit = PortionUnit.fromString(item.defaultUnit)
                            calories = (item.caloriesPer100g * item.defaultPortion / 100).toInt().toString()
                            protein = (item.proteinPer100g * item.defaultPortion / 100).toInt().toString()
                            carbs = (item.carbsPer100g * item.defaultPortion / 100).toInt().toString()
                            fat = (item.fatPer100g * item.defaultPortion / 100).toInt().toString()
                            showFoodSuggestions = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🍽️", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.Medium)
                                Text("${item.caloriesPer100g.toInt()} kcal/100g · 已用${item.useCount}次",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Portion + Unit
            item {
                Text("份量与单位", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = portion, onValueChange = { portion = it.filter(digitFilter) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)
                    )
                    Box {
                        OutlinedButton(onClick = { showUnitPicker = true }) { Text(selectedUnit.display) }
                        DropdownMenu(expanded = showUnitPicker, onDismissRequest = { showUnitPicker = false }) {
                            PortionUnit.entries.forEach { unit ->
                                DropdownMenuItem(text = { Text("${unit.display} (${unit.name})") },
                                    onClick = { selectedUnit = unit; showUnitPicker = false })
                            }
                        }
                    }
                }
            }

            // AI Calculate button
            item {
                var aiLoading by remember { mutableStateOf(false) }
                var aiResult by remember { mutableStateOf<String?>(null) }
                val scope = rememberCoroutineScope()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("营养素", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            if (foodName.isNotBlank()) {
                                aiLoading = true; aiResult = null
                                viewModel.estimate(foodName, portion, selectedUnit.display) { estimate ->
                                    if (estimate != null) {
                                        calories = estimate.calories.toInt().toString()
                                        protein = estimate.protein.toInt().toString()
                                        carbs = estimate.carbs.toInt().toString()
                                        fat = estimate.fat.toInt().toString()
                                        aiResult = "✅ AI 已估算"
                                    } else {
                                        // Offline fallback based on food name
                                        val fn = foodName.lowercase()
                                        calories = when { "鸡" in fn || "肉" in fn -> "450"; "面" in fn || "饭" in fn -> "500"; "蛋" in fn -> "150"; "奶" in fn || "豆浆" in fn -> "120"; else -> "300" }
                                        protein = when { "鸡" in fn || "肉" in fn || "蛋" in fn -> "25"; else -> "12" }
                                        carbs = when { "面" in fn || "饭" in fn || "包" in fn -> "60"; else -> "25" }
                                        fat = when { "炸" in fn || "油" in fn -> "20"; else -> "8" }
                                        aiResult = "⚡ 离线估算"
                                    }
                                    aiLoading = false
                                }
                            }
                        },
                        enabled = foodName.isNotBlank() && !aiLoading,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(if (aiLoading) Icons.Filled.MoreVert else Icons.Filled.Send, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (aiLoading) "计算中..." else "🤖 AI 计算")
                    }
                }
                if (aiResult != null) {
                    Text(aiResult!!, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = calories, onValueChange = { calories = it.filter(digitFilter) },
                        label = { Text("热量(kcal)") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = protein, onValueChange = { protein = it.filter(digitFilter) },
                        label = { Text("蛋白(g)") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(12.dp))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = carbs, onValueChange = { carbs = it.filter(digitFilter) },
                        label = { Text("碳水(g)") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = fat, onValueChange = { fat = it.filter(digitFilter) },
                        label = { Text("脂肪(g)") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(12.dp))
                }
            }

            // Meal type
            item {
                Text("餐次", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("BREAKFAST" to "🥣 早餐", "LUNCH" to "🍱 午餐", "DINNER" to "🍽️ 晚餐", "SNACK" to "🍎 加餐")
                        .forEach { (type, label) ->
                            FilterChip(selected = selectedMeal == type, onClick = { selectedMeal = type },
                                label = { Text(label, style = MaterialTheme.typography.bodySmall) })
                        }
                }
            }

            // Save
            item {
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    val record = FoodRecordEntity(
                        foodName = foodName.ifBlank { "未命名食物" },
                        portion = portion.toDoubleOrNull() ?: 100.0,
                        portionUnit = selectedUnit.name,
                        calories = calories.toDoubleOrNull() ?: 0.0,
                        proteinGrams = protein.toDoubleOrNull() ?: 0.0,
                        carbsGrams = carbs.toDoubleOrNull() ?: 0.0,
                        fatGrams = fat.toDoubleOrNull() ?: 0.0,
                        mealType = selectedMeal,
                        recordDate = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        recordTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
                        source = "MANUAL"
                    )
                    onSaved(record)
                }, modifier = Modifier.fillMaxWidth().height(52.dp), enabled = foodName.isNotBlank(),
                    shape = RoundedCornerShape(26.dp)) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("保存记录", style = MaterialTheme.typography.bodyLarge)
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}
