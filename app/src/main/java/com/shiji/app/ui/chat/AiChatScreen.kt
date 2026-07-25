package com.shiji.app.ui.chat

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shiji.core.ai.api.ChatRole
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    initialPrompt: String = "",
    userName: String = "Shawn",
    onBack: () -> Unit = {},
    onNavigateToAiSettings: () -> Unit = {},
    viewModel: AiChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    var showClearDialog by remember { mutableStateOf(false) }

    // Auto-send the initial prompt exactly once.
    LaunchedEffect(initialPrompt) {
        if (initialPrompt.isNotBlank()) viewModel.sendMessage(initialPrompt, userName)
    }

    // Keep the list pinned to the latest content while streaming.
    LaunchedEffect(uiState.messages.lastOrNull()?.content?.length, uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AI 营养顾问", fontWeight = FontWeight.SemiBold)
                        if (uiState.aiConfigured) {
                            Text(uiState.modelLabel, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "返回") } },
                actions = {
                    if (uiState.messages.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Filled.DeleteOutline, "清空对话",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            if (!uiState.aiConfigured) {
                AiNotConfiguredCard(onNavigateToAiSettings)
            }

            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                if (uiState.messages.isEmpty()) {
                    item {
                        WelcomeCard(
                            suggestions = viewModel.suggestedQueries,
                            enabled = uiState.aiConfigured && !uiState.isAiTyping,
                            onPick = { viewModel.sendMessage(it, userName) }
                        )
                    }
                }

                items(uiState.messages, key = { it.id }) { msg ->
                    ChatBubbleRow(
                        msg = msg,
                        onCopy = { clipboard.setText(AnnotatedString(msg.content)) },
                        onRetry = { viewModel.retryLast(userName) }
                    )
                }
            }

            // Input bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(if (uiState.aiConfigured) "输入你的问题..." else "配置 AI 后即可提问")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        enabled = uiState.aiConfigured && !uiState.isAiTyping,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            viewModel.sendMessage(inputText, userName); inputText = ""
                        })
                    )
                    Spacer(Modifier.width(8.dp))
                    if (uiState.isAiTyping) {
                        FilledIconButton(
                            onClick = { viewModel.stopGenerating() },
                            modifier = Modifier.size(48.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) { Icon(Icons.Filled.Stop, "停止生成") }
                    } else {
                        FilledIconButton(
                            onClick = { viewModel.sendMessage(inputText, userName); inputText = "" },
                            enabled = inputText.isNotBlank() && uiState.aiConfigured,
                            modifier = Modifier.size(48.dp)
                        ) { Icon(Icons.AutoMirrored.Filled.Send, "发送") }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空对话记录？") },
            text = { Text("当前对话内容将被清除，此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearHistory(); showClearDialog = false }) {
                    Text("清空", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("取消") } }
        )
    }
}

// ==================== pieces ====================

@Composable
private fun AiNotConfiguredCard(onNavigateToAiSettings: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🔑", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("尚未配置 AI 模型", fontWeight = FontWeight.SemiBold)
                Text("配置你自己的 API Key，即可拥有私人 AI 营养顾问",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onNavigateToAiSettings) { Text("去配置") }
        }
    }
}

@Composable
private fun WelcomeCard(
    suggestions: List<String>,
    enabled: Boolean,
    onPick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🤖", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(12.dp))
        Text("我是你的私人营养顾问", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text("我会结合你的饮食记录和目标来回答", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        suggestions.forEach { q ->
            SuggestionChip(
                onClick = { onPick(q) },
                label = { Text(q) },
                enabled = enabled,
                modifier = Modifier.padding(vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun ChatBubbleRow(
    msg: AiChatViewModel.ChatMessageUi,
    onCopy: () -> Unit,
    onRetry: () -> Unit
) {
    val isUser = msg.role == ChatRole.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Surface(modifier = Modifier.size(32.dp), shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer) {
                Box(contentAlignment = Alignment.Center) { Text("🤖", style = MaterialTheme.typography.bodySmall) }
            }
            Spacer(Modifier.width(8.dp))
        }

        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            Surface(
                modifier = Modifier.widthIn(max = 300.dp),
                shape = if (isUser) RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
                        else RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
                color = when {
                    msg.isError -> MaterialTheme.colorScheme.errorContainer
                    isUser -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        msg.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            msg.isError -> MaterialTheme.colorScheme.onErrorContainer
                            isUser -> MaterialTheme.colorScheme.onPrimary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    if (msg.isStreaming) StreamingCursor()
                }
            }

            // Actions under AI bubbles
            if (!isUser && !msg.isStreaming) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.alpha(0.75f)
                ) {
                    if (msg.isError) {
                        TextButton(onClick = onRetry, contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("重试", style = MaterialTheme.typography.labelSmall)
                        }
                    } else {
                        Text(
                            "复制",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clickable(onClick = onCopy)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        if (isUser) {
            Spacer(Modifier.width(8.dp))
            Surface(modifier = Modifier.size(32.dp), shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.secondaryContainer) {
                Box(contentAlignment = Alignment.Center) { Text("👤", style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
private fun StreamingCursor() {
    val infinite = rememberInfiniteTransition(label = "cursor")
    val alpha by infinite.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "cursorAlpha"
    )
    Text(" ▍", color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
        style = MaterialTheme.typography.bodyMedium)
}
