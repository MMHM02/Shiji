package com.shiji.app.ui.textrecord

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shiji.app.ui.components.LoadingState
import com.shiji.app.ui.foodconfirm.FoodConfirmContent
import com.shiji.app.ui.foodconfirm.FoodLogEntryViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextRecordScreen(
    onBack: () -> Unit = {},
    onSaved: () -> Unit = {},
    selectedDate: LocalDate = LocalDate.now(),
    onNavigateToAiSettings: () -> Unit = {},
    viewModel: FoodLogEntryViewModel = hiltViewModel()
) {
    LaunchedEffect(selectedDate) { viewModel.selectedDate = selectedDate }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) { viewModel.reset(); onSaved() }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.reset() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("文字记录", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        when (val state = uiState.parseState) {
            is FoodLogEntryViewModel.ParseState.Parsing -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Text(uiState.sourceText, modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge)
                    }
                    Spacer(Modifier.height(16.dp))
                    LoadingState(message = "AI 正在识别食物...")
                    TextButton(onClick = { viewModel.cancelParse() }) { Text("取消") }
                }
            }

            is FoodLogEntryViewModel.ParseState.Parsed,
            is FoodLogEntryViewModel.ParseState.OfflineEstimated -> {
                Column(Modifier.fillMaxSize().padding(innerPadding)) {
                    if (state is FoodLogEntryViewModel.ParseState.OfflineEstimated) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📴", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("AI 暂不可用，已离线粗略估算",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold)
                                    Text("配置 AI 后可获得精准识别",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                TextButton(onClick = onNavigateToAiSettings) { Text("去配置") }
                            }
                        }
                    }
                    FoodConfirmContent(
                        items = uiState.items,
                        selectedMealType = uiState.selectedMealType,
                        onSetMealType = viewModel::setMealType,
                        onUpdateItem = viewModel::updateItem,
                        onRemoveItem = viewModel::removeItem,
                        onAddToLibrary = { viewModel.addToLibrary(it) },
                        onCancel = { viewModel.reset() },
                        onSave = { viewModel.save("TEXT") },
                        cancelLabel = "重新输入",
                        libraryStatus = viewModel.libraryAdded.collectAsStateWithLifecycle().value
                    )
                }
            }

            is FoodLogEntryViewModel.ParseState.Failed -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("😵", style = MaterialTheme.typography.displayMedium)
                    Spacer(Modifier.height(16.dp))
                    Text("解析失败", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text(state.message, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { viewModel.parse(uiState.sourceText) },
                        shape = RoundedCornerShape(26.dp)) { Text("重试") }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { viewModel.reset() }) { Text("重新输入") }
                }
            }

            is FoodLogEntryViewModel.ParseState.Idle -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(8.dp))
                    Text("📝", style = MaterialTheme.typography.displayMedium)
                    Spacer(Modifier.height(12.dp))
                    Text("告诉我你吃了什么", style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold)
                    Text("AI 会自动估算热量和营养素", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("例如：中午吃了一碗牛肉面，加了卤蛋和豆皮，还喝了一杯豆浆...") },
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        minLines = 5
                    )

                    Spacer(Modifier.height(12.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            if (uiState.aiConfigured)
                                "💡 不需要精确克重，描述吃了什么和大概份量就行，AI 会智能估算。"
                            else
                                "💡 未配置 AI，将使用离线粗略估算。配置后可获得精准识别。",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.parse(inputText) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = inputText.isNotBlank(),
                        shape = RoundedCornerShape(26.dp)
                    ) {
                        Text(if (uiState.aiConfigured) "确认，让 AI 识别" else "离线估算",
                            style = MaterialTheme.typography.bodyLarge)
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}
