package com.example

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val action = intent?.action
        val isLauncher = action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_LAUNCHER)
        val isIntercepting = !isLauncher && (
            action == "android.provider.MediaStore.RECORD_SOUND" ||
            action == Intent.ACTION_GET_CONTENT ||
            action == Intent.ACTION_PICK ||
            action == Intent.ACTION_OPEN_DOCUMENT ||
            action == "android.provider.action.PICK_IMAGES"
        )

        setContent {
            MyApplicationTheme {
                if (isIntercepting) {
                    InterceptScreen()
                } else {
                    ConfigScreen()
                }
            }
        }
    }
}

// Intercept Target Mode definitions:
// 0 = 每次询问 (Prompt Every Time)
// 1 = 系统原生相册 / Photo Picker (System Native Gallery)
// 2 = ColorOS / OPlus 相册 (com.coloros.gallery3d / com.oplus.gallery)
// 3 = 小米 HyperOS / MIUI 相册 (com.miui.gallery)
// 4 = 谷歌相册 (Google Photos)
// 5 = 系统文件管理器 (File Picker)

fun createSystemGalleryIntent(context: Context): Intent {
    // Priority 1: Android 13+ (API 33+) System Photo Picker
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val pickImagesIntent = Intent(MediaStore.ACTION_PICK_IMAGES).apply {
            type = "image/*"
            putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, 100)
        }
        val resolved = context.packageManager.queryIntentActivities(pickImagesIntent, 0)
        if (resolved.isNotEmpty()) {
            return pickImagesIntent
        }
    }

    // Priority 2: Standard ACTION_PICK on MediaStore
    val mediaStoreIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
    }
    val resolvedList = context.packageManager.queryIntentActivities(mediaStoreIntent, 0)
    val externalActivity = resolvedList.firstOrNull { it.activityInfo.packageName != context.packageName }
    if (externalActivity != null) {
        return mediaStoreIntent
    }

    // Priority 3: ACTION_GET_CONTENT fallback
    return Intent(Intent.ACTION_GET_CONTENT).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        addCategory(Intent.CATEGORY_OPENABLE)
    }
}

fun createColorOsGalleryIntent(context: Context): Intent {
    val candidatePackages = listOf(
        "com.coloros.gallery3d",
        "com.oplus.gallery",
        "com.heytap.gallery"
    )
    for (pkg in candidatePackages) {
        val testIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
            setPackage(pkg)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        val resolved = context.packageManager.queryIntentActivities(testIntent, 0)
        if (resolved.isNotEmpty()) {
            return testIntent
        }
        
        // Also test GET_CONTENT with package
        val getContentIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            setPackage(pkg)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        if (context.packageManager.queryIntentActivities(getContentIntent, 0).isNotEmpty()) {
            return getContentIntent
        }
    }

    // Fallback: standard pick with coloros package hint
    return Intent(Intent.ACTION_PICK).apply {
        type = "image/*"
        setPackage("com.coloros.gallery3d")
        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
    }
}

fun createMiuiGalleryIntent(context: Context): Intent {
    val miuiIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
        type = "image/*"
        setPackage("com.miui.gallery")
        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
    }
    val resolved = context.packageManager.queryIntentActivities(miuiIntent, 0)
    if (resolved.isNotEmpty()) {
        return miuiIntent
    }
    return Intent(Intent.ACTION_GET_CONTENT).apply {
        type = "image/*"
        setPackage("com.miui.gallery")
        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
    }
}

fun createGooglePhotosIntent(): Intent {
    return Intent(Intent.ACTION_GET_CONTENT).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        setPackage("com.google.android.apps.photos")
    }
}

fun createFilePickerIntent(context: Context): Intent {
    return Intent(Intent.ACTION_GET_CONTENT).apply {
        type = "*/*"
        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        addCategory(Intent.CATEGORY_OPENABLE)
    }
}

@Composable
fun InterceptScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current as Activity
    val prefs = context.getSharedPreferences("GalleryHookConfig", Context.MODE_PRIVATE)
    val mode = prefs.getInt("intercept_mode", 0)

    var showDialog by remember { mutableStateOf(mode == 0) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val intentData = result.data!!
            val resultIntent = Intent().apply {
                if (intentData.clipData != null) {
                    clipData = intentData.clipData
                }
                if (intentData.data != null) {
                    data = intentData.data
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.setResult(Activity.RESULT_OK, resultIntent)
        } else {
            context.setResult(Activity.RESULT_CANCELED)
        }
        context.finishAndRemoveTask()
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            kotlin.system.exitProcess(0)
        }, 300)
    }

    fun launchSafe(intent: Intent) {
        try {
            pickerLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "未找到对应相册应用或系统组件", Toast.LENGTH_SHORT).show()
            context.setResult(Activity.RESULT_CANCELED)
            context.finishAndRemoveTask()
        } catch (e: SecurityException) {
            Toast.makeText(context, "无权限调起该组件，已转为原生选择器", Toast.LENGTH_SHORT).show()
            try {
                pickerLauncher.launch(createSystemGalleryIntent(context))
            } catch (ex: Exception) {
                context.setResult(Activity.RESULT_CANCELED)
                context.finishAndRemoveTask()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "调起失败: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            context.setResult(Activity.RESULT_CANCELED)
            context.finishAndRemoveTask()
        }
    }

    LaunchedEffect(Unit) {
        when (mode) {
            1 -> launchSafe(createSystemGalleryIntent(context))
            2 -> launchSafe(createColorOsGalleryIntent(context))
            3 -> launchSafe(createMiuiGalleryIntent(context))
            4 -> launchSafe(createGooglePhotosIntent())
            5 -> launchSafe(createFilePickerIntent(context))
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                context.setResult(Activity.RESULT_CANCELED)
                context.finishAndRemoveTask()
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    kotlin.system.exitProcess(0)
                }, 300)
            },
            title = { Text("选择目标相册 / 来源", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("请选择要调起的相册选择器或文件源：", style = MaterialTheme.typography.bodyMedium)
                    
                    OutlinedButton(
                        onClick = {
                            showDialog = false
                            launchSafe(createSystemGalleryIntent(context))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("系统原生相册 (Photo Picker)")
                    }

                    OutlinedButton(
                        onClick = {
                            showDialog = false
                            launchSafe(createColorOsGalleryIntent(context))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ColorOS 相册 (com.coloros.gallery3d)")
                    }

                    OutlinedButton(
                        onClick = {
                            showDialog = false
                            launchSafe(createMiuiGalleryIntent(context))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("小米相册 (com.miui.gallery)")
                    }

                    OutlinedButton(
                        onClick = {
                            showDialog = false
                            launchSafe(createGooglePhotosIntent())
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Google Photos (谷歌相册)")
                    }

                    OutlinedButton(
                        onClick = {
                            showDialog = false
                            launchSafe(createFilePickerIntent(context))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("系统文件管理器")
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    context.setResult(Activity.RESULT_CANCELED)
                    context.finishAndRemoveTask()
                }) {
                    Text("取消")
                }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Transparent))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("GalleryHookConfig", Context.MODE_PRIVATE) }

    var interceptMode by remember { mutableIntStateOf(prefs.getInt("intercept_mode", 0)) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(modifier = Modifier.size(16.dp).background(MaterialTheme.colorScheme.onPrimary).rotate(45f))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("GalleryHook", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "拦截重定向模式 (INTERCEPTION MODE)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp),
                        color = MaterialTheme.colorScheme.primary
                    )

                    val modes = listOf(
                        "每次询问 (Prompt Every Time)" to "弹出选择器对话框供您实时选择目标",
                        "系统原生相册 (System Gallery)" to "自动调起 Android 原生 Photo Picker / MediaStore",
                        "ColorOS / OPlus 相册" to "调起 OPPO / OnePlus / Realme 官方相册",
                        "小米相册 (HyperOS / MIUI)" to "调起小米官方相册选择器",
                        "Google Photos (谷歌相册)" to "调起 Google Photos 选择器",
                        "系统文件管理器 (File Picker)" to "调起系统内置文件管理器选取任意文件"
                    )

                    modes.forEachIndexed { index, item ->
                        val isSelected = interceptMode == index
                        val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        val subColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(bgColor, shape = RoundedCornerShape(16.dp))
                                .clickable {
                                    interceptMode = index
                                    prefs.edit().putInt("intercept_mode", index).apply()
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.first,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = textColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.second,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = subColor
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        }
                    }
                }
            }

            // WeChat photo picking analysis card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "💬 微信照片选择特殊说明与解决技巧",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "为什么微信点照片不弹系统选择框？\n" +
                               "• 微信在聊天界面点击「照片」时，根本不会向 Android 发送系统的选图 Intent 请求，而是直接在其内部启动微信自带的私有 Activity (AlbumPreviewUI) 并直接读取本地数据库。\n\n" +
                               "💡 微信如何成功调用 GalleryHook：\n" +
                               "1. 微信「文件」通道法：在微信聊天框点击「+」→ 选择「文件」→ 点击「手机存储」或右上角「其它应用」，微信就会发出系统级 Intent，从而正常唤起 GalleryHook！\n" +
                               "2. 小程序 / 网页上传：微信内的网页或小程序点击上传图片时会触发系统 Intent，可直接拦截。\n" +
                               "3. Root / LSPosed 用户：若需强制劫持微信内所有选图页面，需配合 Xposed/LSPosed 模块 Hook 拦截 com.tencent.mm.plugin.gallery.ui.AlbumPreviewUI。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        lineHeight = 21.sp
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "🛡️ 防死循环与安全机制",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "• 原生相册智能适配：自动识别 Android 13/14/15 的 Photo Picker 与老版本 MediaStore，杜绝 SecurityException 崩溃。\n• 防自身递归：调起外部相册时自动过滤自身包名，保证永不卡死。\n• 内存极小化：无后台守护常驻，选图完成后 300ms 内自动彻底结束进程释放资源。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 21.sp
                    )
                }
            }
        }
    }
}
