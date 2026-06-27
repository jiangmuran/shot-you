<div align="center">

[English](README.md) · **简体中文**

# Shot You

### 尽管拍。让 AI 替你留下唯一值得留的那张。

你随手连拍了五十张几乎一样的照片,最后只留下平庸的一张。
**Shot You** 读懂整组连拍,把该归到一起的归到一起,再把每组里最好的部分融合成一张
**自然、好看、看不出是 AI** 的照片——全程自带 Key、端上直连,无需后端。

[![Android CI](https://github.com/jiangmuran/shot-you/actions/workflows/build.yml/badge.svg)](https://github.com/jiangmuran/shot-you/actions/workflows/build.yml)
[![Release](https://github.com/jiangmuran/shot-you/actions/workflows/release.yml/badge.svg)](https://github.com/jiangmuran/shot-you/actions/workflows/release.yml)
[![Latest release](https://img.shields.io/github/v/release/jiangmuran/shot-you?sort=semver)](https://github.com/jiangmuran/shot-you/releases)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![minSdk 26](https://img.shields.io/badge/minSdk-26-3DDC84?logo=android&logoColor=white)
![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)

[下载 APK](https://github.com/jiangmuran/shot-you/releases/latest) ·
[反馈问题](https://github.com/jiangmuran/shot-you/issues) ·
[English](README.md)

</div>

---

## 痛点

手机拍二十张同一个瞬间很容易,挑出最好的那张却很痛苦:近似图来回滑、总有人眨眼、
姿势好的背景差,最后只能将就。其实每张照片都是有用的素材——只是没人有耐心去组合它们。

## Shot You 怎么做

```
   你的相册              端上处理               后台(用你的 AI Key)              你来决定
 ────────────       ────────────────        ───────────────────────────      ─────────────────
  点选/拖动批量   →   缩放 + 分批           →   VLM 聚类近似图、打分类、       →   策展:保留、
  选一堆照片          (滑动窗口,不卡 UI)      建议哪些值得生成                   取消冗余、放大、
                                                       │                          改提示词
                                                       ▼
                                          每个保留的组生成 3 个候选
                                          (保守 / 中立 / 大胆)——          →   滑动比选、保留、
                                          参考图通过 images/edits              提建议迭代、
                                          真正喂给模型                          其余移入回收站
```

分类和生成都是**后台会话**,在可暂停/继续的队列里运行,进度显示在状态栏——它干活时你照样拍。

---

## 亮点

| | |
|---|---|
| **永不卡死** | 分类是对 VLM 的滑动窗口、**并发 + 重试**;几百张也不卡,进度在分阶段队列和状态栏。 |
| **花钱花在刀刃上** | VLM 给每组打**分类**和**是否值得生成**的建议,冗余组在你付费生成前就被标出。先策展、放大、改词。 |
| **有据可依的生成** | 参考图通过 `images/edits` **真正发给模型**,结果忠于真实主体而非瞎编;画幅自动匹配原图方向。 |
| **三版任你选** | 每组产出**保守 / 中立 / 大胆**三个候选,提示词差异明显。滑动对比、任意保留,或**提建议**迭代。 |
| **美而可信** | 提示词由"专业人像摄影师"人设打磨:真实构图、光线与肤质,刻意避开塑料"AI 感"。 |
| **按分类配置风格** | 人物→人像、风景→旅行……每类可指定风格;另有 真实/美化/电影感/清新/艺术 预设 + 强度滑块。 |
| **自带 Key,OpenAI 兼容** | 任意 **OpenAI 格式**接口 + **自定义 API host**(官方/中转/本地)。Key 仅存本地,无后端。 |
| **真实用量统计** | 用量看板按你设的每百万 token / 每张图单价,统计调用数、token、图片数与预估花费。 |
| **温和保活** | 前台服务生成 + 状态栏进度;可选 **root** Doze 白名单,几乎不耗电。 |
| **安全可恢复** | 原图移入系统**回收站**(可恢复),绝不硬删。完整相册权限,含 Android 14 部分选择。 |
| **精致** | Compose + Material 3、考究配色、双指缩放、edge-to-edge、深色模式;应用内中英文,提示词跟随语言。 |

---

## 使用流程

1. 在相册**选图**——点选或按住拖动批量刷选,点**用 AI 分类**;已处理的照片会被标记。
2. 分类在**后台**进行;该会话出现在**队列**里,经历 *分类中 → 待决策 → 生成中*,状态栏实时显示进度。
3. 打开**就绪**的会话**策展**:每组显示分类、缩略图、是否建议跳过、可编辑提示词。放大查看、取消不想要的,点**开始**。
4. 每个保留的组生成**三个候选**。进去**滑动**比选,保留喜欢的,或**提建议**从当前版本迭代出新候选。
5. 最后把**多余的原图移入回收站**——或只保留一张。

---

## 架构

单模块、清晰 MVVM,按层划分:

```
ui/         Compose 界面 + ViewModel(相册、队列、策展、候选、结果、模板、用量、设置)+ 导航 + 主题
domain/     模型、仓库接口、AI 契约、PromptComposer、会话
data/
  local/    Room(模板/任务/用量/会话)+ 迁移
  remote/   OpenAI 兼容客户端(视觉对话、images/edits)、AiProviderFactory
  repository/  仓库实现
  settings/ DataStore 设置
work/       WorkManager:ClassificationWorker + GenerationWorker(前台)
di/         Hilt 模块
```

- **Hilt** 依赖注入,**Room** + **DataStore** 持久化,**WorkManager** 跑后台会话,
  **Retrofit/OkHttp** + **kotlinx.serialization** 网络,**Coil** 图片。
- AI 层抽象在 `VlmProvider` / `LlmProvider` / `ImageGenProvider` + `AiProviderFactory` 之后,新增提供商只需一个类。
- **滑动窗口 + 并查集合并**:大批量照片既不超模型单次上限,也不会把一组拆到两次请求。

> **开放式构建,由一队 agent 完成。** 先打地基与契约,再在隔离的 git worktree 里并行开发、
> 合并,全程以 GitHub Actions 作为构建闸门,并在版本之间用对抗式审查找 bug。提交历史就是构建日志。

---

## 开始使用

### 安装
到 [Releases](https://github.com/jiangmuran/shot-you/releases/latest) 下载最新 **APK** 安装
(也可从 [CI 产物](https://github.com/jiangmuran/shot-you/actions) 下载)。

### 接上你的 AI
打开**设置**填写:

- **API Host**——默认 `https://api.openai.com/v1`,可改成任意 OpenAI 兼容地址(官方/中转/本地)。
- **API Key**——仅存本地。
- **模型**——VLM(分类)、LLM(提示词优化)、图像 分别配置(默认 `gpt-4o`、`gpt-4o-mini`、`gpt-image-2`)。
- 可选:**单价**(每百万 token / 每张图)、**按分类风格**、队列并发与节流、保活、语言。

> VLM 模型必须支持**图片输入(视觉)**。用中转 host 时,务必把 VLM 模型名设成支持视觉的型号,
> 否则分类阶段"看不到"照片,会出现猫被认成人之类的瞎编。任何实现 OpenAI `chat/completions`
> 与 `images/edits` 的服务都能接入。

---

## 从源码构建

```bash
git clone https://github.com/jiangmuran/shot-you.git
cd shot-you
./gradlew assembleDebug    # 产物:app/build/outputs/apk/debug/app-debug.apk
```

需要 **JDK 17** 与 Android SDK(**compileSdk 35**)。每次推送都会 CI 构建,每个 tag 发布签名 APK。

---

## 路线图

- [x] 后台分类会话;可暂停的分阶段队列 + 状态栏进度
- [x] 先策展再花钱;三候选(保守/中立/大胆);回收站
- [x] 按分类风格规则;OpenAI 兼容自定义 host;中英文;用量 + 单价
- [ ] 结果页前后对比
- [ ] 端上感知哈希预聚类,降低大批量的 VLM 成本
- [ ] 局部重绘 / 定点编辑("只修一下眼睛")

欢迎 issue 与 PR。

## 许可证

[MIT](LICENSE) © 2026 jiangmuran
