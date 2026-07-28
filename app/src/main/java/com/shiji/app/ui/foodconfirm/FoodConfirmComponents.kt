package com.shiji.app.ui.foodconfirm

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.shiji.app.ui.theme.BrandGreen

@Composable
fun FoodConfirmContent(
    items: List<EditableFoodItem>,
    selectedMealType: String,
    onSetMealType: (String) -> Unit,
    onUpdateItem: (String, (EditableFoodItem) -> EditableFoodItem) -> Unit,
    onRemoveItem: (String) -> Unit,
    onAddToLibrary: (EditableFoodItem) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    cancelLabel: String = "重录",
    header: (@Composable () -> Unit)? = null,
    libraryStatus: Map<String, Boolean> = emptyMap()
) {
    val totalCalories = items.sumOf { it.caloriesValue() }

    Column(modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("确认食物信息", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("AI 估算结果，可点击修改", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Text("${totalCalories.toInt()} kcal",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }

        header?.invoke()

        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items, key = { it.id }) { item ->
                EditableFoodCard(
                    item = item,
                    onChange = { onUpdateItem(item.id, it) },
                    onRemove = { onRemoveItem(item.id) },
                    onAddToLibrary = { onAddToLibrary(item) },
                    libraryAdded = libraryStatus[item.id] == true
                )
            }

            item {
                Spacer(Modifier.height(4.dp))
                Text("餐次", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MEAL_TYPE_OPTIONS.forEach { (type, label) ->
                        FilterChip(selected = selectedMealType == type,
                            onClick = { onSetMealType(type) }, label = { Text(label) })
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(26.dp)) { Text(cancelLabel) }
            Button(onClick = onSave, enabled = items.isNotEmpty(),
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(26.dp)) { Text("保存 (${items.size}项)") }
        }
    }
}

@Composable
fun EditableFoodCard(
    item: EditableFoodItem,
    onChange: ((EditableFoodItem) -> EditableFoodItem) -> Unit,
    onRemove: () -> Unit,
    onAddToLibrary: () -> Unit,
    libraryAdded: Boolean = false
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(12.dp)) {
            // Name + delete
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = item.name,
                    onValueChange = { v -> onChange { it.copy(name = v) } },
                    modifier = Modifier.weight(1f), singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    shape = RoundedCornerShape(10.dp))
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.DeleteOutline, "删除此项", tint = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(6.dp))
            // Portion / Unit / Calories
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = item.portion,
                    onValueChange = { v -> onChange { it.copy(portion = v) } },
                    modifier = Modifier.weight(0.8f), label = { Text("份量") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp))
                OutlinedTextField(value = item.portionUnit,
                    onValueChange = { v -> onChange { it.copy(portionUnit = v) } },
                    modifier = Modifier.weight(0.7f), label = { Text("单位") }, singleLine = true,
                    shape = RoundedCornerShape(10.dp))
                OutlinedTextField(value = item.calories,
                    onValueChange = { v -> onChange { it.copy(calories = v) } },
                    modifier = Modifier.weight(1f), label = { Text("kcal") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp))
            }
            Spacer(Modifier.height(6.dp))
            // Protein / Carbs / Fat
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = if (item.proteinGrams % 1 == 0.0) item.proteinGrams.toInt().toString() else String.format("%.1f", item.proteinGrams),
                    onValueChange = { v -> onChange { it.copy(proteinGrams = v.toDoubleOrNull() ?: item.proteinGrams) } },
                    modifier = Modifier.weight(1f), label = { Text("蛋白质 g") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp))
                OutlinedTextField(
                    value = if (item.carbsGrams % 1 == 0.0) item.carbsGrams.toInt().toString() else String.format("%.1f", item.carbsGrams),
                    onValueChange = { v -> onChange { it.copy(carbsGrams = v.toDoubleOrNull() ?: item.carbsGrams) } },
                    modifier = Modifier.weight(1f), label = { Text("碳水 g") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp))
                OutlinedTextField(
                    value = if (item.fatGrams % 1 == 0.0) item.fatGrams.toInt().toString() else String.format("%.1f", item.fatGrams),
                    onValueChange = { v -> onChange { it.copy(fatGrams = v.toDoubleOrNull() ?: item.fatGrams) } },
                    modifier = Modifier.weight(1f), label = { Text("脂肪 g") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp))
            }
            // Add to library
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onAddToLibrary, enabled = !libraryAdded) {
                    Icon(if (libraryAdded) Icons.Filled.Check else Icons.Filled.Add,
                        null, modifier = Modifier.size(16.dp),
                        tint = if (libraryAdded) BrandGreen else MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(if (libraryAdded) "已加入食物库" else "加入食物库",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (libraryAdded) BrandGreen else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (item.confidence < 0.7f) {
                Text("⚠️ AI 对此项不太确定，请核对",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}
