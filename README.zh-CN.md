<div align="center">

[English](README.md) · **简体中文**

# 📸 Shot You

### 拍一堆,让 AI 帮你留最好的那张。

*你随手连拍了五十张几乎一样的照片。Shot You 找出这些近似图、理解它们,再把每组里最好的部分融合成一张完美的照片。*

[![Android CI](https://github.com/jiangmuran/shot-you/actions/workflows/build.yml/badge.svg)](https://github.com/jiangmuran/shot-you/actions/workflows/build.yml)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![minSdk](https://img.shields.io/badge/minSdk-26-3DDC84?logo=android&logoColor=white)
![License](https://img.shields.io/badge/license-MIT-blue.svg)

</div>

---

## ✨ 它能做什么

你拍了一组连拍 —— 同一个人、同一个地方、姿势略有不同,其中三张还闭着眼。平时你得来回滑动、放大纠结,最后只留下一张平庸的。

**Shot You** 替你来想:

1. 从相册里**批量选中**一堆照片。
2. **视觉语言模型(VLM)** 把近似的照片聚成一组(连拍 / 同人 / 同景),并解释*为什么*归到一起。
3. 自动**挑出每组里最好的几张作为参考帧**。
4. 你加上**模板提示词** —— 从模板库里选,或自己写一句让大模型**优化**。发型、表情、姿势、位置都可调。
5. **图像模型融合**参考图 + 提示词,生成一张精修照片。
6. 不满意?**重新生成。** 全程走**后台队列**,用起来顺滑无感,还有**用量看板**记录每一次调用。

---

## 🧩 功能

| | |
|---|---|
| 🖼️ **完整相册权限** | 现代 Photo Picker + 媒体权限,含 Android 14 的*部分(仅选定照片)*访问,并向下兼容到 Android 8。 |
| 🧠 **智能分组** | VLM 聚类近似照片,给出每组的标题、理由和参考图建议。 |
| 🎯 **参考图挑选** | 每组的最佳帧默认选中,可手动增删。 |
| ✍️ **提示词融合** | 模板库(含内置)+ 一键**大模型优化**;发型 / 表情 / 姿势 / 位置 快捷词。 |
| 🎨 **风格与强度** | 真实 / 美化 / 电影感 / 清新 / 艺术 等风格预设 + 强度滑块;默认风格可在设置里配置。 |
| 🌅 **生成与重生成** | 参考图 + 提示词融合出图;不满意可在结果页提建议迭代;自动存相册、可分享。 |
| 🛰️ **后台队列 / 常驻** | WorkManager 队列,可配并发、请求间隔、重试、仅 Wi-Fi;可选**后台常驻 + 进度通知**。 |
| 📊 **用量看板** | 按调用类型统计调用数、token、图片数与估算费用。 |
| 🔌 **自带 Key,OpenAI 兼容** | 兼容任意 **OpenAI 格式**接口,**API host 可自定义**(官方 / 中转 / 本地模型皆可)。Key 仅存本地,无需后端。 |
| 🌐 **中英文切换** | 应用内一键切换 简体中文 / English / 跟随系统。 |
| 💎 **现代 UI** | Jetpack Compose、**Material 3**、动态取色、edge-to-edge、深色模式。 |

---

## 🚀 快速开始

### 安装
到 [Releases](https://github.com/jiangmuran/shot-you/releases) 下载最新的 **`shot-you-*.apk`** 安装即可
(也可从 [CI 产物](https://github.com/jiangmuran/shot-you/actions) 下载)。

### 配置接口
打开 **设置**,填入:

- **API Host** —— 默认 `https://api.openai.com/v1`,可改成任意 OpenAI 兼容地址(中转 / 自建 / 本地)。
- **API Key** —— 仅存本地,不上传。
- **模型名** —— 分别配置 VLM(分组)、LLM(提示词优化)、图像 三个模型。

> 任何兼容 OpenAI `chat/completions` 与 `images/generations` 协议的服务都能接入。

### 使用
**相册** → 多选(支持按住滑动批量选)→ **AI 分组** → 打开一组 → 选模板 / 写并优化提示词、选风格强度 → **生成** → 在 **队列** 里看进度 → 打开**结果** → 不满意就提建议重生成。

---

## 🔨 从源码构建

```bash
git clone https://github.com/jiangmuran/shot-you.git
cd shot-you
./gradlew assembleDebug      # 产物:app/build/outputs/apk/debug/app-debug.apk
```

需要 **JDK 17** 与 Android SDK(**compileSdk 35**)。每次推送 CI 都会自动编译 APK。

---

## 📄 许可证

[MIT](LICENSE) © 2026 jiangmuran
