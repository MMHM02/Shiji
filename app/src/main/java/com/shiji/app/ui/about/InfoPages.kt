package com.shiji.app.ui.about

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit = {}) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("隐私政策", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("食记 — 隐私政策", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("最后更新：2026年7月14日", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Section("数据存储") {
                Text("食记的所有数据均存储在你的设备本地，不上传到任何服务器。包括但不限于：")
                Bullet("饮食记录（食物名称、热量、营养素、照片）")
                Bullet("健康指标（体重、运动、水分摄入）")
                Bullet("个人资料（昵称、头像、身高、体重目标）")
                Bullet("食物库（自定义食物条目）")
                Bullet("AI 用量统计")
            }

            Section("API Key") {
                Text("你的 AI API Key 使用 Android Keystore + AES-256 加密存储在设备的 EncryptedSharedPreferences 中。")
                Text("API Key 仅用于直接向 AI 厂商（如 DeepSeek、OpenAI 等）发送请求，不会经过任何中间服务器。")
                Text("网络请求直接在你的设备和 AI 厂商服务器之间传输，我们无法访问你的 API Key 或请求内容。")
            }

            Section("照片") {
                Text("拍照识食功能产生的照片仅用于 AI 分析，临时保存在应用私有目录中，最多保留 3 天后自动清理。")
                Text("照片不会上传到任何服务器。")
            }

            Section("权限使用") {
                Bullet("相机：仅用于拍摄食物照片")
                Bullet("网络：仅用于 AI API 调用")
                Bullet("Health Connect：仅用于读取健康数据（可选，未来版本）")
            }

            Section("开源承诺") {
                Text("食记 100% 开源（Apache 2.0 许可证），所有代码可在 GitHub 上审计。")
                Text("你可以验证：API Key 的存储方式、网络请求的目标地址、数据是否被上传。每行代码都可见，无需信任，只需核实。")
            }

            Section("联系我们") {
                Text("如有隐私问题或建议，请通过 GitHub Issues 联系我们。")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit = {}) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("关于食记", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Text("🍽️", style = MaterialTheme.typography.displayLarge)
            Text("食记", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("AI 驱动的饮食记录 App", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))

            InfoRow("版本", "v0.2.1")
            InfoRow("平台", "Android 8.0+")
            InfoRow("技术栈", "Kotlin + Compose + Room + Hilt")
            InfoRow("许可证", "Apache 2.0")
            InfoRow("代码仓库", "GitHub (开源)")
            InfoRow("开发者", "MMHM")
            Spacer(Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🔓 开源承诺", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("代码 100% 公开可审计。API Key 如何存储、网络请求发往何处、数据是否上传——每一行代码都可以验证，无需信任，只需核实。",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        content()
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun Bullet(text: String) {
    Row(modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp)) {
        Text("•  ", style = MaterialTheme.typography.bodyMedium)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
