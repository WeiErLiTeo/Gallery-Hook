# GalleryHook 🪝

<p align="center">
  <img src="app/src/main/res/mipmap-xxhdpi/ic_launcher.png" width="96" alt="GalleryHook Logo" />
</p>

<p align="center">
  <b>平时微信/QQ选图被恶心到了？用这个小工具把选图和录音请求重定向到你想用的相册或文件管理器。</b>
</p>

<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License"></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-1.9.0-blue.svg?logo=kotlin" alt="Kotlin"></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose" alt="Jetpack Compose"></a>
  <a href="CHANGELOG.md"><img src="https://img.shields.io/badge/Version-1.1.0-orange.svg" alt="Version"></a>
</p>

---

## 💡 这个 App 是干嘛的？

很多第三方软件在调用选图、传文件或者某些特殊录音接口时，经常强制弹出难用的内置界面或者指定的选择器。

**GalleryHook** 的作用非常简单直接：
1. **伪装成系统相册**：当别的 App 要选图时，系统会弹窗让你在“原生相册”和“GalleryHook”之间二选一。
2. **想调谁就调谁**：你可以让它每次弹窗问你，也可以直接锁死调起 **ColorOS 官方相册**、**原生相册**、**Google 相册** 或者 **系统文件管理器**。
3. **选完就走**：选好图片/文件后直接把结果原路塞回给调用的 App，然后自动退出后台，不占内存不耗电。

---

## 🛠️ 支持的几个功能

- **伪装相册选择器**：第三方 App 调起选图时，系统会把它当成相册列出来。
- **ColorOS 专属相册支持**：能直接调起 OPPO / OnePlus / Realme 的官方相册（`com.coloros.gallery3d`）。
- **五种拦截模式随便选**：
  - 💬 **每次询问**：每次选图时弹个小菜单让你自己挑用哪个。
  - 🖼️ **仅系统相册**：直接走系统自带的原生选图器。
  - 🎨 **仅 ColorOS 相册**：直接打开欧加系统的官方相册。
  - 🌐 **仅 Google Photos**：直接唤起谷歌相册。
  - 📁 **仅文件管理器**：直接打开系统的文件浏览界面选任意文件。
- **防套娃防死循环**：自己调系统相册时会自动排除自己，绝不会出现无限唤醒自己的 Bug。
- **支持 GitHub 自动打包**：仓库里配好了 Actions，点一下网页按钮就能直接下载打包好的 APK，不需要自己装 Android Studio 编译。

---

## 🔬 技术原理（写给开发者看的）

原理就是利用 Android 的隐式 Intent 机制接管以下标准 Action：
- `android.intent.action.GET_CONTENT`
- `android.intent.action.PICK`
- `android.intent.action.OPEN_DOCUMENT`
- `android.provider.MediaStore.RECORD_SOUND`

```
+-------------------------------------------------------------+
|                  第三方 App 发起调起请求                       |
|       (ACTION_GET_CONTENT / ACTION_PICK / RECORD_SOUND)     |
+------------------------------+------------------------------+
                               |
                               v
                     [ GalleryHook MainActivity ]
                               |
         +---------------------+---------------------+
         |                                           |
         v                                           v
 [ 拦截界面 (透明背景) ]                       [ 普通打开进入设置页 ]
         |                                           |
  读取 SharedPreferences 中的目标模式            切换模式 & 保存配置
  (弹窗询问 / ColorOS / 原生 / 谷歌 / 文件)
         |
  排除自身包名，通过 ActivityResultLauncher 调起目标
         |
  拿到 Uri 并带 FLAG_GRANT_READ_URI_PERMISSION 返回给原 App
```

---

## 📂 项目目录

```
GalleryHook/
├── app/                        # Android 应用主工程
│   └── src/
│       ├── main/
│       │   ├── java/com/example/
│       │   │   ├── MainActivity.kt # 拦截分发与设置界面逻辑
│       │   │   └── ui/theme/       # Compose 主题配置
│       │   └── res/            # 图标、文字与 Manifest 配置
├── .github/
│   └── workflows/
│       └── build-apk.yml       # GitHub Actions 网页手动打包脚本
├── CONTRIBUTING.md             # 贡献指南
├── CHANGELOG.md                # 更新记录
├── LICENSE                     # 开源协议 (Apache 2.0)
└── README.md                   # 说明文档
```

---

## 📦 怎么下载 / 编译 APK？

### 方式一：直接在 GitHub 网页上打包下载（最省事）
1. 把这个项目 Fork 或者传到你自己的 GitHub 仓库；
2. 打开仓库顶部的 **Actions** 菜单；
3. 点击左侧的 **Build & Release APK**；
4. 点击右边的 **Run workflow** 按钮开始打包；
5. 等两三分钟编译完成后，在页面下方的 **Artifacts** 处点击就能下载 APK 安装包了。

### 方式二：本地用命令行编译
```bash
git clone https://github.com/your-username/GalleryHook.git
cd GalleryHook
gradle :app:assembleRelease
```
编译好的安装包就在 `app/build/outputs/apk/release/` 目录下。

---

## 📄 开源协议

基于 [Apache License 2.0](LICENSE) 协议开源。
