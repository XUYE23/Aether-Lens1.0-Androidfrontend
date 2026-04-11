# Aether AI 管家 App - 设计文档

## 项目概述

**项目名称**：Aether (以太)
**定位**：革命性个人 AI 管家，类似钢铁侠 Jarvis 的专属高定数字生命
**设计哲学**：隐形设计 (Invisible Design) - 极简、深邃、物理直觉

## 技术栈

- **语言**：100% Kotlin
- **UI 框架**：Jetpack Compose
- **架构模式**：MVI (Model-View-Intent)
- **状态管理**：ViewModel + StateFlow
- **网络通信**：REST API + SSE (Server-Sent Events)
- **最低 SDK**：API 26 (Android 8.0)
- **目标 SDK**：API 34 (Android 14)

## 品牌视觉规范

### 颜色系统
- **背景色**：True Black `#000000`
- **品牌渐变**：深海蓝 `#0A1929` → 霓虹紫 `#7C3AED`
- **成功/确认**：蓝绿光 `#00D9FF`
- **拒绝/警告**：尘土红 `#FF4444`
- **危险模式**：琥珀色 `#FFA726`

### 视觉元素
- **磨砂玻璃质感** (Glassmorphism)
- **细微金属边缘描边**
- **流体呼吸光晕动画**
- **物理厚度感卡片**

### 动画规范
- **禁止线性动画**：所有位移使用弹簧物理动画 (spring spec)
- **触觉反馈**：每次交互配合线性马达震动
- **弹簧参数**：stiffness = 300f, dampingRatio = 0.7f

## 核心架构

### 双空间设计

**Space 1: The Ambient Hub & Action Deck (左侧主页)**
- 上半部：流体呼吸光晕 + AI 状态摘要
- 下半部：待确认任务卡片堆栈

**Space 2: The Personal Nexus (右侧个人空间)**
- 用户昵称：Pain
- 晶体标签：用户画像展示
- 功能矩阵：已授权生态 + 危险模式开关

### 核心交互：Tinder 式滑动

- **右滑**：发送/放行（边缘泛蓝绿光）
- **左滑**：拒绝/销毁（边缘泛红色）
- **拖动效果**：卡片微缩 + 倾斜角度
- **释放反馈**：剧烈震动 + 弹簧飞出动画
- **滑动阈值**：屏幕宽度的 30%

## 第一里程碑目标

### 1. 基础工程结构
- ✅ 标准 Android Studio 项目
- ✅ Gradle 配置 + 依赖管理
- ✅ Compose 主题系统（强制 Dark Theme）
- ✅ MVI 架构基础类

### 2. 配对启动仪式 (The Awakening)
- ✅ 纯黑背景
- ✅ 线条收束成圆环动画
- ✅ 重震动触发
- ✅ 文字浮现："神经链路已建立。绑定完成，专属 AI 入驻。"
- ✅ 平滑过渡到主空间
- **触发时机**：仅首次启动

### 3. Swipe 卡片组件
- ✅ 高度可复用的 Composable
- ✅ 拖动位移计算
- ✅ 旋转角度计算
- ✅ 左右滑出阈值回调
- ✅ Mock 飞书消息草稿展示

## 数据层设计

### Mock 数据结构

```kotlin
data class ActionCard(
    val id: String,
    val type: String, // "message", "email", "calendar"
    val title: String,
    val content: String,
    val timestamp: Long,
    val metadata: Map<String, Any>
)
```

### 预留 API 接口

```kotlin
interface AetherApiService {
    suspend fun getActionCards(): List<ActionCard>
    suspend fun confirmAction(cardId: String): Result<Unit>
    suspend fun rejectAction(cardId: String): Result<Unit>
}
```

## 实施优先级

1. **P0**：项目结构 + 主题系统
2. **P0**：Swipe 卡片组件（核心交互）
3. **P1**：配对启动仪式
4. **P2**：双空间导航
5. **P3**：Personal Nexus 页面

## 成功标准

- ✅ 卡片滑动流畅（60fps）
- ✅ 触觉反馈清晰可感知
- ✅ 动画符合物理直觉
- ✅ 视觉符合品牌规范
- ✅ 代码结构清晰可维护

---

**文档版本**：v1.0
**创建日期**：2026-04-01
**作者**：Claude (Brainstorming Phase)
