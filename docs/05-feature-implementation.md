# ⚙️ 05 — 功能实现方案文档

> 版本：v1.0 | 日期：2026-06-25 | 状态：初稿

---

## 1. 文档说明

本文档对每个核心功能模块给出实现方案。每个功能按以下结构组织：

```
功能 → 技术方案 → 关键代码设计 → 状态管理 → 测试要点
```

---

## 2. 模块一：拍照识食

### 2.1 功能入口

- **首页快捷按钮**（主要入口）
- **饮食页 FAB**
- **饮食日志页"+"按钮**

### 2.2 Composable 组件树

```
CameraScreen
├── CameraPreview (CameraX + PreviewView)
│   ├── CameraOverlay (网格线、提示文字)
│   └── FlashToggle (闪光灯开关)
├── BottomControlBar
│   ├── GalleryButton (相册入口)
│   ├── CaptureButton (大圆形拍照按钮)
│   └── SwitchCameraButton (前后摄切换)
│
├── ImageReviewScreen (图片预览)
│   ├── ImagePreview
│   ├── RotateButton
│   └── ActionBar [重拍] [使用照片]
│
├── AnalysisLoadingScreen (分析中)
│   ├── PulsingFoodAnimation (食物图标脉冲动画)
│   ├── ProgressHint ("AI 正在识别食物...")
│   └── CancelButton
│
└── ResultConfirmationScreen (结果确认)
    ├── FoodThumbnail (食物缩略图)
    ├── FoodItemList (识别结果列表)
    │   └── FoodItemCard * N
    │       ├── FoodNameField (可编辑)
    │       ├── PortionStepper (份量 +/-)
    │       ├── CaloriesDisplay
    │       └── MacroNutrientBars (蛋白质/碳水/脂肪条)
    ├── MealTypeSelector (早餐/午餐/晚餐/加餐)
    ├── DatePicker
    └── SaveButton / CancelButton
```

### 2.3 关键代码：CameraScreen Composable

```kotlin
// ui/diet/camera/CameraScreen.kt

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onImageCaptured: (Uri) -> Unit,
    onGalleryClick: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 权限请求
    val cameraPermission = rememberPermission(android.Manifest.permission.CAMERA)

    if (cameraPermission.status.isGranted) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 相机预览
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { previewView ->
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            val imageCapture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                                .build()

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner, cameraSelector, preview, imageCapture
                            )

                            viewModel.onCameraReady(imageCapture)
                        }, ContextCompat.getMainExecutor(ctx))
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // 顶部控制
            CameraTopBar(
                onBackClick = onBackClick,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // 底部控制栏
            CameraBottomBar(
                onCapture = { viewModel.capturePhoto(context) },
                onGalleryClick = onGalleryClick,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    } else {
        // 权限未授权：显示说明 + 请求按钮
        CameraPermissionScreen(
            onRequestPermission = { cameraPermission.launchPermissionRequest() },
            onBackClick = onBackClick
        )
    }
}
```

### 2.4 关键代码：CameraViewModel

```kotlin
// ui/diet/camera/CameraViewModel.kt

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val analyzeFoodPhoto: AnalyzeFoodPhotoUseCase,
    private val saveFoodRecord: SaveFoodRecordUseCase
) : ViewModel() {

    data class CameraUiState(
        val isCameraReady: Boolean = false,
        val capturedImageUri: Uri? = null,
        val analysisState: AnalysisState = AnalysisState.Idle,
        val analysisResult: FoodAnalysisResult? = null,
        val selectedMealType: MealType = MealType.LUNCH,
        val error: String? = null
    )

    sealed interface AnalysisState {
        data object Idle : AnalysisState
        data object Capturing : AnalysisState
        data object Analyzing : AnalysisState
        data object Success : AnalysisState
        data class Failed(val reason: String) : AnalysisState
    }

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private var imageCapture: ImageCapture? = null

    fun onCameraReady(capture: ImageCapture) {
        imageCapture = capture
        _uiState.update { it.copy(isCameraReady = true) }
    }

    fun capturePhoto(context: Context) {
        val capture = imageCapture ?: return
        val photoFile = File(context.cacheDir, "food_${System.currentTimeMillis()}.jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    _uiState.update {
                        it.copy(
                            capturedImageUri = Uri.fromFile(photoFile),
                            analysisState = AnalysisState.Capturing
                        )
                    }
                }
                override fun onError(exc: ImageCaptureException) {
                    _uiState.update {
                        it.copy(error = "拍照失败: ${exc.message}")
                    }
                }
            }
        )
    }

    fun analyzeImage(context: Context) {
        val uri = _uiState.value.capturedImageUri ?: return
        _uiState.update { it.copy(analysisState = AnalysisState.Analyzing) }

        viewModelScope.launch {
            analyzeFoodPhoto(uri, context)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            analysisResult = result,
                            analysisState = AnalysisState.Success
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            analysisState = AnalysisState.Failed(error.message ?: "分析失败"),
                            error = error.message
                        )
                    }
                }
        }
    }

    fun confirmAndSave() {
        val result = _uiState.value.analysisResult ?: return
        viewModelScope.launch {
            result.items.forEach { item ->
                saveFoodRecord(
                    FoodRecord(
                        id = 0,
                        mealType = _uiState.value.selectedMealType,
                        dateTime = LocalDateTime.now(),
                        food = FoodItem(
                            name = item.name,
                            portion = Portion(item.portion, PortionUnit.valueOf(item.portionUnit.uppercase())),
                            nutrition = NutritionInfo(
                                item.calories, item.proteinGrams,
                                item.carbsGrams, item.fatGrams
                            )
                        ),
                        source = RecordSource.CAMERA,
                        imageUri = _uiState.value.capturedImageUri?.toString(),
                        aiModel = null,
                        note = null
                    )
                )
            }
            _uiState.update { it.copy(analysisState = AnalysisState.Idle) }
        }
    }
}
```

### 2.5 测试要点

| 测试场景 | 测试方法 |
|----------|----------|
| 权限拒绝 → 显示权限说明页 | Compose Test |
| 拍照成功 → 图片预览显示 | Compose Test + 模拟 CameraX |
| 图片预处理 → 尺寸 ≤ 2048px, 大小 ≤ 2MB | Unit Test |
| AI 返回正常 JSON → 解析为 Result | Unit Test (Mock AiService) |
| AI 返回非食物 → 提示用户 | Unit Test |
| AI 网络超时 → 显示错误 + 降级选项 | Integration Test |
| 用户编辑食物项 → 数据更新 | Compose Test |
| 确认保存 → Room 写入 → UI 更新 | Instrumented Test |

---

## 3. 模块二：语音口述记录

### 3.1 Composable 组件树

```
VoiceRecorderScreen
├── TopBar (取消按钮)
├── CentralArea
│   ├── WaveformAnimation (声波动画，随音量变化)
│   ├── RecordHintText ("按住说话，松开发送")
│   │   状态变化：准备录音 → 正在听... → 识别中... → 完成
│   ├── LiveTranscriptText (实时 ASR 文字上屏)
│   └── CancelZone (上滑取消区域)
│
├── RecordButton
│   ├── 按下：开始录音
│   ├── 松开：停止录音，触发 AI 解析
│   └── 上滑：取消录音
│
└── ResultConfirmationScreen (同拍照模块，复用)
```

### 3.2 关键代码：VoiceRecorderViewModel

```kotlin
// ui/diet/voice/VoiceRecorderViewModel.kt

@HiltViewModel
class VoiceRecorderViewModel @Inject constructor(
    private val speechRecognizer: SpeechRecognizerWrapper,
    private val parseFoodDescription: ParseFoodDescriptionUseCase
) : ViewModel() {

    data class VoiceUiState(
        val isRecording: Boolean = false,
        val voiceState: VoiceRecognitionState = VoiceRecognitionState.Ready,
        val transcript: String = "",
        val parsedResult: FoodAnalysisResult? = null,
        val isParsing: Boolean = false,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(VoiceUiState())
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    fun startRecording() {
        _uiState.update { it.copy(isRecording = true, transcript = "", error = null) }

        viewModelScope.launch {
            speechRecognizer.startListening().collect { state ->
                _uiState.update { current ->
                    when (state) {
                        is VoiceRecognitionState.PartialResult ->
                            current.copy(transcript = state.text)
                        is VoiceRecognitionState.FinalResult -> {
                            current.copy(
                                isRecording = false,
                                transcript = state.text,
                                isParsing = true
                            ).also { parseAndAnalyze(state.text) }
                        }
                        is VoiceRecognitionState.Error ->
                            current.copy(isRecording = false, error = state.message)
                        else -> current.copy(voiceState = state)
                    }
                }
            }
        }
    }

    fun stopRecording() {
        speechRecognizer.stopListening()
        _uiState.update { it.copy(isRecording = false) }
    }

    fun cancelRecording() {
        speechRecognizer.cancel()
        _uiState.update {
            it.copy(isRecording = false, transcript = "", error = null)
        }
    }

    private fun parseAndAnalyze(text: String) {
        viewModelScope.launch {
            parseFoodDescription(text)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(parsedResult = result, isParsing = false)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(error = error.message, isParsing = false)
                    }
                }
        }
    }
}
```

### 3.3 语音录制 UX 细节

```kotlin
// ui/diet/voice/VoiceRecorderScreen.kt (关键交互部分)

@Composable
fun RecordButton(
    isRecording: Boolean,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit,
    onSwipeUpCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(0f) }

    val cancelThreshold = -150f // 上滑 > 150px 触发取消

    Box(
        modifier = modifier
            .size(80.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isPressed = true; onPressStart() },
                    onDragEnd = {
                        isPressed = false
                        if (dragOffset < cancelThreshold) {
                            onSwipeUpCancel()
                        } else {
                            onPressEnd()
                        }
                        dragOffset = 0f
                    },
                    onDragCancel = { isPressed = false; dragOffset = 0f },
                    onDrag = { change, offset ->
                        change.consume()
                        dragOffset += offset.y
                    }
                )
            }
            .graphicsLayer {
                // 上滑时按钮缩小 + 透明度降低
                val scale = if (dragOffset < 0) {
                    (1f + dragOffset / 300f).coerceIn(0.5f, 1f)
                } else 1f
                scaleX = scale
                scaleY = scale
                alpha = scale
            }
            .background(
                color = if (isPressed) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(
                if (isRecording) R.drawable.ic_mic_filled else R.drawable.ic_mic
            ),
            contentDescription = if (isRecording) "松开停止" else "按住说话",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(36.dp)
        )
    }
}
```

---

## 4. 模块三：饮食日志与营养分析

### 4.1 饮食日志页面

```
DietLogScreen
├── DateSelector (水平滑动日期选择器，默认今天)
│   └── DateChip * 7 (最近 7 天)
├── DailySummaryCard
│   ├── CaloriesRing (环形图：已摄入/目标)
│   ├── MacroBars (蛋白质/碳水/脂肪 进度条)
│   └── RemainingCalories ("还可摄入 XXX kcal")
├── MealSection * 4 (早餐/午餐/晚餐/加餐)
│   ├── MealHeader
│   │   ├── MealIcon
│   │   ├── MealTitle
│   │   └── MealCaloriesSum
│   └── FoodRecordItem * N
│       ├── FoodIcon (或缩略图)
│       ├── FoodName
│       ├── PortionText
│       ├── CaloriesBadge
│       └── SwipeToDelete (滑动删除)
└── AddMealFAB
```

### 4.2 营养分析页面

```
DietAnalysisScreen
├── PeriodSelector (周/月/自定义)
├── CaloriesTrendChart (折线图)
├── MacroPieChart (宏量营养素饼图)
├── MacroDailyTrendChart (堆叠柱状图)
├── MealDistributionCard (各餐次热量分布)
├── FrequentFoodsCard (常吃食物 TOP5)
├── AiInsightCard (AI 饮食洞察)
│   └── "本周蛋白质摄入偏低，建议增加..."
└── ExportButton (导出报告)
```

### 4.3 图表实现（使用 Vico）

```kotlin
// ui/diet/analysis/DietAnalysisScreen.kt (图表部分)

@Composable
fun CaloriesTrendChart(
    dailyData: List<DailyCalories>,
    goalCalories: Double,
    modifier: Modifier = Modifier
) {
    val goalLineColor = MaterialTheme.colorScheme.error
    val actualLineColor = MaterialTheme.colorScheme.primary

    Card(modifier = modifier.padding(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("热量摄入趋势", style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(12.dp))

            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(),
                    rememberLineCartesianLayer(listOf(LineCartesianLayerComponentEntry(goalLineColor)))
                ),
                model = rememberCartesianChartModel(
                    dailyData.map { it.calories },
                    List(dailyData.size) { goalCalories } // 目标线
                ),
                modifier = Modifier.fillMaxWidth().height(200.dp)
            )

            // 图例
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendItem(color = actualLineColor, label = "实际摄入")
                LegendItem(color = goalLineColor, label = "目标 (${goalCalories.toInt()} kcal)")
            }
        }
    }
}
```

---

## 5. 模块四：AI 顾问对话

### 5.1 对话页面架构

```
AiChatScreen
├── ChatTopBar
│   ├── BackButton
│   ├── Title ("AI 营养顾问")
│   └── ModelIndicator ("GPT-4o-mini ▼")
├── ChatMessageList (LazyColumn, 倒序)
│   ├── WelcomeCard (首次进入)
│   │   ├── Greeting ("你好！我是你的私人营养顾问")
│   │   └── SuggestedQueries * 4 (推荐问题按钮)
│   ├── UserMessageBubble * N
│   │   └── 右对齐, 主色背景, 白字
│   ├── AiMessageBubble * N
│   │   ├── 左对齐, 灰色背景
│   │   ├── MarkdownText (支持 Markdown 渲染)
│   │   └── ActionButtons (复制/重新生成)
│   └── TypingIndicator (正在输入动画)
├── ChatInputBar
│   ├── TextField
│   ├── AttachImageButton (可选：附加图片)
│   └── SendButton
└── ClearHistoryDialog
```

### 5.2 对话状态管理

```kotlin
// ui/ai/AiChatViewModel.kt

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val chatWithAi: ChatWithAiUseCase,
    private val aiServiceManager: AiServiceManager,
    private val usageTracker: AiUsageTracker
) : ViewModel() {

    data class ChatUiState(
        val messages: List<ChatMessageUi> = emptyList(),
        val isAiTyping: Boolean = false,
        val inputText: String = "",
        val showWelcome: Boolean = true,
        val error: String? = null,
        val currentModel: String = "",
        val suggestedQueries: List<String> = listOf(
            "分析我今天的热量摄入",
            "本周饮食有什么问题？",
            "推荐适合减脂的晚餐",
            "如何增加蛋白质摄入？"
        )
    )

    data class ChatMessageUi(
        val id: String = UUID.randomUUID().toString(),
        val role: ChatRole,
        val content: String,
        val timestamp: Long = System.currentTimeMillis(),
        val isStreaming: Boolean = false
    )

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMessage = ChatMessageUi(role = ChatRole.USER, content = text.trim())
        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                inputText = "",
                showWelcome = false,
                isAiTyping = true
            )
        }

        viewModelScope.launch {
            val chatHistory = _uiState.value.messages.map { msg ->
                ChatMessage(role = msg.role, content = msg.content)
            }

            chatWithAi(text.trim(), chatHistory)
                .onSuccess { response ->
                    val aiMessage = ChatMessageUi(role = ChatRole.ASSISTANT, content = response)
                    _uiState.update {
                        it.copy(
                            messages = it.messages + aiMessage,
                            isAiTyping = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            error = error.message ?: "对话失败",
                            isAiTyping = false
                        )
                    }
                }
        }
    }

    fun clearHistory() {
        _uiState.update { it.copy(messages = emptyList(), showWelcome = true) }
    }

    fun retryLast() {
        val lastUserMessage = _uiState.value.messages.lastOrNull { it.role == ChatRole.USER }
        if (lastUserMessage != null) {
            // 移除最后的 AI 回复
            _uiState.update { it.copy(messages = it.messages.dropLast(1)) }
            sendMessage(lastUserMessage.content)
        }
    }

    init {
        viewModelScope.launch {
            val service = aiServiceManager.getChatService()
            _uiState.update { it.copy(currentModel = service?.modelId ?: "未配置") }
        }
    }
}
```

### 5.3 Markdown 渲染与代码高亮

```kotlin
// 使用 compose-markdown 或自定义解析
// 注意：需要添加 `io.github.yahiaangelo:compose-markdown:1.0.2` 依赖

@Composable
fun AiMessageBubble(
    message: ChatMessageUi,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(0.85f)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 使用 Markdown 渲染
            MarkdownText(
                content = message.content,
                style = MarkdownStyle(
                    textStyle = MaterialTheme.typography.bodyMedium,
                    codeStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        background = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 操作按钮行
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { /* 复制 */ }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.ContentCopy, "复制", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = { /* 重新生成 */ }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Refresh, "重新生成", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
```

---

## 6. 模块五：首页仪表盘

### 6.1 数据加载策略

```kotlin
// ui/home/HomeViewModel.kt

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getDailyNutrition: GetDailyNutritionUseCase,
    private val getRecentRecords: GetRecentRecordsUseCase,
    private val getWeightHistory: GetWeightHistoryUseCase,
    private val checkAiStatus: CheckAiStatusUseCase
) : ViewModel() {

    data class HomeUiState(
        val todayNutrition: DailyNutrition? = null,
        val recentRecords: List<FoodRecord> = emptyList(),
        val weightTrend: List<WeightRecord> = emptyList(),
        val isAiOnline: Boolean = false,
        val isLoading: Boolean = true,
        val refreshError: String? = null,
        val currentGreeting: String = getTimeBasedGreeting()
    )

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        val today = LocalDate.now()
        val weekAgo = today.minusDays(7)

        // 并行加载数据
        viewModelScope.launch {
            launch { getDailyNutrition(today).collect { _uiState.update { s -> s.copy(todayNutrition = it) } } }
            launch { getRecentRecords(today).collect { _uiState.update { s -> s.copy(recentRecords = it) } } }
            launch { getWeightHistory(weekAgo, today).collect { _uiState.update { s -> s.copy(weightTrend = it) } } }
        }

        // AI 状态检查（异步，不阻塞 UI）
        viewModelScope.launch {
            checkAiStatus().onSuccess { available ->
                _uiState.update { it.copy(isAiOnline = available) }
            }
        }

        // 加载完成后
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    companion object {
        fun getTimeBasedGreeting(): String {
            val hour = LocalTime.now().hour
            return when (hour) {
                in 5..10 -> "早上好 ☀️"
                in 11..13 -> "中午好 🌤️"
                in 14..17 -> "下午好 🌈"
                in 18..21 -> "晚上好 🌙"
                else -> "夜深了 🌙"
            }
        }
    }
}
```

### 6.2 热量圆环组件

```kotlin
// ui/components/CaloriesRing.kt

@Composable
fun CaloriesRing(
    consumed: Double,
    goal: Double,
    protein: Double, proteinGoal: Double,
    carbs: Double, carbsGoal: Double,
    fat: Double, fatGoal: Double,
    modifier: Modifier = Modifier
) {
    val progress = (consumed / goal).toFloat().coerceIn(0f, 1.5f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800, easing = EaseOutCubic)
    )

    val color = when {
        progress > 1.1f -> MaterialTheme.colorScheme.error
        progress > 0.9f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Card(modifier = modifier.padding(16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 热量圆环
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(160.dp)) {
                    val strokeWidth = 12.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2

                    // 背景圆环
                    drawCircle(
                        color = color.copy(alpha = 0.15f),
                        radius = radius,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // 进度圆弧
                    drawArc(
                        color = color,
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // 中心文字
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${consumed.toInt()}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    Text(
                        "/ ${goal.toInt()} kcal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 宏量营养素条
            MacroBar("蛋白质", protein, proteinGoal, ProteinColor, "g")
            MacroBar("碳水", carbs, carbsGoal, CarbsColor, "g")
            MacroBar("脂肪", fat, fatGoal, FatColor, "g")
        }
    }
}
```

---

## 7. 模块六：设置与 API Key 管理

### 7.1 API 配置页状态管理

```kotlin
// ui/settings/AiSettingsViewModel.kt

@HiltViewModel
class AiSettingsViewModel @Inject constructor(
    private val encryptedKeyStore: EncryptedKeyStore,
    private val aiServiceManager: AiServiceManager,
    private val usageTracker: AiUsageTracker,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    data class AiSettingsUiState(
        val providers: List<ProviderUiModel> = emptyList(),
        val selectedProvider: String = "openai",
        val apiKeyInput: String = "",
        val isApiKeyVisible: Boolean = false,
        val connectionTestState: ConnectionTestState = ConnectionTestState.Idle,
        val visionModel: String = "",
        val chatModel: String = "",
        val monthlyUsage: AiUsageSummary? = null,
        val availableModels: List<String> = emptyList(),
        val isSaving: Boolean = false
    )

    sealed interface ConnectionTestState {
        data object Idle : ConnectionTestState
        data object Testing : ConnectionTestState
        data object Success : ConnectionTestState
        data class Failed(val message: String) : ConnectionTestState
    }

    data class ProviderUiModel(
        val id: String,
        val displayName: String,
        val isConfigured: Boolean,
        val isSelected: Boolean,
        val recommendedModel: String
    )

    // ... 方法实现
}
```

### 7.2 API Key 输入安全处理

```kotlin
// ui/settings/ApiKeyInputField.kt

@Composable
fun ApiKeyInputField(
    apiKey: String,
    onValueChange: (String) -> Unit,
    isVisible: Boolean,
    onToggleVisibility: () -> Unit,
    onPaste: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = apiKey,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text("API Key") },
        placeholder = { Text("sk-... 或 api-key-...") },
        singleLine = true,
        visualTransformation = if (isVisible)
            VisualTransformation.None
        else
            PasswordVisualTransformation('•'),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        trailingIcon = {
            Row {
                IconButton(onClick = onToggleVisibility) {
                    Icon(
                        if (isVisible) Icons.Outlined.VisibilityOff
                        else Icons.Outlined.Visibility,
                        contentDescription = if (isVisible) "隐藏" else "显示"
                    )
                }
                IconButton(onClick = onPaste) {
                    Icon(Icons.Outlined.ContentPaste, contentDescription = "粘贴")
                }
            }
        },
        supportingText = {
            Text(
                "API Key 将加密存储在本地设备，不会上传至任何服务器",
                style = MaterialTheme.typography.bodySmall
            )
        },
        isError = apiKey.isNotBlank() && !isValidApiKeyFormat(apiKey)
    )
}

private fun isValidApiKeyFormat(key: String): Boolean {
    // 基本格式校验
    return key.length >= 20 &&
           (key.startsWith("sk-") ||
            key.startsWith("api-key-") ||
            key.startsWith("AIza") ||
            key.length >= 32) // DeepSeek / 自定义
}
```

---

## 8. 本地数据管理

### 8.1 数据导出为 JSON/CSV

```kotlin
// domain/usecase/ExportDataUseCase.kt

@Inject
class ExportDataUseCase(
    private val foodRepository: FoodRepository,
    private val healthRepository: HealthRepository
) {
    suspend fun exportAsJson(startDate: LocalDate, endDate: LocalDate): String {
        val records = foodRepository.getRecordsByDateRange(startDate, endDate).first()
        val metrics = healthRepository.getMetricsByDateRange(startDate, endDate).first()

        return buildString {
            appendLine("{")
            appendLine("  \"export_date\": \"${LocalDate.now()}\",")
            appendLine("  \"period\": \"$startDate ~ $endDate\",")
            appendLine("  \"food_records\": [")
            records.forEachIndexed { i, r ->
                append("    ${Json.encodeToString(r)}")
                if (i < records.lastIndex) appendLine(",") else appendLine()
            }
            appendLine("  ],")
            appendLine("  \"health_metrics\": [")
            metrics.forEachIndexed { i, m ->
                append("    ${Json.encodeToString(m)}")
                if (i < metrics.lastIndex) appendLine(",") else appendLine()
            }
            appendLine("  ]")
            appendLine("}")
        }
    }

    suspend fun exportAsCsv(startDate: LocalDate, endDate: LocalDate): String {
        val records = foodRepository.getRecordsByDateRange(startDate, endDate).first()

        return buildString {
            appendLine("日期,餐次,食物名称,份量,单位,热量(kcal),蛋白质(g),碳水(g),脂肪(g),来源")
            records.forEach { r ->
                appendLine("${r.dateTime.toLocalDate()},${r.mealType},${r.food.name}," +
                    "${r.food.portion.amount},${r.food.portion.unit}," +
                    "${r.food.nutrition.calories},${r.food.nutrition.proteinGrams}," +
                    "${r.food.nutrition.carbsGrams},${r.food.nutrition.fatGrams},${r.source}")
            }
        }
    }
}
```

### 8.2 数据库迁移策略

```kotlin
// core/data/database/FitnessDatabase.kt

@Database(
    entities = [
        FoodRecordEntity::class,
        HealthMetricEntity::class,
        UserGoalEntity::class,
        AiProviderEntity::class,
        CachedFoodItemEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class FitnessDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun healthMetricDao(): HealthMetricDao
    abstract fun userGoalDao(): UserGoalDao
    abstract fun aiProviderDao(): AiProviderDao
    abstract fun cachedFoodDao(): CachedFoodDao

    companion object {
        // Migration 策略：每次版本升级添加新的 Migration
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE food_records ADD COLUMN fiber_grams REAL NOT NULL DEFAULT 0.0")
            }
        }
    }
}
```

---

> **下一篇**：[06 — 开发路线图文档](06-roadmap.md)
