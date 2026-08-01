# GalleryHook 🪝 (System Media Interceptor)

<p align="center">
  <img src="app/src/main/res/mipmap-xxhdpi/ic_launcher.png" width="100" alt="GalleryHook Logo" />
</p>

<p align="center">
  <b>一款高性能 Android 系统媒体/声音录制 Intent 拦截重定向与选择器管理应用</b>
</p>

<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License"></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-1.9.0-blue.svg?logo=kotlin" alt="Kotlin"></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose" alt="Jetpack Compose"></a>
  <a href="https://www.android.com/"><img src="https://img.shields.io/badge/Platform-Android%208.0%2B-green.svg?logo=android" alt="Platform"></a>
  <a href="CHANGELOG.md"><img src="https://img.shields.io/badge/Version-1.0.0-orange.svg" alt="Version"></a>
</p>

---

## 📖 项目简介 (Overview)

**GalleryHook** 是一款轻量级、响应迅速的 Android 系统 Intent 拦截与媒体重定向工具。通过接管系统级的 `android.provider.MediaStore.RECORD_SOUND` 等调起请求，帮助用户在第三方应用（如社交软件、论坛或特定平台）请求选取文件或音频时，无缝重定向至**系统相册**、**文件管理器**或 **Google Photos (谷歌相册)**。

打破第三方应用强制调用特定系统选择器的限制，实现自由、优雅的媒体选取重定向体验。

---

## 🌟 核心功能亮点 (Key Features)

- 🎯 **灵活拦截模式 (Interception Modes)**
  - **每次询问 (Prompt Every Time)**: 调起时弹出 Material Design 3 风格弹窗，供用户实时选择来源。
  - **仅系统相册 (Always Gallery)**: 自动调起系统相册选取图片。
  - **仅文件管理器 (Always File Picker)**: 自动调起系统文件管理器选取任意文件。
  - **仅谷歌相册 (Always Google Photos)**: 自动重定向至 Google Photos 应用。
- ⚡ **无感透明重定向 (Seamless & Transparent)**
  - 拦截界面全透明化，在选择完成后自动返回选择结果并安全销毁任务队列，零资源残留。
- 🎨 **沉浸式 MD3 视觉 (Material Design 3 Theme)**
  - 采用 Edge-to-Edge 沉浸式状态栏与导航栏设计，支持 Android 12+ 动态取色 (Dynamic Color) 与暗色模式。
- 🔒 **权限与安全控制 (Security & Permissions)**
  - 严格使用 standard Intent Result Code 传递 Uri，并自动授予读权限 (`FLAG_GRANT_READ_URI_PERMISSION`)。

---

## 🔬 技术原理 (Technical Architecture)

`GalleryHook` 借助于 Android Activity 隐式 Intent 机制注册高优先级的 Action 过滤器，并在入口点解析事件上下文：

```
+-------------------------------------------------------------+
|                  Third-party App Request                    |
|             (android.provider.MediaStore.RECORD_SOUND)      |
+------------------------------+------------------------------+
                               |
                               v
                     [ GalleryHook MainActivity ]
                               |
         +---------------------+---------------------+
         |                                           |
         v                                           v
 [ InterceptScreen ]                         [ ConfigScreen ]
 (Intent Triggered)                          (App Launch)
         |                                           |
  Check Intercept Mode                       Mode Switch & Storage
  (Prompt / Gallery / File / Google Photos)  (SharedPreferences)
         |
  ActivityResultLauncher Launch
         |
  Return Activity.RESULT_OK
```

---

## 📂 项目结构 (Project Structure)

```
GalleryHook/
├── app/                        # 主应用模块
│   └── src/
│       ├── main/
│       │   ├── java/com/example/
│       │   │   ├── MainActivity.kt # 拦截响应与配置主界面
│       │   │   └── ui/theme/       # Material Design 3 主题与色彩配置
│       │   └── res/            # Icon, Strings 与 AndroidManifest 配置
│       └── test/               # 单元测试与 Robolectric 测试
├── .github/                    # Issue 模板与 CI 配置
├── CONTRIBUTING.md             # 开源贡献指南
├── CHANGELOG.md                # 版本更新日志
├── SECURITY.md                 # 安全政策
├── LICENSE                     # Apache 2.0 开源协议
└── README.md                   # 项目主文档
```

---

## 🚀 快速开始与构建说明 (Getting Started)

### 环境要求 (Prerequisites)
- **Android Studio**: Iguana (2023.2.1) 或更高版本
- **JDK Version**: JDK 17
- **Target SDK**: Android 14 (API 34)
- **Min SDK**: Android 8.0 (API 26)

### 本地编译步骤 (Build Steps)

1. 克隆代码库：
   ```bash
   git clone https://github.com/your-username/GalleryHook.git
   cd GalleryHook
   ```

2. 使用 Gradle 编译 Debug/Release APK：
   ```bash
   ./gradlew assembleRelease
   ```
   编译生成的 APK 位于：`app/build/outputs/apk/release/app-release.apk`

---

## 🤝 贡献与社区 (Contributing)

非常欢迎任何形式的贡献！在提交 Issue 或 Pull Request 前，请先阅读我们的 [CONTRIBUTING.md](CONTRIBUTING.md)。

- 🐛 发现 Bug？提交 [Bug Report](.github/ISSUE_TEMPLATE/bug_report.md)
- 💡 有新想法？提交 [Feature Request](.github/ISSUE_TEMPLATE/feature_request.md)

---

## 📄 开源协议 (License)

根据 [Apache License 2.0](LICENSE) 协议开源。
