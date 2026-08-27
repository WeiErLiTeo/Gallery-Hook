package com.example

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
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

// 0: 每次询问 (Prompt Every Time)
// 1: 系统原生相册 (System Gallery / Photo Picker)
// 2: ColorOS / OPlus 相册 (com.coloros.gallery3d)
// 3: Google Photos (谷歌相册)

fun createSystemGalleryIntent(context: Context): Intent {
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

    val mediaStoreIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
    }
    val resolvedList = context.packageManager.queryIntentActivities(mediaStoreIntent, 0)
    val externalActivity = resolvedList.firstOrNull { it.activityInfo.packageName != context.packageName }
    if (externalActivity != null) {
        return mediaStoreIntent
    }

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
        
        val getContentIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            setPackage(pkg)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        if (context.packageManager.queryIntentActivities(getContentIntent, 0).isNotEmpty()) {
            return getContentIntent
        }
    }

    return Intent(Intent.ACTION_PICK).apply {
        type = "image/*"
        setPackage("com.coloros.gallery3d")
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

private fun terminateSelfAndClean(activity: Activity) {
    activity.finishAndRemoveTask()
    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
        Process.killProcess(Process.myPid())
    }, 150)
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
        terminateSelfAndClean(context)
    }

    fun launchSafe(intent: Intent) {
        try {
            pickerLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "未找到对应相册应用", Toast.LENGTH_SHORT).show()
            context.setResult(Activity.RESULT_CANCELED)
            terminateSelfAndClean(context)
        } catch (e: SecurityException) {
            try {
                pickerLauncher.launch(createSystemGalleryIntent(context))
            } catch (ex: Exception) {
                context.setResult(Activity.RESULT_CANCELED)
                terminateSelfAndClean(context)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "调起失败: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            context.setResult(Activity.RESULT_CANCELED)
            terminateSelfAndClean(context)
        }
    }

    LaunchedEffect(Unit) {
        when (mode) {
            1 -> launchSafe(createSystemGalleryIntent(context))
            2 -> launchSafe(createColorOsGalleryIntent(context))
            3 -> launchSafe(createGooglePhotosIntent())
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                context.setResult(Activity.RESULT_CANCELED)
                terminateSelfAndClean(context)
            },
            title = { Text("选择目标相册", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            launchSafe(createGooglePhotosIntent())
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Google Photos (谷歌相册)")
                    }

                    OutlinedButton(
                        onClick = {
                            showDialog = false
                            launchSafe(createSystemGalleryIntent(context))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("系统原生相册")
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    context.setResult(Activity.RESULT_CANCELED)
                    terminateSelfAndClean(context)
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
                        text = "拦截重定向模式",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp),
                        color = MaterialTheme.colorScheme.primary
                    )

                    val modes = listOf(
                        "每次询问" to "每次唤起时弹出选择框供您选择",
                        "系统原生相册" to "自动调用系统原生相册/Photo Picker",
                        "ColorOS 相册" to "自动调用 ColorOS / OPlus 相册",
                        "Google Photos (谷歌相册)" to "自动调用 Google Photos 选择器"
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
        }
    }
}
