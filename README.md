# 🍽️ 食记 — AI 驱动的饮食记录与健康数据看板

<p align="center">
  <img src="https://img.shields.io/badge/version-v0.1.3-green" alt="Version">
  <img src="https://img.shields.io/badge/license-Apache%202.0-blue" alt="License">
  <img src="https://img.shields.io/badge/platform-Android%208.0%2B-green" alt="Platform">
  <img src="https://img.shields.io/badge/language-Kotlin-purple" alt="Language">
  <img src="https://img.shields.io/badge/architecture-MVVM-orange" alt="Architecture">
</p>

## 项目概述

**食记** 是一款完全开源的 Android 饮食记录应用。接入你自己的大模型 API Key，拍照/文字记录每一餐，AI 为你估算营养并给出个性化建议。所有数据存本地，代码公开可审计。

> 🔓 你的 API Key，你的数据，你的 AI 选择。食记不做中间人，代码公开可查。

| 属性 | 说明 |
|------|------|
| **平台** | Android 8.0+ (API 26+) |
| **开发语言** | Kotlin 2.1 |
| **UI** | Jetpack Compose + Material 3 |
| **架构** | MVVM + Hilt DI |
| **数据库** | Room + DataStore |
| **AI** | 多厂商适配（DeepSeek / Kimi / 通义千问 / 智谱 GLM / 自定义） |
| **许可证** | Apache 2.0 |
| **仓库** | https://github.com/MMHM02/Shiji |

---

## 功能

| 模块 | 功能 |
|------|------|
| 🔑 **AI 配置** | 自选厂商，API Key 加密存储（Android Keystore），连接测试，用量统计 |
| 📷 **拍照识食** | CameraX 拍摄/相册选图 → AI 视觉分析 → 可编辑确认 → 存 Room |
| ✍️ **文字记录** | 自然语言描述 → AI 解析食物营养 → 确认保存 |
| ✋ **手动记录** | 手动输入/搜索食物库，AI 一键估算营养素 |
| 🤖 **AI 顾问** | SSE 流式对话，注入真实饮食数据上下文，建议精准 |
| 📊 **首页仪表盘** | 热量圆环 + 水分进度条 + 营养素卡片 + 三餐列表 + 日期切换补签 |
| 📈 **数据看板** | 近7天热量趋势图 + 基于真实数据的 AI 建议卡片 |
| ⚖️ **体重追踪** | 手动记录 + 趋势折线图，数据持久化 Room 重启不丢 |
| 💧 **水分摄入** | 快速加水确认弹窗 + 每日目标可自定义 + 首页竖条进度 |
| 🎯 **目标设定** | 预设/自定义 kcal·蛋白质·碳水·脂肪目标 + 体重同步 |
| 📋 **饮食日志** | 日历快速跳转 + 有记录日期浅绿高亮 + 饮水进度条 |
| 🗂️ **个人食物库** | 随记录自动沉淀常用食物，快速复用 |
| 🌙 **深色模式** | 一键切换明/暗主题 |
| 📤 **数据管理** | JSON 导出/导入，数据备份与恢复 |

---

## 快速开始

```bash
git clone https://github.com/MMHM02/Shiji.git
```

用 Android Studio 打开项目，Sync Gradle → Run。或直接下载 [最新 Release APK](https://github.com/MMHM02/Shiji/releases) 安装。

**使用步骤**：

1. 完成 3 步引导（目标 → AI 配置 → 就绪，均可跳过）
2. **我的 → AI 模型配置** → 选厂商 → 输 API Key → 点测试连接
3. 测试通过后即可使用全部 AI 功能

---

## 项目结构

```
fitness/
├── app/                             # 主应用（DI / 导航 / 所有界面）
├── core/
│   ├── ai/                          # AI 适配层（OpenAI 兼容接口 + SSE 流式）
│   ├── camera/                      # CameraX 封装 + 图片预处理
│   ├── voice/                       # ASR 封装（已移除 UI，保留接口）
│   ├── data/                        # Room 数据库 + DAO + DataStore
│   └── common/                      # 安全存储（Keystore）+ 通用工具
├── feature/                         # 功能模块（占位，主界面在 app）
├── design/                          # UI 设计稿（HTML/CSS Mockup）
├── docs/                            # 需求 / 架构 / AI 方案 / 路线图
└── gradle/                          # Gradle Wrapper + Version Catalog
```

---

## 技术栈

| 层 | 技术 |
|----|------|
| UI | Jetpack Compose + Material 3 |
| 导航 | Compose Navigation |
| DI | Hilt |
| 数据库 | Room + DataStore |
| 安全 | Android Keystore + EncryptedSharedPreferences |
| 网络 | OkHttp + Kotlinx Serialization（直连 AI API，无中间服务） |
| 相机 | CameraX |
| 图片 | Coil 3 |
| AI | 统一适配层 → OpenAI 兼容协议（支持 DeepSeek / Kimi / 通义 / GLM / 自定义） |

---

## 文档索引

| 文档 | 描述 |
|------|------|
| [📋 需求分析](docs/01-requirements.md) | 市场分析、用户画像、功能需求 |
| [🎨 产品设计](docs/02-product-design.md) | 信息架构、交互流程、UI/UX |
| [🏗️ 技术架构](docs/03-architecture.md) | 系统架构、技术选型、数据模型 |
| [🤖 AI 集成方案](docs/04-ai-integration.md) | 多模型接入、Prompt 工程、视觉分析 |
| [⚙️ 功能实现](docs/05-feature-implementation.md) | 核心功能实现方案 |
| [🗺️ 路线图](docs/06-roadmap.md) | 里程碑 + 更新日志 |

---

## 为什么开源

传统闭源 App 让你「相信」它不会盗用 API Key。食记让你「验证」它不会：

| 可验证 | 方法 |
|--------|------|
| Key 存储 | 搜索 `EncryptedKeyStore` → Keystore 加密，无明文落盘 |
| 网络请求 | 检查 AI 适配层 → 只向用户选择的厂商发请求，无中转 |
| 数据不上传 | 全文搜索 `POST`/`PUT` → 零第三方服务器 |
| 权限使用 | 仅相机权限（拍照），无水印/后台上传 |

```
传统 App：「相信我」❌ 黑盒
食记：「验证我」✅ 每行代码可见
```
