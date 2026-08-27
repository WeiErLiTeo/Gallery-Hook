# GalleryHook 🪝 (System Media Interceptor)

<p align="center">
  <img src="app/src/main/res/mipmap-xxhdpi/ic_launcher.png" width="100" alt="GalleryHook Logo" />
</p>

<p align="center">
  <b>一款高性能 Android 系统媒体/相册/声音录制 Intent 拦截重定向与 ColorOS 专属选择器管理应用</b>
</p>

<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License"></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-1.9.0-blue.svg?logo=kotlin" alt="Kotlin"></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose" alt="Jetpack Compose"></a>
  <a href="https://www.android.com/"><img src="https://img.shields.io/badge/Platform-Android%208.0%2B-green.svg?logo=android" alt="Platform"></a>
  <a href="CHANGELOG.md"><img src="https://img.shields.io/badge/Version-1.1.0-orange.svg" alt="Version"></a>
</p>

---

## 📖 项目简介 (Overview)

**GalleryHook** 是一款轻量级、响应迅速的 Android 系统 Intent 拦截与媒体重定向工具。通过接管系统级的相册选择请求（`ACTION_GET_CONTENT` / `ACTION_PICK` / `ACTION_OPEN_DOCUMENT`）以及录音调用（`android.provider.MediaStore.RECORD_SOUND`），帮助用户在第三方应用（如微信、QQ、社交平台、特定浏览器等）请求选图或选取文件时：

1. **伪装系统相册选择器**：系统弹出二选一列表（原生相册 / GalleryHook），自由决定走向；
2. **专属定向调起**：支持一键直达 **ColorOS 专属相册 (`com.coloros.gallery3d`)**、**Google Photos (谷歌相册)**、**系统原生相册**或**文件管理器**；
3. **安全防循环机制**：自动识别并排除自身包名，彻底杜绝递归唤醒。

---

## 🌟 核心功能亮点 (Key Features)

- 📸 **伪装系统相册选择器 (Photo Picker Disguise)**
  - 注册标准 `ACTION_GET_CONTENT` 与 `ACTION_PICK` Filter。
  - 第三方应用唤起选图时，系统弹窗自动列出 GalleryHook 与原生相册，打破强制限制。
- 🎨 **ColorOS 专属相册支持 (ColorOS Gallery Direct-Launch)**
  - 深度适配 OPPO / OnePlus / Realme 等机型，定向直连 `com.coloros.gallery3d` 专属组件。
- 🎯 **五大拦截与重定向模式 (Interception Modes)**
  - **每次询问 (Prompt Every Time)**: 调起时弹出 Material Design 3 风格对话框供您实时选择。
  - **仅系统原生相册 (Always System Gallery)**: 自动调起 AOSP / Google 原生 Photo Picker。
  - **仅 ColorOS 相册 (Always ColorOS Gallery)**: 自动唤起 ColorOS 专属相册选择器。
  - **仅谷歌相册 (Always Google Photos)**: 自动重定向至 Google Photos 应用。
  - **仅系统文件管理器 (Always File Picker)**: 自动调起系统文件管理器选取任意文件。
- ⚡ **无感透明重定向 (Seamless & Transparent)**
  - 拦截界面全透明化，在选择完成后自动返回选择结果并安全销毁任务队列，零资源残留。
- 🤖 **GitHub Actions 手动云编译 (Automated Cloud CI/CD)**
  - 内置 `.github/workflows/build-apk.yml`，在 GitHub 仓库页面点击 **"Run workflow"** 即可一键自动编译并下载 Release APK。

---

## 🔬 技术原理 (Technical Architecture)

```
+-------------------------------------------------------------+
|                  Third-party App Request                    |
|       (ACTION_GET_CONTENT / ACTION_PICK / RECORD_SOUND)     |
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
  (Prompt / ColorOS / Gallery / File)        (SharedPreferences)
         |
  Exclude self-package & Launch Target
         |
  Return Activity.RESULT_OK with Grant URI
```

---

## 📂 项目结构 (Project Structure)

```
GalleryHook/
├── app/                        # 主应用模块
│   └── src/
│       ├── main/
│       │   ├── java/com/example/
│       │   │   ├── MainActivity.kt # 拦截响应、安全分发与配置主界面
│       │   │   └── ui/theme/       # Material Design 3 主题与色彩配置
│       │   └── res/            # Icon, Strings 与 AndroidManifest 配置
│       └── test/               # 单元测试与 Robolectric 测试
├── .github/
│   └── workflows/
│       └── build-apk.yml       # GitHub Actions 手动/自动编译 APK 工作流
├── CONTRIBUTING.md             # 开源贡献指南
├── CHANGELOG.md                # 版本更新日志
├── SECURITY.md                 # 安全政策
├── LICENSE                     # Apache 2.0 开源协议
└── README.md                   # 项目主文档
```

---

## 🚀 快速开始与构建说明 (Getting Started)

### 方式一：GitHub Actions 一键云编译（推荐）
1. 将本代码库 Fork 或推送至您的 GitHub 仓库；
2. 打开仓库顶部的 **Actions** 标签页；
3. 在左侧选择 **Build & Release APK** 工作流；
4. 点击右侧 **Run workflow**，选择 `release` 分支，点击执行；
5. 构建完成后，在页面下方的 **Artifacts** 区域即可一键下载已编译好的 APK 安装包。

### 方式二：本地 Gradle 编译
```bash
git clone https://github.com/your-username/GalleryHook.git
cd GalleryHook
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
