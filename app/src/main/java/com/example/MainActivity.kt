package com.example

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
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
            action == Intent.ACTION_OPEN_DOCUMENT
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

// Target Mode definitions:
// 0 = 每次询问 (Prompt Every Time)
// 1 = 系统原生相册 (System Gallery)
// 2 = ColorOS 相册 (com.coloros.gallery3d)
// 3 = 谷歌相册 (Google Photos)
// 4 = 文件管理器 (File Picker)

fun createColorOsGalleryIntent(context: Context): Intent {
    val intent = Intent(Intent.ACTION_PICK).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
    }
    val candidatePackages = listOf("com.coloros.gallery3d", "com.oplus.gallery", "com.heytap.gallery")
    var targetPkg: String? = null
    for (pkg in candidatePackages) {
        val testIntent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
            setPackage(pkg)
        }
        val resolved = context.packageManager.queryIntentActivities(testIntent, 0)
        if (resolved.isNotEmpty()) {
            targetPkg = pkg
            break
        }
    }
    intent.setPackage(targetPkg ?: "com.coloros.gallery3d")
    return intent
}

fun createGooglePhotosIntent(): Intent {
    return Intent(Intent.ACTION_GET_CONTENT).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        setPackage("com.google.android.apps.photos")
    }
}

fun createSystemGalleryIntent(context: Context): Intent {
    val intent = Intent(Intent.ACTION_PICK).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
    }
    // Exclude our own app from target to prevent recursive loop
    val resolvedList = context.packageManager.queryIntentActivities(intent, 0)
    val externalActivity = resolvedList.firstOrNull { 
        it.activityInfo.packageName != context.packageName &&
        it.activityInfo.packageName != "com.coloros.gallery3d" &&
        it.activityInfo.packageName != "com.oplus.gallery"
    } ?: resolvedList.firstOrNull { it.activityInfo.packageName != context.packageName }

    if (externalActivity != null) {
        intent.setClassName(externalActivity.activityInfo.packageName, externalActivity.activityInfo.name)
    }
    return intent
}

fun createFilePickerIntent(context: Context): Intent {
    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
        type = "*/*"
        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        addCategory(Intent.CATEGORY_OPENABLE)
    }
    val resolvedList = context.packageManager.queryIntentActivities(intent, 0)
    val externalActivity = resolvedList.firstOrNull { it.activityInfo.packageName != context.packageName }
    if (externalActivity != null) {
        intent.setClassName(externalActivity.activityInfo.packageName, externalActivity.activityInfo.name)
    }
    return intent
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
        }, 500)
    }

    fun launchSafe(intent: Intent) {
        try {
            pickerLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "未找到对应应用或相册组件", Toast.LENGTH_SHORT).show()
            context.setResult(Activity.RESULT_CANCELED)
            context.finishAndRemoveTask()
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
            3 -> launchSafe(createGooglePhotosIntent())
            4 -> launchSafe(createFilePickerIntent(context))
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                context.setResult(Activity.RESULT_CANCELED)
                context.finishAndRemoveTask()
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    kotlin.system.exitProcess(0)
                }, 500)
            },
            title = { Text("选择目标相册 / 来源", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("请选择要调起的相册选择器或文件源：", style = MaterialTheme.typography.bodyMedium)
                    
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
                            launchSafe(createSystemGalleryIntent(context))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("系统原生相册 (Photo Picker)")
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "拦截重定向模式 (INTERCEPTION MODE)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp),
                        color = MaterialTheme.colorScheme.primary
                    )

                    val modes = listOf(
                        "每次询问 (Prompt Every Time)" to "弹出选择器对话框供您实时选择",
                        "仅系统原生相册 (System Gallery)" to "直接调用 Android 系统原生相册选择器",
                        "仅 ColorOS 相册 (com.coloros.gallery3d)" to "直接调起 OPPO / OnePlus / Realme 官方相册选择器",
                        "仅 Google Photos (谷歌相册)" to "直接调起 Google Photos",
                        "仅系统文件管理器 (File Picker)" to "直接调用系统文件管理器选取任意文件"
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
                                .padding(16.dp),
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

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "功能说明与使用提示",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "• 伪装相册选择器：当第三方应用（如微信、QQ、浏览器等）打开相册或请求选图时，系统会弹出二选一列表（原生相册 / GalleryHook），您可以随时自由选择。\n• ColorOS 专属支持：支持精准定位并唤起 com.coloros.gallery3d 欧加相册。\n• 防死循环保护：内置智能过滤机制，调起原生相册时自动排除自身，防止递归唤醒。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}


