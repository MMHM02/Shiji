package com.shiji.app.ui.camera

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.shiji.app.ui.components.LoadingState
import com.shiji.app.ui.foodconfirm.EditableFoodItem
import com.shiji.app.ui.foodconfirm.FoodConfirmContent
import java.io.File

private enum class CameraStep { PREVIEW, PHOTO_CONFIRM, ANALYZING, RESULT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    onBack: () -> Unit = {},
    onSaved: () -> Unit = {},
    onNavigateToAiSettings: () -> Unit = {},
    onNavigateToTextRecord: () -> Unit = {},
    viewModel: CameraViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var step by remember { mutableStateOf(CameraStep.PREVIEW) }

    // ---- permissions ----
    var hasCameraPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }
    LaunchedEffect(Unit) {
        hasCameraPermission = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasCameraPermission) permissionLauncher.launch(android.Manifest.permission.CAMERA)
    }

    // ---- gallery picker ----
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onPhotoReady(uri)
            step = CameraStep.PHOTO_CONFIRM
        }
    }

    // Navigate back after a successful save.
    LaunchedEffect(uiState.saved) {
        if (uiState.saved) { onSaved(); viewModel.retake() }
    }

    Scaffold { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (step) {
                CameraStep.PREVIEW -> {
                    if (hasCameraPermission) {
                        CameraPreviewContent(
                            onPhotoCaptured = { uri ->
                                viewModel.onPhotoReady(uri)
                                step = CameraStep.PHOTO_CONFIRM
                            },
                            onGalleryClick = {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onBack = onBack
                        )
                    } else {
                        PermissionRequestContent(
                            onRequest = { permissionLauncher.launch(android.Manifest.permission.CAMERA) },
                            onGalleryClick = {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onBack = onBack
                        )
                    }
                }

                CameraStep.PHOTO_CONFIRM -> PhotoConfirmContent(
                    photoUri = uiState.photoUri,
                    onRetake = { viewModel.retake(); step = CameraStep.PREVIEW },
                    onUse = {
                        step = CameraStep.ANALYZING
                        viewModel.analyze(context)
                    }
                )

                CameraStep.ANALYZING -> {
                    when (val analysis = uiState.analysisState) {
                        is CameraViewModel.AnalysisState.Analyzing,
                        is CameraViewModel.AnalysisState.Idle -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    LoadingState(message = "AI 正在识别食物...")
                                    Spacer(Modifier.height(8.dp))
                                    TextButton(onClick = {
                                        viewModel.cancelAnalysis(); step = CameraStep.PHOTO_CONFIRM
                                    }) { Text("取消") }
                                }
                            }
                        }
                        is CameraViewModel.AnalysisState.Success -> {
                            LaunchedEffect(Unit) { step = CameraStep.RESULT }
                        }
                        is CameraViewModel.AnalysisState.NoFood -> {
                            AnalysisErrorContent(
                                emoji = "🍽️",
                                title = "没有识别到食物",
                                body = "照片中没有检测到食物，请重新拍摄清晰的餐食照片",
                                primaryLabel = "重新拍摄",
                                onPrimary = { viewModel.retake(); step = CameraStep.PREVIEW },
                                secondaryLabel = "改用文字记录",
                                onSecondary = onNavigateToTextRecord
                            )
                        }
                        is CameraViewModel.AnalysisState.Failed -> {
                            if (analysis.visionMissing) {
                                AnalysisErrorContent(
                                    emoji = "👁️",
                                    title = "未配置视觉模型",
                                    body = "拍照识食需要视觉模型（如 GLM-4V / Qwen-VL / GPT-4o）。你也可以改用文字描述记录。",
                                    primaryLabel = "去配置视觉模型",
                                    onPrimary = onNavigateToAiSettings,
                                    secondaryLabel = "改用文字记录",
                                    onSecondary = onNavigateToTextRecord
                                )
                            } else {
                                AnalysisErrorContent(
                                    emoji = "⚠️",
                                    title = "分析失败",
                                    body = analysis.message,
                                    primaryLabel = "重试",
                                    onPrimary = { viewModel.analyze(context) },
                                    secondaryLabel = "重新拍摄",
                                    onSecondary = { viewModel.retake(); step = CameraStep.PREVIEW }
                                )
                            }
                        }
                    }
                }

                CameraStep.RESULT -> ResultConfirmContent(
                    uiState = uiState,
                    onSetMealType = viewModel::setMealType,
                    onUpdateItem = viewModel::updateItem,
                    onRemoveItem = viewModel::removeItem,
                    onRetake = { viewModel.retake(); step = CameraStep.PREVIEW },
                    onSave = { viewModel.save(context) }
                )
            }
        }
    }
}

// ==================== camera preview ====================

@Composable
private fun CameraPreviewContent(
    onPhotoCaptured: (Uri) -> Unit,
    onGalleryClick: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { mutableStateOf<ImageCapture?>(null) }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture
                        )
                        imageCapture.value = capture
                    } catch (_: Exception) { /* camera unavailable — gallery still works */ }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // top bar
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.Close, "返回", tint = Color.White) }
            Spacer(Modifier.weight(1f))
            Text("拍照识食", color = Color.White, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.size(48.dp))
        }

        // bottom controls
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 36.dp),
            horizontalArrangement = Arrangement.spacedBy(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onGalleryClick, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.PhotoLibrary, "相册", tint = Color.White, modifier = Modifier.size(30.dp))
            }
            // capture button
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = {
                        val capture = imageCapture.value ?: return@IconButton
                        val photoFile = File(
                            context.cacheDir, "capture_${System.currentTimeMillis()}.jpg"
                        )
                        val output = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                        capture.takePicture(
                            output, ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(results: ImageCapture.OutputFileResults) {
                                    onPhotoCaptured(Uri.fromFile(photoFile))
                                }
                                override fun onError(exception: ImageCaptureException) { /* keep preview */ }
                            }
                        )
                    },
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.White)
                ) {}
            }
            Spacer(Modifier.size(48.dp)) // keeps the shutter centered
        }
    }
}

@Composable
private fun PermissionRequestContent(
    onRequest: () -> Unit,
    onGalleryClick: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📷", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(16.dp))
        Text("需要相机权限", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text("拍摄食物照片后，AI 将自动识别并估算营养。照片仅保存在本机。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequest, shape = RoundedCornerShape(26.dp)) { Text("授权相机") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onGalleryClick, shape = RoundedCornerShape(26.dp)) { Text("从相册选择") }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack) { Text("返回") }
    }
}

@Composable
private fun PhotoConfirmContent(
    photoUri: Uri?,
    onRetake: () -> Unit,
    onUse: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f).background(Color.Black)) {
            AsyncImage(
                model = photoUri,
                contentDescription = "拍摄的食物照片",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onRetake,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(26.dp)
            ) { Text("重拍") }
            Button(
                onClick = onUse,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(26.dp)
            ) { Text("使用照片") }
        }
    }
}

@Composable
private fun AnalysisErrorContent(
    emoji: String,
    title: String,
    body: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(emoji, style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onPrimary, shape = RoundedCornerShape(26.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)) { Text(primaryLabel) }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onSecondary, shape = RoundedCornerShape(26.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)) { Text(secondaryLabel) }
    }
}

// ==================== result confirmation ====================

@Composable
private fun ResultConfirmContent(
    uiState: CameraViewModel.CameraUiState,
    onSetMealType: (String) -> Unit,
    onUpdateItem: (String, (EditableFoodItem) -> EditableFoodItem) -> Unit,
    onRemoveItem: (String) -> Unit,
    onRetake: () -> Unit,
    onSave: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        // header with photo thumbnail
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = uiState.photoUri,
                contentDescription = null,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("确认识别结果", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("识别自 ${(uiState.analysisState as? CameraViewModel.AnalysisState.Success)?.modelLabel ?: "AI"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        FoodConfirmContent(
            items = uiState.items,
            selectedMealType = uiState.selectedMealType,
            onSetMealType = onSetMealType,
            onUpdateItem = onUpdateItem,
            onRemoveItem = onRemoveItem,
            onCancel = onRetake,
            onSave = onSave,
            cancelLabel = "重拍"
        )
    }
}
