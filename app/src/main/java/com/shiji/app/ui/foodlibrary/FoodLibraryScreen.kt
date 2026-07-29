package com.shiji.app.ui.foodlibrary

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
import com.shiji.core.data.entity.CachedFoodItemEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodLibraryScreen(
    foods: List<CachedFoodItemEntity> = emptyList(),
    onAddFood: (CachedFoodItemEntity) -> Unit = {},
    onDeleteFood: (Long) -> Unit = {},
    onSelectFood: (CachedFoodItemEntity) -> Unit = {},
    onBack: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    val filtered = remember(foods, searchQuery) {
        if (searchQuery.isBlank()) foods
        else foods.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("个人食物库", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it },
                placeholder = { Text("搜索食物...") }, modifier = Modifier.fillMaxWidth().padding(16.dp),
                leadingIcon = { Icon(Icons.Filled.Search, "搜索") },
                shape = RoundedCornerShape(12.dp), singleLine = true)

            if (foods.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🍽️", style = MaterialTheme.typography.displayLarge)
                        Spacer(Modifier.height(16.dp))
                        Text("还没有添加食物", style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("在记录确认页点击「加入食物库」即可添加", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.id }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onSelectFood(item) },
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("🍽️", style = MaterialTheme.typography.titleLarge)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(item.name, fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyLarge)
                                    val portionStr = if (item.defaultPortion % 1 == 0.0) item.defaultPortion.toInt().toString()
                                        else String.format("%.1f", item.defaultPortion)
                                    Text(
                                        "${portionStr}${item.defaultUnit} · ${item.caloriesPer100g.toInt()}kcal · 蛋白${item.proteinPer100g.toInt()}g · 碳水${item.carbsPer100g.toInt()}g · 脂肪${item.fatPer100g.toInt()}g",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onDeleteFood(item.id) }) {
                                    Icon(Icons.Filled.Delete, "删除", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }

    if (showAddDialog) {
        var newName by remember { mutableStateOf("") }
        var newPortion by remember { mutableStateOf("1") }
        var newUnit by remember { mutableStateOf("份") }
        var newCal by remember { mutableStateOf("") }
        var newProtein by remember { mutableStateOf("") }
        var newCarbs by remember { mutableStateOf("") }
        var newFat by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("添加食物") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = newName, onValueChange = { newName = it },
                        label = { Text("名称") }, singleLine = true, shape = RoundedCornerShape(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = newPortion,
                            onValueChange = { newPortion = it.filter { it.isDigit() || it == '.' } },
                            label = { Text("份量") }, modifier = Modifier.weight(1f), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(10.dp))
                        OutlinedTextField(value = newUnit, onValueChange = { newUnit = it },
                            label = { Text("单位") }, modifier = Modifier.weight(0.8f), singleLine = true,
                            shape = RoundedCornerShape(10.dp))
                    }
                    OutlinedTextField(value = newCal,
                        onValueChange = { newCal = it.filter { it.isDigit() || it == '.' } },
                        label = { Text("热量 (kcal)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(value = newProtein,
                            onValueChange = { newProtein = it.filter { it.isDigit() || it == '.' } },
                            label = { Text("蛋白g") }, modifier = Modifier.weight(1f), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(10.dp))
                        OutlinedTextField(value = newCarbs,
                            onValueChange = { newCarbs = it.filter { it.isDigit() || it == '.' } },
                            label = { Text("碳水g") }, modifier = Modifier.weight(1f), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(10.dp))
                        OutlinedTextField(value = newFat,
                            onValueChange = { newFat = it.filter { it.isDigit() || it == '.' } },
                            label = { Text("脂肪g") }, modifier = Modifier.weight(1f), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(10.dp))
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newName.isNotBlank()) {
                        onAddFood(CachedFoodItemEntity(
                            name = newName,
                            caloriesPer100g = newCal.toDoubleOrNull() ?: 0.0,
                            proteinPer100g = newProtein.toDoubleOrNull() ?: 0.0,
                            carbsPer100g = newCarbs.toDoubleOrNull() ?: 0.0,
                            fatPer100g = newFat.toDoubleOrNull() ?: 0.0,
                            defaultPortion = newPortion.toDoubleOrNull() ?: 1.0,
                            defaultUnit = newUnit.ifBlank { "份" }))
                        showAddDialog = false
                    }
                }) { Text("添加") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("取消") } }
        )
    }
}
