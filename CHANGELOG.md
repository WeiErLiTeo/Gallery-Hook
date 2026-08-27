# 版本更新日志 (CHANGELOG)

所有重大更新与版本迭代都将记录在此文件中。

## [1.1.0] - 2026-08-26

### 🌟 新增功能与优化 (Features & Enhancements)
- 📸 **系统相册选择器伪装**: 注册标准 `ACTION_GET_CONTENT`、`ACTION_PICK` 与 `ACTION_OPEN_DOCUMENT` Intent Filter。在第三方应用拉起相册时展示系统二选一列表。
- 🎨 **ColorOS 专属相册定向支持**: 新增对 `com.coloros.gallery3d`（兼容 `com.oplus.gallery`）相册选择器的精准调起与适配。
- 🔒 **智能防死循环机制**: 在调用系统相册组件时动态排除 GalleryHook 自身包名，杜绝二次递归唤醒。
- 🤖 **GitHub Actions 手动云端编译**: 新增 `.github/workflows/build-apk.yml`，支持通过 `workflow_dispatch` 手动触发 Release/Debug APK 编译并生成下载 Artifact。
- ⚙️ **配置界面升级**: 扩展配置项为五大模式（每次询问、系统相册、ColorOS 相册、谷歌相册、文件管理器）。

## [1.0.0] - 2026-07-31

### 🌟 初始发布 (Initial Release)
- 🪝 **Intent 隐式拦截**: 自动接管并重定向 `android.provider.MediaStore.RECORD_SOUND` 请求。
- 🎨 **Material Design 3 界面**: 全面支持 Edge-to-Edge 沉浸式 UI 与 Dynamic Color 动态调色盘。
- ⚡ **无感选择返回**: 选定媒体后自动返回 `RESULT_OK` 并在后台优雅退出进程。
