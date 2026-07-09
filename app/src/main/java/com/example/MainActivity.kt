package com.example

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val isIntercepting = intent?.action == "android.provider.MediaStore.RECORD_SOUND"

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

@Composable
fun InterceptScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current as Activity
    val prefs = context.getSharedPreferences("GalleryHookConfig", Context.MODE_PRIVATE)
    // 0 = 询问(Ask), 1 = 仅相册(Image), 2 = 仅文件(File), 3 = 仅谷歌相册
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

    LaunchedEffect(Unit) {
        if (mode == 1) {
            pickerLauncher.launch(Intent(Intent.ACTION_GET_CONTENT).apply { 
                type = "image/*" 
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            })
        } else if (mode == 2) {
            pickerLauncher.launch(Intent(Intent.ACTION_GET_CONTENT).apply { 
                type = "*/*" 
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            })
        } else if (mode == 3) {
            pickerLauncher.launch(Intent(Intent.ACTION_GET_CONTENT).apply { 
                type = "image/*" 
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                setPackage("com.google.android.apps.photos")
            })
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { 
                context.finishAndRemoveTask()
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    kotlin.system.exitProcess(0)
                }, 500)
            },
            title = { Text("选择来源", fontWeight = FontWeight.Bold) },
            text = { Text("请选择需要重定向的提取方式") },
            containerColor = MaterialTheme.colorScheme.surface,
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        showDialog = false
                        pickerLauncher.launch(Intent(Intent.ACTION_GET_CONTENT).apply { 
                            type = "image/*" 
                            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                            setPackage("com.google.android.apps.photos")
                        })
                    }) {
                        Text("谷歌相册")
                    }
                    TextButton(onClick = {
                        showDialog = false
                        pickerLauncher.launch(Intent(Intent.ACTION_GET_CONTENT).apply { 
                            type = "image/*" 
                            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                        })
                    }) {
                        Text("系统相册")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                    pickerLauncher.launch(Intent(Intent.ACTION_GET_CONTENT).apply { 
                        type = "*/*" 
                        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    })
                }) {
                    Text("文件")
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
                        text = "INTERCEPTION MODE",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    val modes = listOf("Prompt Every Time", "Always Gallery", "Always File Picker", "Always Google Photos")
                    modes.forEachIndexed { index, title ->
                        val isSelected = interceptMode == index
                        val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        
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
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = textColor
                            )
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

