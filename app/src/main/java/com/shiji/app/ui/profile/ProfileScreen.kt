package com.shiji.app.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shiji.app.ui.theme.BrandGreenDark
import com.shiji.app.ui.theme.BrandGreenLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    userName: String = "Shawn",
    userAvatar: String = "👤",
    isDarkTheme: Boolean = false,
    onEditProfile: (name: String, avatar: String) -> Unit = { _, _ -> },
    onToggleDarkTheme: (Boolean) -> Unit = {},
    onNavigateToGoal: () -> Unit = {},
    onNavigateToAiSettings: () -> Unit = {},
    onNavigateToAIChat: () -> Unit = {},
    onNavigateToFoodLibrary: () -> Unit = {},
    onNavigateToDataExport: () -> Unit = {},
    onNavigateToWeight: () -> Unit = {},
    onNavigateToWater: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {}
) {
    var showEditDialog by remember { mutableStateOf(false) }

    if (showEditDialog) {
        EditProfileDialog(
            currentName = userName,
            currentAvatar = userAvatar,
            onSave = { name, avatar -> onEditProfile(name, avatar); showEditDialog = false },
            onDismiss = { showEditDialog = false }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("我的", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Profile card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                onClick = { showEditDialog = true },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(64.dp), shape = CircleShape, color = BrandGreenLight) {
                            Box(contentAlignment = Alignment.Center) { Text(userAvatar, style = MaterialTheme.typography.headlineLarge) }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(userName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            Text("点击编辑资料", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("✏️", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(16.dp))
                    Surface(shape = RoundedCornerShape(50), color = BrandGreenLight) {
                        Text("🎯 目标：减脂 · 每日 1,800 kcal",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = BrandGreenDark)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // AI Assistant
            SectionDivider("AI 助手")
            SettingsItem("🤖", "AI 营养顾问", onClick = onNavigateToAIChat)

            Spacer(Modifier.height(8.dp))

            // Health section
            SectionDivider("健康")
            SettingsItem("⚖️", "体重追踪", onClick = onNavigateToWeight)
            SettingsItem("💧", "水分摄入", onClick = onNavigateToWater)

            Spacer(Modifier.height(8.dp))

            // Settings section
            SectionDivider("设置")
            SettingsItem("🎯", "目标设定", onClick = onNavigateToGoal)
            SettingsItem("🔑", "AI 模型配置", onClick = onNavigateToAiSettings)
            SettingsItem("📦", "个人食物库", onClick = onNavigateToFoodLibrary)
            SettingsItem("🌙", "深色模式", onClick = { onToggleDarkTheme(!isDarkTheme) }, trailing = {
                Switch(checked = isDarkTheme, onCheckedChange = onToggleDarkTheme)
            })
            SettingsItem("📤", "数据管理", onClick = onNavigateToDataExport)
            SettingsItem("📄", "隐私政策", onClick = onNavigateToPrivacy)
            SettingsItem("ℹ️", "关于食记", onClick = onNavigateToAbout)

            Spacer(Modifier.height(24.dp))

            // Stats section
            SectionDivider("统计")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📊 AI 用量统计", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    UsageRow("本月调用", "0 次")
                    UsageRow("估算费用", "¥0.00")
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable private fun SectionDivider(title: String) {
    Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
private fun SettingsItem(icon: String, label: String, onClick: () -> Unit, trailing: @Composable (() -> Unit)? = null) {
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.background) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(40.dp), shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant) {
                Box(contentAlignment = Alignment.Center) { Text(icon) }
            }
            Spacer(Modifier.width(16.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f))
            if (trailing != null) trailing() else Text("›", color = MaterialTheme.colorScheme.outline)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
}

@Composable
private fun UsageRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}
