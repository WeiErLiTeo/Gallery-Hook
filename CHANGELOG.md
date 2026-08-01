# 版本更新日志 (CHANGELOG)

所有重大更新与版本迭代都将记录在此文件中。

## [1.0.0] - 2026-07-31

### 🌟 新增功能 (Features)
- 🪝 **Intent 隐式拦截**: 自动接管并重定向 `android.provider.MediaStore.RECORD_SOUND` 及相册选取请求。
- ⚙️ **四大拦截模式**: 支持“每次询问”、“仅系统相册”、“仅文件选择器”与“仅 Google Photos”。
- 🎨 **Material Design 3 界面**: 全面支持 Edge-to-Edge 沉浸式 UI 与 Dynamic Color 动态调色盘。
- ⚡ **无感选择返回**: 选定媒体后自动返回 `RESULT_OK` 并在后台优雅退出进程。
