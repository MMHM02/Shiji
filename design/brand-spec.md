# 食记 — Brand Spec

## 来源
mrjec9va-07-ui-design-brief.md（完整设计说明书）

## 色彩令牌

### Light Theme
| Token | Hex | 用途 |
|-------|-----|------|
| --bg | `#FAFAFA` | 页面背景 |
| --surface | `#FFFFFF` | 卡片、组件背景 |
| --surface-variant | `#F5F5F5` | 次级背景 |
| --fg | `#1C1B1F` | 主要文字 |
| --muted | `#49454F` | 次级文字 |
| --border | `#79747E` | 分割线、边框 |
| --accent | `#4CAF50` | 品牌主色（健康绿） |
| --accent-dark | `#388E3C` | 主色深 |
| --accent-light | `#C8E6C9` | 主色浅 |

### Dark Theme
| Token | Hex | 用途 |
|-------|-----|------|
| --bg | `#121212` | 页面背景 |
| --surface | `#1C1B1F` | 卡片背景 |
| --surface-variant | `#2B2930` | 次级背景 |
| --fg | `#E6E1E5` | 主要文字 |
| --muted | `#CAC4D0` | 次级文字 |
| --border | `#938F99` | 分割线 |

### 语义色
| Token | Hex | 用途 |
|-------|-----|------|
| --calories | `#FF6D00` | 热量 |
| --protein | `#E91E63` | 蛋白质 |
| --carbs | `#FFC107` | 碳水 |
| --fat | `#2196F3` | 脂肪 |
| --success | `#4CAF50` | 成功/达标 |
| --warning | `#FF9800` | 警告 |
| --error | `#F44336` | 错误/超标 |

## 字体
- Display / Body 中文: Noto Sans SC
- Display / Body 英文: Roboto
- Mono: Roboto Mono（数字/统计）

## 造型规则
- 圆角: 16dp（卡片标准）、12dp（输入框）、8dp（按钮/Chip）、24dp（弹窗）、全圆角（FAB/胶囊）
- 阴影: 极轻柔，仅浮层和弹窗使用
- 间距: 4dp 栅格（8/16/24/32/48）
- 图标: Material Icons（线性），24dp 导航、20dp 列表项、18dp 按钮内
- 动效: 微交互为主（缩放 96%、800ms 缓出圆环动画、列表过渡）
- 布局: 单屏单任务，大量留白，底部导航三 Tab
