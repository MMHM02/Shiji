# 🤖 04 — AI 集成方案文档

> 版本：v1.0 | 日期：2026-06-25 | 状态：初稿

---

## 1. AI 集成概览

### 1.1 设计哲学

```
用户自主 · 模型中立 · 隐私优先 · 代码透明 · 降级优雅
```

Fitness 不内置任何 AI 服务，不设后端代理。所有 AI 能力由用户自行配置的 API Key 驱动，应用直接与用户选择的模型厂商通信。

**开源 = API Key 安全的终极答案**。闭源应用让你"相信"它不会偷你的 Key；Fitness 让你"验证"它不会 — 整个 `EncryptedKeyStore` 加密链路、`AiService` 网络调用路径全部公开可审查。你不会在代码里找到任何将 Key 发送到非厂商地址的逻辑，因为根本没有。

```
┌──────────────────────────────────────────────────────┐
│              用户完全掌控 + 代码完全透明               │
│                                                      │
│   🔑 你的 API Key     🎛️ 你选模型     💰 你的账单    │
│   📖 代码可审计        🔍 网络可抓包    🏠 数据在本地  │
│                                                      │
│   Fitness 的职责：                                    │
│   • 提供优秀的客户端体验                              │
│   • 提供统一的 AI 适配层                              │
│   • 本地数据 + 云端 AI 的桥梁                        │
│   • 安全存储凭证（Keystore 加密，源码可查）            │
│   • 永远不做 API Key 中转                              │
└──────────────────────────────────────────────────────┘
```

### 1.2 支持模型矩阵

| 厂商 | 模型 | 视觉 | 对话 | 推荐场景 | 参考价格（每 1M token） |
|------|------|:----:|:----:|----------|:------------------------:|
| **OpenAI** | GPT-4o | ✅ | ✅ | 拍照识食（首选） | $2.50 / $10.00 |
| | GPT-4o-mini | ✅ | ✅ | 语音解析、简单对话 | $0.15 / $0.60 |
| **Anthropic** | Claude 4 Sonnet | ✅ | ✅ | 拍照识食、深度分析 | $3.00 / $15.00 |
| | Claude 4 Haiku | ✅ | ✅ | 快速识别、日常对话 | $0.25 / $1.25 |
| **Google AI** | Gemini 2.5 Flash | ✅ | ✅ | 高性价比视觉 | $0.15 / $0.60 |
| | Gemini 2.5 Pro | ✅ | ✅ | 复杂场景分析 | $1.25 / $5.00 |
| **DeepSeek** | DeepSeek-V3 | ❌ | ✅ | 纯文本对话、分析 | $0.27 / $1.10 |
| **自定义** | OpenAI 兼容 | 待定 | ✅ | 国产模型 / 自部署 | 视厂商而定 |

> 📝 **费用估算**：假设用户每天拍照识食 3 次（~500 input tokens + ~300 output tokens/次 = 约 7,200 tokens/月），使用 GPT-4o-mini 成本约 **$0.002/月**。加上 AI 对话，月均消费通常在 **$0.10 ~ $0.50**。

---

## 2. AI 适配层架构

### 2.1 适配器模式

```
                   ┌──────────────────┐
                   │    AiService      │  ← 统一接口
                   │   (interface)     │
                   └────────┬─────────┘
                            │
          ┌─────────┬───────┼───────┬──────────┬──────────┐
          ▼         ▼       ▼       ▼          ▼          ▼
     ┌────────┐┌────────┐┌──────┐┌────────┐┌────────┐┌─────────┐
     │ OpenAI ││Anthrop ││Google││DeepSeek││Custom ││Factory  │
     │Adapter ││Adapter ││Adapter││Adapter ││Adapter ││         │
     └───┬────┘└───┬────┘└──┬───┘└───┬────┘└───┬────┘└────┬────┘
         │         │        │        │         │          │
         └─────────┴────────┴────────┴─────────┘          │
                          │                                │
                          ▼                                ▼
              ┌──────────────────┐           ┌──────────────────────┐
              │  AiServiceManager│ ←─────── │EncryptedKeyStore     │
              │  (路由/选择/缓存) │           │(API Key 安全存储)    │
              └──────────────────┘           └──────────────────────┘
```

### 2.2 AiServiceManager — 核心路由

```kotlin
// core/ai/api/AiServiceManager.kt

@Singleton
class AiServiceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val encryptedKeyStore: EncryptedKeyStore
) {
    // 缓存的适配器实例（每个 provider 一个）
    private val adapters = mutableMapOf<String, AiService>()

    /**
     * 获取用于视觉分析的 AiService
     * 优先级：用户指定的视觉模型 > 默认视觉模型 > 第一个可用
     */
    fun getVisionService(): AiService? {
        val config = getConfig() ?: return null
        val visionModel = config.visionModel ?: return getChatService()
        return getService(visionModel)
    }

    /**
     * 获取用于对话的 AiService
     */
    fun getChatService(): AiService? {
        val config = getConfig() ?: return null
        val chatModel = config.chatModel ?: return null
        return getService(chatModel)
    }

    /**
     * 获取指定 model 的服务实例（带缓存）
     */
    fun getService(modelId: String): AiService? {
        val provider = resolveProvider(modelId)
        val apiKey = encryptedKeyStore.getKey(provider.id) ?: return null

        return adapters.getOrPut(provider.id) {
            createAdapter(provider, apiKey)
        }
    }

    /**
     * 测试连接
     */
    suspend fun testConnection(modelId: String): Result<Boolean> {
        val service = getService(modelId) ?: return Result.failure(
            IllegalStateException("No API key configured for $modelId")
        )
        return runCatching {
            service.chat(
                messages = listOf(ChatMessage(ChatRole.USER, "Hello, reply with just 'ok'.")),
                systemPrompt = "Reply with only the word 'ok'."
            ).isSuccess
        }
    }

    private fun createAdapter(provider: AiProviderConfig, apiKey: String): AiService {
        return when (provider.type) {
            "openai" -> OpenAiAdapter(apiKey, provider.baseUrl, okHttpClient)
            "anthropic" -> AnthropicAdapter(apiKey, provider.baseUrl, okHttpClient)
            "google" -> GoogleAiAdapter(apiKey, provider.baseUrl, okHttpClient)
            "deepseek" -> DeepSeekAdapter(apiKey, provider.baseUrl, okHttpClient)
            else -> CustomOpenAiCompatibleAdapter(apiKey, provider.baseUrl, okHttpClient)
        }
    }

    /**
     * 清除所有适配器缓存（API Key 变更时调用）
     */
    fun invalidate() {
        adapters.clear()
    }
}
```

---

## 3. 核心 AI 功能实现

### 3.1 拍照识食 — Prompt 工程

```kotlin
// core/ai/api/FoodAnalysisPrompts.kt

object FoodAnalysisPrompts {

    const val SYSTEM_PROMPT = """
你是一位专业的营养分析师。你的任务是分析用户提供的食物照片，识别照片中所有的食物项目，并估算每项食物的营养信息。

分析规则：
1. 识别照片中所有可见的食物和饮品。
2. 根据食物外观、盛放容器、参照物估算份量大小。
3. 基于标准营养数据库估算每项食物的热量和宏量营养素。
4. 如果无法确定具体食物，请给出最合理的推测并标注 confidence < 0.7。
5. 如果照片中没有食物，返回空列表并说明原因。

重要提醒：
- 你只能估算，不能精确测量，请在 confidence 字段体现你的确定程度。
- 对于中餐、混合菜肴，请尽量拆分主要食材分别估算。
- 饮品也需要估算热量（包括含糖饮料、奶茶等）。
"""

    const val USER_PROMPT_TEMPLATE = """
请分析这张食物照片，返回以下 JSON 格式的结果。

要求：
- 逐项列出所有食物/饮品
- 份量单位为: grams, milliliters, serving, bowl, piece, cup 之一
- 热量单位: kcal
- confidence 取值 0.0 到 1.0，表示你对每项识别的确定程度

返回格式（严格 JSON）：
{
  "items": [
    {
      "name": "食物名称",
      "portion": 份量数值,
      "portion_unit": "份量单位",
      "calories": 热量,
      "protein_grams": 蛋白质克数,
      "carbs_grams": 碳水克数,
      "fat_grams": 脂肪克数,
      "confidence": 0.85
    }
  ],
  "total_calories": 所有食物热量总和,
  "general_confidence": 0.88,
  "notes": "额外的分析备注（可选）"
}
"""
}
```

### 3.2 拍照识食 — 完整流式处理链

```kotlin
// domain/usecase/AnalyzeFoodPhotoUseCase.kt

@Inject
class AnalyzeFoodPhotoUseCase(
    private val aiServiceManager: AiServiceManager,
    private val imageProcessor: ImageProcessor,
    private val analytics: AnalyticsTracker
) {
    suspend operator fun invoke(
        imageUri: Uri,
        context: Context
    ): Result<FoodAnalysisResult> {
        // Step 1: 图片预处理
        val processedImage = imageProcessor.process(
            uri = imageUri,
            maxWidth = 2048,
            maxHeight = 2048,
            quality = 85,
            maxSizeBytes = 2 * 1024 * 1024  // 2MB
        ).getOrElse { return Result.failure(it) }

        // Step 2: 获取视觉 AI 服务
        val aiService = aiServiceManager.getVisionService()
            ?: return Result.failure(AiNotConfiguredException("请先配置 AI API Key"))

        // Step 3: 调用 AI 分析
        val startTime = System.currentTimeMillis()
        val result = aiService.analyzeFoodImage(
            imageBytes = processedImage.bytes,
            mimeType = "image/jpeg",
            prompt = FoodAnalysisPrompts.USER_PROMPT_TEMPLATE
        )

        // Step 4: 记录分析耗时
        val elapsed = System.currentTimeMillis() - startTime
        analytics.track("food_analysis", mapOf(
            "duration_ms" to elapsed,
            "items_count" to result.getOrNull()?.items?.size,
            "success" to result.isSuccess
        ))

        return result
    }
}
```

### 3.3 图片预处理器

```kotlin
// core/camera/ImageProcessor.kt

class ImageProcessor @Inject constructor() {

    data class ProcessedImage(
        val bytes: ByteArray,
        val width: Int,
        val height: Int,
        val originalSize: Int,
        val compressedSize: Int
    )

    fun process(
        uri: Uri,
        context: Context,
        maxWidth: Int = 2048,
        maxHeight: Int = 2048,
        quality: Int = 85,
        maxSizeBytes: Int = 2 * 1024 * 1024
    ): Result<ProcessedImage> = runCatching {
        // Step 1: 读取原始图片元信息
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }

        // Step 2: 计算采样率（大幅缩小图片）
        val sampleSize = calculateInSampleSize(
            options.outWidth, options.outHeight, maxWidth, maxHeight
        )

        // Step 3: 解码并缩放
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: throw IOException("Failed to decode image")

        // Step 4: 转换为 JPEG 并压缩
        val outputStream = ByteArrayOutputStream()
        var compressQuality = quality
        do {
            outputStream.reset()
            bitmap.compress(Bitmap.CompressFormat.JPEG, compressQuality, outputStream)
            compressQuality -= 5
        } while (outputStream.size() > maxSizeBytes && compressQuality > 20)

        // Step 5: 释放 bitmap
        bitmap.recycle()

        ProcessedImage(
            bytes = outputStream.toByteArray(),
            width = bitmap.width,
            height = bitmap.height,
            originalSize = options.outWidth * options.outHeight,
            compressedSize = outputStream.size()
        )
    }

    private fun calculateInSampleSize(
        rawWidth: Int, rawHeight: Int,
        reqWidth: Int, reqHeight: Int
    ): Int {
        var inSampleSize = 1
        if (rawHeight > reqHeight || rawWidth > reqWidth) {
            val halfHeight = rawHeight / 2
            val halfWidth = rawWidth / 2
            while ((halfHeight / inSampleSize) >= reqHeight &&
                   (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
```

---

## 4. 语音识别 + AI 解析管道

### 4.1 语音识别封装

```kotlin
// core/voice/SpeechRecognizerWrapper.kt

class SpeechRecognizerWrapper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val speechRecognizer: SpeechRecognizer =
        SpeechRecognizer.createSpeechRecognizer(context)

    /**
     * 开始语音识别，返回识别结果 Flow
     */
    fun startListening(): Flow<VoiceRecognitionState> = callbackFlow {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                trySend(VoiceRecognitionState.Ready)
            }

            override fun onBeginningOfSpeech() {
                trySend(VoiceRecognitionState.Listening)
            }

            override fun onRmsChanged(rmsdB: Float) {
                trySend(VoiceRecognitionState.Volume(rmsdB / 10f + 0.5f))
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val results = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                results?.firstOrNull()?.let {
                    trySend(VoiceRecognitionState.PartialResult(it))
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                trySend(VoiceRecognitionState.FinalResult(text))
                channel.close()
            }

            override fun onError(error: Int) {
                trySend(VoiceRecognitionState.Error(
                    getErrorText(error)
                ))
                channel.close()
            }

            override fun onEndOfSpeech() {
                trySend(VoiceRecognitionState.Processing)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        speechRecognizer.setRecognitionListener(listener)
        speechRecognizer.startListening(intent)

        awaitClose {
            speechRecognizer.destroy()
        }
    }

    fun stopListening() {
        speechRecognizer.stopListening()
    }

    fun cancel() {
        speechRecognizer.cancel()
    }
}

sealed interface VoiceRecognitionState {
    data object Ready : VoiceRecognitionState
    data object Listening : VoiceRecognitionState
    data object Processing : VoiceRecognitionState
    data class Volume(val level: Float) : VoiceRecognitionState   // 0.0~1.0
    data class PartialResult(val text: String) : VoiceRecognitionState
    data class FinalResult(val text: String) : VoiceRecognitionState
    data class Error(val message: String) : VoiceRecognitionState
}
```

### 4.2 语音→食物解析 Prompt

```kotlin
// core/ai/api/FoodVoicePrompts.kt

object FoodVoicePrompts {

    const val SYSTEM_PROMPT = """
你是一位专业的营养师助手。用户会用自然语言口述他们吃了什么，你需要解析这些描述并估算营养信息。

解析规则：
1. 从自然语言中提取所有食物/饮品项。
2. 根据常见份量估算每项的热量和宏量营养素。
3. 如果用户描述了份量（如"一小碗""两个""半杯"），使用该信息。
4. 如果用户没有明确份量，使用常见份量（如"一碗米饭≈300g"）。
5. 对于不清楚的食物，给出最佳推测并标注低置信度。
6. 饮品同样需要估算（特别是含糖饮料、奶茶等）。

注意：
- 中式菜名可能含混（如"一份盖饭"），请结合常识拆分估算。
- 用户可能使用口语化的份量描述（"一点""少许"等）。
- 将所有营养数值以"每份"计算，不是每100g。
"""

    fun buildUserPrompt(userText: String, mealContext: String? = null): String = buildString {
        append("请分析以下用户口述的饮食内容，返回结构化营养信息。\n\n")
        if (mealContext != null) {
            append("用餐场景：$mealContext\n\n")
        }
        append("用户口述内容：\n\"$userText\"\n\n")
        append("""
返回格式（严格 JSON）：
{
  "items": [
    {
      "name": "食物名称",
      "portion": 份量数值,
      "portion_unit": "份量单位 (grams/ml/serving/bowl/piece/cup)",
      "calories": 热量(kcal),
      "protein_grams": 蛋白质(g),
      "carbs_grams": 碳水(g),
      "fat_grams": 脂肪(g),
      "confidence": 0.85
    }
  ],
  "total_calories": 总热量,
  "general_confidence": 总体置信度,
  "notes": "补充说明（可选）"
}
        """.trimIndent())
    }
}
```

---

## 5. AI 对话顾问

### 5.1 对话上下文注入

```kotlin
// domain/usecase/ChatWithAiUseCase.kt

@Inject
class ChatWithAiUseCase(
    private val aiServiceManager: AiServiceManager,
    private val foodRepository: FoodRepository,
    private val healthRepository: HealthRepository,
    private val userGoalRepository: UserGoalRepository
) {
    suspend fun buildSystemPrompt(): String {
        val today = LocalDate.now()
        val weekAgo = today.minusDays(7)

        // 收集用户上下文数据
        val todayNutrition = foodRepository.getDailyNutrition(today).first()
        val weeklyRecords = foodRepository.getRecordsByDateRange(weekAgo, today).first()
        val weightHistory = healthRepository.getMetricsByType(MetricType.WEIGHT, weekAgo, today).first()
        val goal = userGoalRepository.getGoal().first() ?: UserGoalEntity()

        // 构建系统 Prompt
        return """
你是一位专业的私人营养师和健身顾问，名叫 "Fitness AI"。

## 用户当前数据
- 今日已摄入: ${todayNutrition?.totalCalories ?: 0} kcal (目标: ${goal.dailyCalories} kcal)
- 蛋白质: ${todayNutrition?.totalProtein ?: 0}g / ${goal.proteinTarget}g
- 碳水: ${todayNutrition?.totalCarbs ?: 0}g / ${goal.carbsTarget}g
- 脂肪: ${todayNutrition?.totalFat ?: 0}g / ${goal.fatTarget}g
- 当前体重: ${weightHistory.lastOrNull()?.value ?: "无记录"} kg
- 目标: ${goal.goalType.description}

## 最近一周饮食概览
平均每日摄入: ${weeklyRecords.groupBy { it.dateTime.toLocalDate() }
    .map { (_, records) -> records.sumOf { it.food.nutrition.calories } }
    .average().takeIf { !it.isNaN() }?.let { "%.0f".format(it) } ?: "无数据"} kcal

## 你的职责
1. 根据用户数据提供个性化饮食建议。
2. 分析饮食结构和营养均衡度。
3. 推荐合适的食谱和食物选择。
4. 回答营养、健身相关问题。
5. 保持鼓励和正向的语气。

## 重要规则
- 你不是医生，不提供医疗建议。
- 如果用户询问极端饮食或危险行为，请温和劝阻。
- 回复简洁实用，避免冗长。
- 基于用户实际数据，而非泛泛而谈。
- 始终提醒用户：你的建议仅供参考，重要决策请咨询专业医师。
        """.trimIndent()
    }

    suspend operator fun invoke(
        userMessage: String,
        chatHistory: List<ChatMessage>
    ): Result<String> {
        val aiService = aiServiceManager.getChatService()
            ?: return Result.failure(AiNotConfiguredException("请先配置 AI API Key"))

        val systemPrompt = buildSystemPrompt()
        val messages = listOf(
            ChatMessage(ChatRole.SYSTEM, systemPrompt)
        ) + chatHistory + listOf(
            ChatMessage(ChatRole.USER, userMessage)
        )

        return aiService.chat(messages)
    }
}
```

---

## 6. 用量追踪与成本估算

### 6.1 用量追踪

```kotlin
// data/local/AiUsageTracker.kt

@Singleton
class AiUsageTracker @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val USAGE_PREFIX = "ai_usage"

    /**
     * 记录一次 API 调用
     */
    suspend fun recordUsage(
        providerId: String,
        modelId: String,
        feature: AiFeature,
        inputTokens: Int,
        outputTokens: Int,
        durationMs: Long,
        success: Boolean
    ) {
        dataStore.edit { prefs ->
            val date = LocalDate.now().toString()
            val key = "$USAGE_PREFIX:$providerId:$modelId:$date"

            val current = prefs[key]?.let { Json.decodeFromString<DailyUsage>(it) }
                ?: DailyUsage(date, providerId, modelId)

            val updated = current.copy(
                callCount = current.callCount + 1,
                totalInputTokens = current.totalInputTokens + inputTokens,
                totalOutputTokens = current.totalOutputTokens + outputTokens,
                totalDurationMs = current.totalDurationMs + durationMs,
                successCount = current.successCount + if (success) 1 else 0,
                failCount = current.failCount + if (!success) 1 else 0,
                byFeature = current.byFeature.toMutableMap().apply {
                    val f = getOrDefault(feature.name, FeatureUsage())
                    put(feature.name, f.copy(count = f.count + 1, tokens = f.tokens + inputTokens + outputTokens))
                }
            )

            prefs[key] = Json.encodeToString(updated)
        }
    }

    /**
     * 获取本月用量摘要
     */
    suspend fun getMonthlySummary(): AiUsageSummary {
        val thisMonth = YearMonth.now()
        var totalCalls = 0
        var totalInputTokens = 0L
        var totalOutputTokens = 0L
        var totalCostEstimate = 0.0

        dataStore.data.first().asMap().forEach { (key, value) ->
            if (key.name.startsWith("$USAGE_PREFIX:")) {
                val usage = Json.decodeFromString<DailyUsage>(value as String)
                val date = LocalDate.parse(usage.date)
                if (YearMonth.from(date) == thisMonth) {
                    totalCalls += usage.callCount
                    totalInputTokens += usage.totalInputTokens
                    totalOutputTokens += usage.totalOutputTokens
                    totalCostEstimate += estimateCost(usage.providerId, usage.modelId,
                        usage.totalInputTokens, usage.totalOutputTokens)
                }
            }
        }

        return AiUsageSummary(
            month = thisMonth,
            totalCalls = totalCalls,
            totalInputTokens = totalInputTokens,
            totalOutputTokens = totalOutputTokens,
            estimatedCost = totalCostEstimate
        )
    }

    private fun estimateCost(provider: String, model: String, inputTokens: Long, outputTokens: Long): Double {
        val pricing = PricingTable.get(provider, model)
        return (inputTokens / 1_000_000.0) * pricing.inputPrice +
               (outputTokens / 1_000_000.0) * pricing.outputPrice
    }
}

data class DailyUsage(
    val date: String,
    val providerId: String,
    val modelId: String,
    val callCount: Int = 0,
    val totalInputTokens: Long = 0,
    val totalOutputTokens: Long = 0,
    val totalDurationMs: Long = 0,
    val successCount: Int = 0,
    val failCount: Int = 0,
    val byFeature: Map<String, FeatureUsage> = emptyMap()
)
```

---

## 7. 错误处理与降级策略

### 7.1 AI 服务异常分类

```kotlin
// core/ai/api/AiExceptions.kt

sealed class AiException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /** API Key 未配置或无效 */
    data class Unauthorized(override val message: String = "API Key 无效或已过期") : AiException(message)

    /** 配额用尽（HTTP 429） */
    data class QuotaExceeded(override val message: String = "API 配额已用尽，请稍后再试或更换 Key") : AiException(message)

    /** 网络超时 */
    data class Timeout(override val message: String = "请求超时，请检查网络连接") : AiException(message)

    /** 模型不支持指定功能 */
    data class FeatureNotSupported(override val message: String) : AiException(message)

    /** 内容安全拦截 */
    data class ContentFiltered(override val message: String = "内容被安全策略拦截") : AiException(message)

    /** 解析失败 */
    data class ParseError(override val message: String = "AI 返回格式异常，请重试") : AiException(message)

    /** 未知错误 */
    data class Unknown(override val message: String = "未知错误", cause: Throwable? = null) : AiException(message, cause)
}
```

### 7.2 降级流程图

```
用户触发 AI 功能
       │
       ▼
  ┌─────────────┐    否     ┌──────────────────┐
  │ API Key     │─────────▶ │ 引导用户配置 API  │
  │ 已配置？    │           │ Key + 图文教程    │
  └─────┬───────┘           └──────────────────┘
        │ 是
        ▼
  ┌─────────────┐    否     ┌──────────────────┐
  │ 网络可用？  │─────────▶ │ 提示离线/网络异常  │
  └─────┬───────┘           │ 降级：手动输入    │
        │ 是                └──────────────────┘
        ▼
  ┌─────────────┐    失败    ┌──────────────────┐
  │ 调用 AI API │─────────▶ │ 错误分类处理       │
  └─────┬───────┘           │ • 401 → 引导更新Key│
        │ 成功              │ • 429 → 提示等一会  │
        ▼                   │ • 超时 → 重试/手动  │
  ┌─────────────┐           │ • 其他 → 手动输入   │
  │ 解析结果    │           └──────────────────┘
  └─────┬───────┘
        │
    ┌───┴───┐
    ▼       ▼
  成功    解析失败
    │       │
    ▼       ▼
  展示    提示用户
  结果    手动输入
```

---

## 8. Prompt 版本管理

### 8.1 版本化 Prompt

```kotlin
// core/ai/api/PromptRegistry.kt

/**
 * Prompt 注册表：集中管理所有 AI Prompt，支持版本迭代和 A/B 测试
 */
object PromptRegistry {

    data class PromptTemplate(
        val version: Int,
        val systemPrompt: String,
        val userPromptTemplate: String,
        val responseSchema: String // JSON Schema for structured output
    )

    val FOOD_ANALYSIS = PromptTemplate(
        version = 1,
        systemPrompt = FoodAnalysisPrompts.SYSTEM_PROMPT,
        userPromptTemplate = FoodAnalysisPrompts.USER_PROMPT_TEMPLATE,
        responseSchema = FOOD_ANALYSIS_JSON_SCHEMA
    )

    val VOICE_FOOD_PARSE = PromptTemplate(
        version = 1,
        systemPrompt = FoodVoicePrompts.SYSTEM_PROMPT,
        userPromptTemplate = "",  // 动态构建
        responseSchema = FOOD_ANALYSIS_JSON_SCHEMA
    )

    // 允许通过 DataStore 远程更新 Prompt（可选功能）
    // 这样可以在不发布新版本的情况下优化 Prompt
}
```

---

## 9. 隐私与合规

### 9.1 数据处理原则

| 原则 | 实现 |
|------|------|
| **最小化传输** | 仅发送必要的图片/文本到 AI API；本地数据不上传 |
| **Key 秒级销毁** | API Key 仅在内存中解密使用，不落盘明文 |
| **用户知情** | 每次 AI 调用前展示将发送的数据概要 |
| **可撤销** | 用户可随时删除 API Key，所有 Key 本地加密存储 |
| **可验证** | 源码公开，任何人均可审计 Key 的完整生命周期，确认无后门 |
| **合规** | 遵循 Google Play 用户数据政策；AI 调用属于用户自主行为 |

### 9.2 API Key 安全生命周期

```
┌─────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│ 用户输入 │ →  │ Keystore │ →  │ 内存中   │ →  │ HTTP     │
│ API Key  │    │ 加密存储  │    │ 临时解密  │    │ Header   │
└─────────┘    └──────────┘    └──────────┘    └──────────┘
                                                      │
                                    ┌─────────────────┘
                                    ▼
                              ┌──────────┐
                              │ 请求完成  │
                              │ 内存清理  │
                              │ Key 不缓存│
                              └──────────┘
```

---

> **下一篇**：[05 — 功能实现方案文档](05-feature-implementation.md)
