
# Aether — Frontend<img width="280" height="275" alt="屏幕截图 2026-04-19 220409" src="https://github.com/user-attachments/assets/d315027a-376b-40d9-af9f-4ce12b643eb6" />



> *Ambient presence. Quiet intelligence. Always with you.*

---

## 这是什么

<img width="2274" height="857" alt="屏幕截图 2026-04-19 014408" src="https://github.com/user-attachments/assets/bb6a693c-1968-4531-a4c3-3177f956bde5" />


这是 Aether App 的前端代码库。

Aether 是一款 AI 智能眼镜配套应用，内置 Proactive AI Agent，拥有陪伴用户 3–5 年的长期私人记忆。它不是工具，它是一个会记得你的伙伴。

但在我们开始讲技术之前，我们想先说一件更重要的事——

---

## 我们为什么造这个

我们相信，这个时代对效率的推崇已经走得太远了。

每一款 App 都在争夺你的注意力，每一个通知都在提醒你"还有什么事没做"，我们随身携带的设备，正在悄悄把我们变成一台永不停机的生产机器。我们的大脑 24 小时在线，却越来越少感受到活着本身的重量——一顿饭的温度，一个老友的眼神，一个清晨窗外的光。

Aether 的出发点很简单：**让 AI 来做"工具"的那部分，让你重新做一个人。**

你的外接大脑替你记住每一件事，替你管理时间、整理信息、预判下一步——而你，终于可以抬起头，热烈地感受这个世界。不是效率更高的你，是更完整的你。

---

## 设计的温度

前端从来不只是视觉。它是你与用户之间最直接的握手。

Aether 的界面遵循一套我们称之为 **"以太设计语言"** 的系统，它的每一个细节都在回答同一个问题：*此刻，这里应该有多安静？*

### 色彩：墨与奶油

主色盘不用冷蓝，不用科技感的霓虹。我们选择了一组温暖的单色调——


<img width="2247" height="1308" alt="屏幕截图 2026-04-19 014427" src="https://github.com/user-attachments/assets/ec56e91e-96f3-45aa-a02c-b43e863f7318" />


点缀色只有一种：**黎明渐变**（Dawn Gradient）。从破晓前的紫褐，穿过燃烧的琥珀，化入柔和的蜜桃与雾光——


```
#2E2438 → #B85C3C → #E8A57A → #FAEBD0
```

这道渐变每屏只用一次，像清晨的第一缕光，用完即止。永远从左到右，永远温暖，从不反转。

<img width="2288" height="1150" alt="屏幕截图 2026-04-19 014432" src="https://github.com/user-attachments/assets/caa26703-6d16-4c3a-9f75-2e80b02d8a0f" />

### 字体：一个会呼吸的衬线，一个会倾听的黑体

- **Fraunces** — 展示级标题，可变字体，笔触柔和，带着一点老派的诚恳
- **Inter** — 正文与 UI，中性、清晰、不抢戏
- **Noto Serif SC / Noto Sans SC** — 中文双线，分别对应以上两种场景
- **JetBrains Mono** — 仅用于时间戳、标签、数据等"低温"信息

字重克制，排列疏朗。我们不希望用户"阅读"界面，我们希望界面像空气一样，被感受，而不是被看见。

### 语气：像一位有分寸的朋友

Aether 说话的方式遵循三条原则——

1. **先观察，再开口。** 它注意到了，但它等你准备好。
2. **提问，而不是假设。** "要我把通知静音到中午吗？"——不是"我帮你静音了。"
3. **保持阅读音量。** 永远不是广播，永远是低声耳语。

---

## 技术概览

```
/
├── app/               # 路由与页面
├── components/        # UI 组件库
│   ├── agent/         # Agent 状态 · 呼吸球 · 消息流
│   ├── memory/        # 记忆卡片 · 时间线
│   └── ui/            # 基础组件
├── styles/            # 设计 Token · 全局样式
│   └── tokens.css     # 品牌色 · 字体 · 圆角 · 间距
├── lib/               # 工具函数
└── public/            # 静态资源
```

### 设计 Token 速查

```css
/* 主色 */
--ink-900:   #1A1614;   /* 主文字 */
--cream-50:  #FAF6F0;   /* 主背景 */

/* 强调 */
--dawn-ember: #B85C3C;  /* 主强调色 · 慎用 */
--dawn-peach: #E8A57A;  /* 次强调 */

/* 字体 */
--font-display: "Fraunces", "Noto Serif SC", serif;
--font-body:    "Inter", "Noto Sans SC", sans-serif;
--font-mono:    "JetBrains Mono", monospace;
```

### 快速开始

```bash
git clone https://github.com/your-org/aether-app.git
cd aether-app
npm install
npm run dev
```

---

## 贡献指南

在你提交第一行代码之前，我们希望你先问自己一个问题：

> **这个改动，让界面更安静了，还是更嘈杂了？**

Aether 的设计原则不是"更多功能"，而是"恰好够用，多一点都是打扰"。如果一个交互让用户需要思考它，那它就还不够好。

具体规范请参阅 [`CONTRIBUTING.md`](./CONTRIBUTING.md) 与 [`DESIGN_SYSTEM.md`](./DESIGN_SYSTEM.md)。

---

## 品牌设计系统

完整的品牌设计文档（色彩、字体、Logo 变体、语气指南）见 `/design/brand-system-v1.html`，或联系设计团队获取最新版本。

---

<br>

*"The best interface is the one that dims its own light when you don't need it, and glows when you do."*

<br>

---

`Aether · Frontend · v1.0 · April 2026`
