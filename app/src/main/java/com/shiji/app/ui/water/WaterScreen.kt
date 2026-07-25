package com.shiji.app.ui.water

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.sp
import com.shiji.app.ui.theme.Fat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterScreen(onBack: () -> Unit = {}) {
    var waterMl by remember { mutableIntStateOf(0) }
    val goalMl = 2000

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("水分摄入", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Water progress
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Fat),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("💧", style = MaterialTheme.typography.displayLarge)
                        Spacer(Modifier.height(8.dp))
                        Text("$waterMl", fontFamily = FontFamily.Monospace,
                            fontSize = 48.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
                        Text("/ ${goalMl}ml", color = androidx.compose.ui.graphics.Color.White.copy(0.7f),
                            style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (waterMl.toFloat() / goalMl).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = androidx.compose.ui.graphics.Color.White,
                            trackColor = androidx.compose.ui.graphics.Color.White.copy(0.3f)
                        )
                    }
                }
            }

            // Quick add buttons
            item {
                Text("快速加水", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth())
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(100 to "100ml", 200 to "200ml", 300 to "300ml", 500 to "500ml").forEach { (ml, label) ->
                        OutlinedButton(
                            onClick = { waterMl = (waterMl + ml).coerceAtMost(goalMl + 1000) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text(label) }
                    }
                }
            }

            // Custom add
            item {
                var customMl by remember { mutableStateOf("250") }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customMl, onValueChange = { customMl = it.filter { c -> c.isDigit() } },
                        label = { Text("自定义 (ml)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Button(
                        onClick = { waterMl += (customMl.toIntOrNull() ?: 0) },
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("添加") }
                }
            }

            item { HorizontalDivider() }

            // Tip
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("💡", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.width(12.dp))
                        Text("每天建议饮水 2000ml，运动后适当增加",
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}
