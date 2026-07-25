package com.shiji.app.ui.aisettings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shiji.core.ai.api.ProviderCatalog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(
    onBack: () -> Unit = {},
    viewModel: AiSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var configTarget by remember { mutableStateOf<ProviderCatalog.ProviderSpec?>(null) }
    var deleteTarget by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 模型配置", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // ---- current status ----
            item {
                StatusCard(
                    chatLabel = uiState.chatProviderId?.let {
                        "${viewModel.providerName(it)} · ${uiState.chatModel}"
                    },
                    visionLabel = uiState.visionProviderId?.let {
                        "${viewModel.providerName(it)} · ${uiState.visionModel}"
                    }
                )
            }

            // ---- provider catalog ----
            item {
                Text("选择 AI 提供商", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("使用你自己的 API Key，Key 加密存储在本机，不会上传任何服务器",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            items(viewModel.catalog) { spec ->
                val entity = uiState.configuredProviders.firstOrNull { it.id == spec.id }
                ProviderCard(
                    spec = spec,
                    configured = entity != null,
                    isChatSlot = uiState.chatProviderId == spec.id,
                    isVisionSlot = uiState.visionProviderId == spec.id,
                    onClick = { configTarget = spec }
                )
            }

            // ---- configured providers ----
            if (uiState.configuredProviders.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("已配置", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                }
                items(uiState.configuredProviders, key = { it.id }) { provider ->
                    ConfiguredProviderCard(
                        provider = provider,
                        isChatSlot = uiState.chatProviderId == provider.id,
                        isVisionSlot = uiState.visionProviderId == provider.id,
                        onSetChat = {
                            viewModel.setChatSlot(provider.id, provider.defaultChatModel ?: return@ConfiguredProviderCard)
                        },
                        onSetVision = {
                            viewModel.setVisionSlot(provider.id, provider.defaultVisionModel ?: return@ConfiguredProviderCard)
                        },
                        onEdit = { ProviderCatalog.byId(provider.id)?.let { configTarget = it } },
                        onDelete = { deleteTarget = provider.id }
                    )
                }
            }

            // ---- usage ----
            uiState.usage?.let { usage ->
                item {
                    Spacer(Modifier.height(8.dp))
                    UsageCard(usage)
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    // ---- configure dialog ----
    configTarget?.let { spec ->
        ConfigureProviderDialog(
            spec = spec,
            viewModel = viewModel,
            onDismiss = { configTarget = null }
        )
    }

    // ---- delete confirmation ----
    deleteTarget?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除该配置？") },
            text = { Text("将同时删除本机加密保存的 API Key，删除后相关 AI 功能不可用。") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteProvider(id); deleteTarget = null }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } }
        )
    }
}

// ==================== components ====================

@Composable
private fun StatusCard(chatLabel: String?, visionLabel: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("当前 AI 配置", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.height(10.dp))
            StatusRow("💬", "对话模型", chatLabel ?: "未配置 — AI 顾问/文字识别不可用")
            Spacer(Modifier.height(6.dp))
            StatusRow("👁️", "视觉模型", visionLabel ?: "未配置 — 拍照识食不可用")
        }
    }
}

@Composable
private fun StatusRow(emoji: String, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emoji)
        Spacer(Modifier.width(8.dp))
        Text("$label：", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun ProviderCard(
    spec: ProviderCatalog.ProviderSpec,
    configured: Boolean,
    isChatSlot: Boolean,
    isVisionSlot: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (configured) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(44.dp), shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant) {
                Box(contentAlignment = Alignment.Center) {
                    Text(spec.emoji, style = MaterialTheme.typography.titleLarge)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(spec.displayName, fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge)
                Text(spec.description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (configured) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (isChatSlot) SlotChip("对话")
                        if (isVisionSlot) SlotChip("视觉")
                    }
                }
            }
            if (configured) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Check, "已配置", tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp))
                    }
                }
            } else {
                Icon(Icons.Filled.ChevronRight, "配置", tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun SlotChip(label: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ConfiguredProviderCard(
    provider: com.shiji.core.data.entity.AiProviderEntity,
    isChatSlot: Boolean,
    isVisionSlot: Boolean,
    onSetChat: () -> Unit,
    onSetVision: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(provider.displayName, fontWeight = FontWeight.SemiBold)
                    Text(provider.defaultChatModel ?: "", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, "编辑", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.DeleteOutline, "删除", tint = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isChatSlot) {
                    SlotChip("✓ 当前对话模型")
                } else {
                    OutlinedButton(onClick = onSetChat, shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)) {
                        Text("设为对话模型", style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (provider.isVisionCapable && provider.defaultVisionModel != null) {
                    if (isVisionSlot) {
                        SlotChip("✓ 当前视觉模型")
                    } else {
                        OutlinedButton(onClick = onSetVision, shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)) {
                            Text("设为视觉模型", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UsageCard(usage: com.shiji.core.ai.usage.AiUsageTracker.UsageSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("📊 本月用量", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                UsageStat("${usage.totalCalls}", "调用次数")
                UsageStat(
                    if (usage.totalCalls > 0) "${usage.successCalls * 100 / usage.totalCalls}%" else "—",
                    "成功率"
                )
                UsageStat(formatTokens(usage.inputTokens + usage.outputTokens), "Token 消耗")
            }
        }
    }
}

@Composable
private fun UsageStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatTokens(tokens: Long): String = when {
    tokens >= 1_000_000 -> "%.1fM".format(tokens / 1_000_000.0)
    tokens >= 1_000 -> "%.1fK".format(tokens / 1_000.0)
    else -> tokens.toString()
}

// ==================== configure dialog ====================

@Composable
private fun ConfigureProviderDialog(
    spec: ProviderCatalog.ProviderSpec,
    viewModel: AiSettingsViewModel,
    onDismiss: () -> Unit
) {
    var apiKey by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var baseUrl by remember(spec) { mutableStateOf(spec.baseUrl) }
    var chatModel by remember(spec) { mutableStateOf(spec.defaultChatModel) }
    var visionModel by remember(spec) { mutableStateOf(spec.defaultVisionModel ?: "") }
    val testState by viewModel.testState.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = { viewModel.resetTestState(); onDismiss() },
        title = { Text("配置 ${spec.displayName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it; viewModel.resetTestState() },
                    label = { Text("API Key") },
                    placeholder = { Text(spec.keyHint) },
                    visualTransformation = if (showKey) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(if (showKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, "显示/隐藏")
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (spec.isCustom) {
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it; viewModel.resetTestState() },
                        label = { Text("接口地址 (Base URL)") },
                        placeholder = { Text("https://api.openai.com/v1") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                OutlinedTextField(
                    value = chatModel,
                    onValueChange = { chatModel = it },
                    label = { Text("对话模型") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (spec.isVisionCapable) {
                    OutlinedTextField(
                        value = visionModel,
                        onValueChange = { visionModel = it },
                        label = { Text("视觉模型（可选，用于拍照识食）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // connection test
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            viewModel.testConnection(
                                baseUrl = if (spec.isCustom) baseUrl else spec.baseUrl,
                                apiKey = apiKey,
                                model = chatModel.ifBlank { spec.defaultChatModel }
                            )
                        },
                        enabled = apiKey.isNotBlank() &&
                                (!spec.isCustom || baseUrl.isNotBlank()) &&
                                testState !is AiSettingsViewModel.TestState.Testing,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (testState is AiSettingsViewModel.TestState.Testing) "测试中..." else "🔌 测试连接")
                    }
                    when (val s = testState) {
                        is AiSettingsViewModel.TestState.Success ->
                            Text("✅ 连接成功", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary)
                        is AiSettingsViewModel.TestState.Failed ->
                            Text("❌ ${s.message.take(90)}", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error)
                        else -> Unit
                    }
                }

                Text("Key 仅加密存储在本机，不会上传到任何服务器。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.saveProvider(
                        spec = spec,
                        apiKey = apiKey,
                        baseUrl = baseUrl,
                        chatModel = chatModel,
                        visionModel = visionModel.ifBlank { null },
                        onDone = { viewModel.resetTestState(); onDismiss() }
                    )
                },
                enabled = apiKey.isNotBlank() && (!spec.isCustom || baseUrl.isNotBlank())
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.resetTestState(); onDismiss() }) { Text("取消") }
        }
    )
}
