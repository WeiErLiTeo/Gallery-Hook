# GalleryHook

<p align="center">
  <img src="app/src/main/res/mipmap-xxhdpi/ic_launcher.png" width="96" alt="GalleryHook Icon" />
</p>

<p align="center">
  <b>Android 平台轻量级多媒体与相册选择重定向工具</b>
</p>

<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License"></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Language-Kotlin-7F52FF.svg?logo=kotlin&logoColor=white" alt="Kotlin"></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white" alt="Jetpack Compose"></a>
  <a href="CHANGELOG.md"><img src="https://img.shields.io/badge/Version-1.2.0-green.svg" alt="Version"></a>
</p>

---

## 概述

**GalleryHook** 是一款针对 Android 系统的媒体选择重定向工具。通过注册系统标准隐式 Intent 过滤器，在第三方应用发起媒体文件选择请求时作为选择器候选呈现，允许用户将选图请求透明分发至指定的相册应用（如 ColorOS 相册、Google Photos 或系统原生 Photo Picker），并在选图完成后将媒体数据回调给调用方并立即销毁自身进程。

---

## 核心特性

- **标准 Intent 劫持与分发**：注册响应 `ACTION_GET_CONTENT`、`ACTION_PICK`、`ACTION_OPEN_DOCUMENT` 及 `ACTION_PICK_IMAGES`。
- **目标相册定向**：
  - **每次询问**：唤起对话框供用户即时选择目标相册组件。
  - **ColorOS / OPlus 相册**：定向唤起 `com.coloros.gallery3d`。
  - **Google Photos**：定向唤起 `com.google.android.apps.photos`。
  - **系统原生相册**：调用 Android Photo Picker / MediaStore。
- **无状态与零后台开销**：
  - 采用无常驻设计，选图流程结束（成功、取消或异常）即刻触发 `finishAndRemoveTask()` 并调用 `Process.killProcess()` 物理终止自身进程。
  - 调起外部相册时自动过滤自身包名，杜绝 Intent 递归死循环。
- **安全与权限透明**：
  - 通过 `FLAG_GRANT_READ_URI_PERMISSION` 透传 URI 读取权限，无需申请额外存储敏感权限。

---

## 系统工作原理

```
+--------------------------------------------------------------------+
|                       调用方应用 (Client App)                        |
|   发起隐式 Intent (ACTION_GET_CONTENT / ACTION_PICK / OPEN_DOCUMENT)  |
+---------------------------------+----------------------------------+
                                  |
                                  v
+--------------------------------------------------------------------+
|                  Android 系统选择器 (ResolverActivity)                |
|                    [ 系统媒体 / GalleryHook / 相机 ]                 |
+---------------------------------+----------------------------------+
                                  | 用户选择 GalleryHook
                                  v
+--------------------------------------------------------------------+
|                       GalleryHook MainActivity                     |
|  - 读取持久化配置 (SharedPreferences: intercept_mode)                 |
|  - 构造显式 Intent (ColorOS / Google Photos / 原生相册)              |
|  - 启动目标 Activity (ActivityResultContracts)                      |
+---------------------------------+----------------------------------+
                                  |
                                  v
+--------------------------------------------------------------------+
|                         目标相册应用                                 |
|  - 用户完成选图，回传 ClipData / Uri                                 |
+---------------------------------+----------------------------------+
                                  |
                                  v
+--------------------------------------------------------------------+
|                       GalleryHook 回调处理                         |
|  - setattr Intent(RESULT_OK, data, FLAG_GRANT_READ_URI_PERMISSION) |
|  - finishAndRemoveTask()                                           |
|  - Process.killProcess(Process.myPid()) (彻底终止进程)             |
+--------------------------------------------------------------------+
```

---

## Android 13+ Photo Picker 机制说明

### 1. 标准 `ACTION_GET_CONTENT` / `ACTION_PICK`
在此类接口中，系统会正常触发应用选择列表，用户可直接选择 **GalleryHook** 进行路由。

### 2. 现代 `MediaStore.ACTION_PICK_IMAGES` (`PickVisualMedia`)
在 Android 13 (API 33) 及更高版本中，部分应用（如 Gemini、ChatGPT 等）直接调用系统级 Photo Picker 组件 (`com.google.android.providers.media.module`)。
- **无 Root 环境**：系统框架层将此类请求直接路由给系统组件，第三方应用无法强制覆盖系统签名级的直接调用；如果需要在类似应用中触发选择器，可通过「文件」上传入口发起系统级 Document/Content 请求。
- **Root / LSPosed 环境**：可通过 Hook 框架（如 Xposed）拦截客户端的 `PickVisualMedia` 调用并转换为标准 `ACTION_GET_CONTENT`。

---

## 项目结构

```
GalleryHook/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/
│   │   │   ├── MainActivity.kt     # 核心路由逻辑与设置界面
│   │   │   └── ui/theme/           # Material 3 主题定义
│   │   └── AndroidManifest.xml     # Intent 过滤器与包名查询配置
│   └── build.gradle.kts            # 构建配置 (R8 Minify & Shrink)
├── .github/workflows/
│   └── build-apk.yml               # CI 构建与 GitHub Releases 发布流程
├── LICENSE                         # Apache 2.0 许可证
└── README.md                       # 技术与使用文档
```

---

## 构建与部署

### 本地编译
```bash
./gradlew :app:assembleRelease
```
产物输出路径：`app/build/outputs/apk/release/app-release.apk`

### CI 自动化构建
项目集成了 GitHub Actions 流水线，在触发 workflow 时会自动编译并发布免压缩的直装 APK 至 Releases。

---

## 许可证

本项目基于 [Apache License 2.0](LICENSE) 协议开源。
