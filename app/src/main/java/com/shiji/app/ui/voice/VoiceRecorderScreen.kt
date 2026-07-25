package com.shiji.app.ui.voice

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shiji.app.ui.components.LoadingState
import com.shiji.app.ui.foodconfirm.FoodConfirmContent
import com.shiji.app.ui.foodconfirm.FoodLogEntryViewModel
import com.shiji.core.voice.SpeechRecognizerManager
import com.shiji.core.voice.VoiceState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceRecorderScreen(
    onBack: () -> Unit = {},
    onSaved: () -> Unit = {},
    onNavigateToAiSettings: () -> Unit = {},
    viewModel: FoodLogEntryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val speechManager = remember { SpeechRecognizerManager(context) }

    // ---- permission ----
    var hasMicPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasMicPermission = granted }
    LaunchedEffect(Unit) {
        hasMicPermission = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    // ---- recording state (screen-owned; the ViewModel owns parse/save) ----
    var isRecording by remember { mutableStateOf(false) }
    var transcript by remember { mutableStateOf("") }
    var volumeLevel by remember { mutableFloatStateOf(0f) }
    var listenJob by remember { mutableStateOf<Job?>(null) }

    fun startListening() {
        if (!hasMicPermission) {
            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            return
        }
        transcript = ""
        volumeLevel = 0f
        isRecording = true
        listenJob?.cancel()
        listenJob = scope.launch {
            speechManager.listen().collect { state ->
                when (state) {
                    is VoiceState.Partial -> transcript = state.text
                    is VoiceState.Volume -> volumeLevel = state.level
                    is VoiceState.Final -> {
                        isRecording = false
                        transcript = state.text
                        if (state.text.isNotBlank()) viewModel.parse(state.text)
                    }
                    is VoiceState.Error -> {
                        isRecording = false
                        if (transcript.isBlank()) transcript = "⚠️ ${state.message}"
                    }
                    else -> Unit
                }
            }
        }
    }

    fun stopListening() {
        isRecording = false
        listenJob?.cancel()
        // If we already have a partial transcript, parse it directly.
        if (transcript.isNotBlank() && !transcript.startsWith("⚠️")) {
            viewModel.parse(transcript)
        }
    }

    // Navigate back once saved.
    LaunchedEffect(uiState.saved) {
        if (uiState.saved) { viewModel.reset(); onSaved() }
    }

    // Reset when leaving mid-flow.
    DisposableEffect(Unit) {
        onDispose { listenJob?.cancel(); viewModel.reset() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("语音记录", fontWeight = FontWeight.SemiBold) },
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
                    LoadingState(message = "AI 正在解析食物信息...")
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
                        onCancel = { viewModel.reset() },
                        onSave = { viewModel.save("VOICE") },
                        cancelLabel = "重录"
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
                    TextButton(onClick = { viewModel.reset() }) { Text("重新录音") }
                }
            }

            is FoodLogEntryViewModel.ParseState.Idle -> {
                // Main recording UI
                Column(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!uiState.aiConfigured) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("💡", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.width(8.dp))
                                Text("未配置 AI 时将使用离线估算",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f))
                                TextButton(onClick = onNavigateToAiSettings) { Text("配置 AI") }
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    // Transcript display
                    Text(
                        transcript.ifBlank { if (isRecording) "正在听..." else "按住下方按钮，说出你吃了什么" },
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (transcript.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(Modifier.height(48.dp))

                    // Record button with volume-driven ripple
                    Box(contentAlignment = Alignment.Center) {
                        if (isRecording) {
                            repeat(2) { i ->
                                Box(
                                    modifier = Modifier
                                        .size((100 + i * 28).dp)
                                        .scale(1f + volumeLevel * 0.5f)
                                        .clip(CircleShape)
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(
                                                alpha = (0.18f - i * 0.07f).coerceAtLeast(0.04f)
                                            )
                                        )
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isRecording) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.primary
                                )
                                .pointerInput(hasMicPermission) {
                                    detectTapGestures(
                                        onPress = {
                                            startListening()
                                            tryAwaitRelease()
                                            stopListening()
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Mic, "按住说话", tint = Color.White,
                                modifier = Modifier.size(40.dp))
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(if (isRecording) "松开发送" else "按住说话",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("例如：中午吃了一份宫保鸡丁盖饭，喝了杯奶茶",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center)

                    Spacer(Modifier.weight(1f))

                    if (!hasMicPermission) {
                        OutlinedButton(
                            onClick = { permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO) },
                            shape = RoundedCornerShape(26.dp)
                        ) { Text("授权麦克风") }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
