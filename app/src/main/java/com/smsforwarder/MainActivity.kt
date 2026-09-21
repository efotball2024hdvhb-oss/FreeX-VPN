package com.smsforwarder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val smsGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) ==
            PackageManager.PERMISSION_GRANTED
        if (!smsGranted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_SMS,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            )
        }
        setContent { SmsForwarderApp() }
    }
}

@Composable
fun SmsForwarderApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { SmsRepository(context) }
    var enabled by remember { mutableStateOf(repo.settings().third) }
    var destination by remember { mutableStateOf(repo.settings().first) }
    var backendUrl by remember { mutableStateOf(repo.settings().second) }
    var messages by remember { mutableStateOf(repo.load()) }
    var showSettings by remember { mutableStateOf(false) }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF111827),
            secondary = Color(0xFF6366F1),
            background = Color(0xFFF7F7FA),
            surface = Color.White
        )
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (showSettings) {
                SettingsScreen(
                    destination = destination,
                    backendUrl = backendUrl,
                    onDestination = { destination = it },
                    onBackend = { backendUrl = it },
                    onSave = {
                        repo.saveSettings(destination, backendUrl, enabled)
                        showSettings = false
                    },
                    onBack = { showSettings = false }
                )
            } else {
                Scaffold(
                    bottomBar = {
                        NavigationBar(containerColor = Color.White) {
                            NavigationBarItem(
                                selected = true, onClick = {},
                                icon = { Icon(Icons.Default.Message, null) },
                                label = { Text("پیام‌ها") }
                            )
                            NavigationBarItem(
                                selected = false, onClick = { showSettings = true },
                                icon = { Icon(Icons.Default.Settings, null) },
                                label = { Text("تنظیمات") }
                            )
                        }
                    }
                ) { pad ->
                    LazyColumn(
                        modifier = Modifier.padding(pad).padding(horizontal = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(vertical = 22.dp)
                    ) {
                        item {
                            Text("SMS Forwarder", style = MaterialTheme.typography.headlineMedium)
                            Text("مدیریت هوشمند پیامک‌ها", color = Color.Gray)
                        }
                        item {
                            Card(
                                shape = RoundedCornerShape(28.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))
                            ) {
                                Row(
                                    Modifier.padding(22.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Shield, null, tint = Color.White)
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("Forwarding", color = Color.White,
                                            style = MaterialTheme.typography.titleLarge)
                                        Text(
                                            if (enabled) "فعال — پیامک‌های جدید پردازش می‌شوند"
                                            else "غیرفعال",
                                            color = Color(0xFFD1D5DB)
                                        )
                                    }
                                    Switch(checked = enabled, onCheckedChange = { enabled = it; repo.saveSettings(destination, backendUrl, enabled) })
                                }
                            }
                        }
                        item {
                            Card(shape = RoundedCornerShape(24.dp)) {
                                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Email, null, tint = Color(0xFF6366F1))
                                    Spacer(Modifier.width(14.dp))
                                    Column {
                                        Text("مقصد ایمیل", style = MaterialTheme.typography.labelLarge)
                                        Text(destination, color = Color.Gray)
                                    }
                                }
                            }
                        }
                        item { Text("پیام‌های اخیر", style = MaterialTheme.typography.titleLarge) }
                        if (messages.isEmpty()) {
                            item { Text("هنوز پیامکی دریافت نشده است.", color = Color.Gray) }
                        } else {
                            items(messages, key = { it.id }) { sms ->
                                Card(shape = RoundedCornerShape(22.dp)) {
                                    Column(Modifier.padding(18.dp)) {
                                        Row {
                                            Text(sms.sender, style = MaterialTheme.typography.titleMedium)
                                            Spacer(Modifier.weight(1f))
                                            Text(
                                                DateFormat.getTimeInstance(DateFormat.SHORT)
                                                    .format(Date(sms.timestamp)),
                                                color = Color.Gray
                                            )
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Text(sms.body)
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            if (sms.forwarded) "ارسال شده" else "در صف ارسال",
                                            color = if (sms.forwarded) Color(0xFF16A34A) else Color.Gray,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            OutlinedButton(onClick = { messages = repo.load() }) {
                                Text("به‌روزرسانی پیام‌ها")
                            }
                        }
                        item {
                            Text(
                                "حریم خصوصی: پردازش پیامک فقط پس از اجازه شما انجام می‌شود. اطلاعات ورود ایمیل نباید داخل APK قرار گیرد.",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    destination: String,
    backendUrl: String,
    onDestination: (String) -> Unit,
    onBackend: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(onClick = onBack) { Text("← برگشت") }
        Text("تنظیمات", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = destination,
            onValueChange = onDestination,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("ایمیل مقصد") },
            singleLine = true
        )
        OutlinedTextField(
            value = backendUrl,
            onValueChange = onBackend,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("آدرس Backend HTTPS") },
            placeholder = { Text("https://example.com") },
            singleLine = true
        )
        Text(
            "برای ارسال خودکار، Backend باید روی HTTPS فعال باشد و endpoint /v1/forward را ارائه کند.",
            color = Color.Gray
        )
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
            Text("ذخیره تنظیمات")
        }
    }
}
