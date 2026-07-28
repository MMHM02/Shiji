package com.shiji.app.ui.export

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataExportScreen(
    onBack: () -> Unit = {},
    viewModel: DataExportViewModel = hiltViewModel()
) {
    val screenState by viewModel.state.collectAsStateWithLifecycle()
    val uiState = screenState.uiState
    var showExportConfirm by remember { mutableStateOf(false) }
    var showImportConfirm by remember { mutableStateOf(false) }
    val hasBackup = screenState.existingBackup != null

    // Dismiss dialogs when state changes
    LaunchedEffect(uiState) {
        if (uiState is DataExportViewModel.UiState.ExportDone ||
            uiState is DataExportViewModel.UiState.Error ||
            uiState is DataExportViewModel.UiState.ImportDone) {
            showExportConfirm = false
            showImportConfirm = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("数据管理", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Export ──
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Upload, "导出", tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("📤 导出数据", fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleMedium)
                            Text("将所有数据导出为 .fitness 文件", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { showExportConfirm = true },
                        enabled = uiState !is DataExportViewModel.UiState.Exporting,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (uiState is DataExportViewModel.UiState.Exporting) "导出中..." else "导出 .fitness 文件")
                    }
                    when (uiState) {
                        is DataExportViewModel.UiState.ExportDone -> {
                            Spacer(Modifier.height(8.dp))
                            Text("✅ 已导出 ${uiState.recordCount} 条记录",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            Text(uiState.filePath, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        is DataExportViewModel.UiState.Error -> {
                            Spacer(Modifier.height(8.dp))
                            Text("❌ ${uiState.message}", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error)
                        }
                        else -> {}
                    }
                }
            }

            // ── Import ──
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Download, "导入", tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("📥 导入数据", fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleMedium)
                            Text("从备份恢复数据（将覆盖当前数据）",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (hasBackup) {
                        Spacer(Modifier.height(8.dp))
                        Text("📁 已有备份: ${screenState.existingBackup!!.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { viewModel.refreshBackupList(); showImportConfirm = true },
                        enabled = uiState !is DataExportViewModel.UiState.Importing,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (uiState is DataExportViewModel.UiState.Importing) "导入中..." else "导入备份")
                    }
                    when (uiState) {
                        is DataExportViewModel.UiState.ImportDone -> {
                            Spacer(Modifier.height(8.dp))
                            Text("✅ 已恢复 ${uiState.recordCount} 条记录",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        is DataExportViewModel.UiState.Error -> {
                            Spacer(Modifier.height(8.dp))
                            Text("❌ ${uiState.message}", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error)
                        }
                        else -> {}
                    }
                }
            }

            // ── Rules ──
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📋 规则说明", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text("• 备份文件存储在「下载/ShiJi/」目录，任何文件管理器都能找到",
                        style = MaterialTheme.typography.bodySmall)
                    Text("• 文件夹内只保留一个 .fitness 文件，导出时自动删除旧备份",
                        style = MaterialTheme.typography.bodySmall)
                    Text("• 导入时自动读取该目录下的 .fitness 文件",
                        style = MaterialTheme.typography.bodySmall)
                    Text("• .fitness 文件是标准 JSON，不含照片，人可阅读",
                        style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Text("📁 备份文件夹（文件管理器可直接打开）:",
                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
                    Text(screenState.backupFolder,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    // Export confirm
    if (showExportConfirm) {
        AlertDialog(
            onDismissRequest = { showExportConfirm = false },
            title = { Text("确认导出") },
            text = { Text("将导出所有饮食记录、健康指标、食物库和目标设定。\n导出后旧备份文件将被删除。") },
            confirmButton = {
                Button(onClick = { viewModel.exportData() }) { Text("导出") }
            },
            dismissButton = { TextButton(onClick = { showExportConfirm = false }) { Text("取消") } }
        )
    }

    // Import confirm
    if (showImportConfirm) {
        val hasFile = screenState.existingBackup != null
        AlertDialog(
            onDismissRequest = { showImportConfirm = false },
            title = { Text("确认导入") },
            text = {
                if (hasFile) {
                    Text("将用「${screenState.existingBackup!!.name}」覆盖当前所有数据。建议先导出一份备份。")
                } else {
                    Text("未找到 .fitness 文件。\n\n请确保备份文件已放入：\n${screenState.backupFolder}\n\n且文件扩展名为 .fitness")
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.importData() },
                    enabled = hasFile
                ) { Text(if (hasFile) "导入" else "无备份文件") }
            },
            dismissButton = { TextButton(onClick = { showImportConfirm = false }) { Text("取消") } }
        )
    }
}
