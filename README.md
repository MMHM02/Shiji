# 🍽️ 食记 — AI 驱动的饮食记录与健康数据看板

<p align="center">
  <img src="https://img.shields.io/badge/license-Apache%202.0-blue" alt="License">
  <img src="https://img.shields.io/badge/platform-Android%208.0%2B-green" alt="Platform">
  <img src="https://img.shields.io/badge/language-Kotlin-purple" alt="Language">
  <img src="https://img.shields.io/badge/architecture-MVVM%20%2B%20Clean-orange" alt="Architecture">
</p>

## 项目概述

**食记** 是一款 **完全开源** 的 Android 端饮食记录应用。用户可以自行接入国内大语言模型（LLM）API Key，利用 AI 视觉模型进行食物拍照识别与卡路里分析、利用语音识别进行口述饮食记录，并基于 AI 提供个性化的健康建议。

> 🔓 **开源承诺**：代码 100% 公开可审计。API Key 如何存储、网络请求发往何处、数据是否上传 —— 每一行代码都可以验证，无需信任，只需核实。

| 属性 | 说明 |
|------|------|
| **项目名称** | 食记 |
| **平台** | Android 8.0+ (API 26+) |
| **开发语言** | Kotlin |
| **UI 框架** | Jetpack Compose |
| **最低 SDK** | 26 (Android 8.0 Oreo) |
| **目标 SDK** | 35 (Android 15) |
| **架构模式** | MVVM + Clean Architecture |
| **许可证** | Apache 2.0 |
| **代码仓库** | GitHub（开源） |

---

## 核心特色

```
┌─────────────────────────────────────────────────────┐
│                   🧠 AI 驱动核心                      │
│  ┌──────────┐  ┌──────────┐  ┌──────────────────┐  │
│  │ 拍照识食  │  │ 语音记录  │  │ 个性化健康建议    │  │
│  │ 视觉模型  │  │ ASR+TTS  │  │ 多模态分析        │  │
│  └──────────┘  └──────────┘  └──────────────────┘  │
│                        │                            │
│        用户自有 API Key（支持多厂商）                 │
│    OpenAI · Anthropic · Google AI · 国产模型         │
└─────────────────────────────────────────────────────┘
```

---

## 文档索引

| 文档 | 路径 | 描述 |
|------|------|------|
| 📋 需求分析 | [`docs/01-requirements.md`](docs/01-requirements.md) | 市场分析、用户画像、功能需求、非功能需求 |
| 🎨 产品设计 | [`docs/02-product-design.md`](docs/02-product-design.md) | 信息架构、交互流程、UI/UX 设计规范 |
| 🏗️ 技术架构 | [`docs/03-architecture.md`](docs/03-architecture.md) | 系统架构、技术选型、模块划分、数据模型 |
| 🤖 AI 集成方案 | [`docs/04-ai-integration.md`](docs/04-ai-integration.md) | 多模型接入、API Key 管理、Prompt 工程、视觉分析 |
| ⚙️ 功能实现 | [`docs/05-feature-implementation.md`](docs/05-feature-implementation.md) | 核心功能详细实现方案、关键代码设计 |
| 🗺️ 开发路线图 | [`docs/06-roadmap.md`](docs/06-roadmap.md) | 里程碑、迭代计划、技术债务管理 |

---

## 快速开始（开发阶段）

```bash
# 克隆项目
git clone <repo-url> fitness
cd fitness

# 使用 Android Studio Hedgehog (2024.1.1+) 打开项目
# Sync Gradle → Run on emulator/device
```

---

## 项目结构（规划）

```
fitness/
├── app/                         # 主应用模块
│   ├── src/main/java/com/fitness/
│   │   ├── MainActivity.kt
│   │   ├── FitnessApp.kt        # Application 类
│   │   ├── di/                  # 依赖注入模块
│   │   ├── data/                # 数据层
│   │   │   ├── local/           # Room DAO, Entities
│   │   │   ├── remote/          # API 服务, AI 适配器
│   │   │   └── repository/      # 仓库实现
│   │   ├── domain/              # 领域层
│   │   │   ├── model/           # 领域模型
│   │   │   ├── usecase/         # 用例
│   │   │   └── repository/      # 仓库接口
│   │   └── ui/                  # 展示层
│   │       ├── navigation/      # 导航图
│   │       ├── theme/           # 主题/设计令牌
│   │       ├── home/            # 首页仪表盘
│   │       ├── diet/            # 饮食模块
│   │       ├── health/          # 健康追踪
│   │       ├── ai/              # AI 对话/分析
│   │       ├── settings/        # 设置（含API Key管理）
│   │       └── components/      # 通用组件
│   └── src/main/res/            # 资源文件
├── core/                        # 核心工具库
│   ├── ai/                      # AI 适配层
│   ├── camera/                  # 相机封装
│   ├── voice/                   # 语音服务封装
│   └── common/                  # 通用工具
├── docs/                        # 技术文档（当前目录）
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 技术栈一览

| 层 | 技术 | 说明 |
|----|------|------|
| **UI** | Jetpack Compose + Material 3 | 声明式 UI，现代化设计 |
| **架构** | MVVM + Clean Architecture | 分层解耦，可测试 |
| **导航** | Compose Navigation | 类型安全的导航 |
| **数据库** | Room | SQLite 抽象层 |
| **网络** | Retrofit + OkHttp + Kotlinx Serialization | HTTP 客户端 |
| **DI** | Hilt | 编译时依赖注入 |
| **相机** | CameraX | Jetpack 相机库 |
| **语音** | Android SpeechRecognizer + MediaPlayer | 语音识别与合成 |
| **图片加载** | Coil 3 | Compose 原生图片加载 |
| **图表** | Vico | Compose 图表库 |
| **AI** | 多厂商适配层（OpenAI / Anthropic / Google AI / 国产） | 统一接口 |
| **测试** | JUnit5 + MockK + Compose Test + Turbo | 全覆盖测试 |
| **CI/CD** | GitHub Actions | 自动化构建 |

---

---

## 🔓 为什么开源？

市面上绝大多数健康类应用都是闭源的。当你把 API Key 或饮食数据输入一个黑盒应用时，你无法确认它是否在后台偷偷上传。

Fitness 选择开源，意味着：

| 你可以验证的 | 具体做法 |
|-------------|----------|
| **API Key 去向** | 搜索 `EncryptedKeyStore`，确认 Key 只存在本地加密存储 |
| **网络请求终点** | 检查 AI 适配层代码，确认只向用户选择的 AI 厂商发请求 |
| **数据不上传** | 全局搜索 `POST`/`PUT` 调用，确认无数据回传第三方服务器 |
| **权限使用** | 相机/录音权限仅在拍照/语音功能时调用，无后台滥用 |
| **依赖安全** | 所有第三方库版本透明，SBOM 可生成 |

```
开源信任模型：
  传统应用：  "相信我"  ❌ 黑盒
  食记：     "验证我"  ✅ 每行代码可见
```

> 💡 **核心理念**：你的 API Key，你的数据，你的 AI 选择。Fitness 不做中间人，代码公开可查。
