package com.smsforwarder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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

data class SmsItem(val sender: String, val body: String, val time: String)

class MainActivity : ComponentActivity() {
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS))
        }
        setContent { SmsForwarderApp() }
    }
}

@Composable
fun SmsForwarderApp() {
    var enabled by remember { mutableStateOf(true) }
    var destination by remember { mutableStateOf("efotball2024hdvhb@gmail.com") }
    val messages = remember {
        mutableStateListOf(
            SmsItem("System", "SMS Forwarder آماده است. پیام‌های جدید اینجا نمایش داده می‌شوند.", "اکنون")
        )
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF111827),
            secondary = Color(0xFF6366F1),
            background = Color(0xFFF7F7FA),
            surface = Color.White
        )
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    NavigationBar(containerColor = Color.White) {
                        NavigationBarItem(true, {}, icon = { Icon(Icons.Default.Message, null) }, label = { Text("پیام‌ها") })
                        NavigationBarItem(false, {}, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("تنظیمات") })
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
                            Column(Modifier.padding(22.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Shield, null, tint = Color.White)
                                    Spacer(Modifier.width(10.dp))
                                    Text("Forwarding", color = Color.White, style = MaterialTheme.typography.titleLarge)
                                    Spacer(Modifier.weight(1f))
                                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                                }
                                Spacer(Modifier.height(18.dp))
                                Text(
                                    if (enabled) "فعال — پیامک‌های جدید پردازش می‌شوند"
                                    else "غیرفعال",
                                    color = Color(0xFFD1D5DB)
                                )
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
                    item {
                        Text("پیام‌های اخیر", style = MaterialTheme.typography.titleLarge)
                    }
                    items(messages) { sms ->
                        Card(shape = RoundedCornerShape(22.dp)) {
                            Column(Modifier.padding(18.dp)) {
                                Row {
                                    Text(sms.sender, style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.weight(1f))
                                    Text(sms.time, color = Color.Gray)
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(sms.body)
                            }
                        }
                    }
                    item {
                        Text(
                            "حریم خصوصی: این برنامه فقط پس از اجازه شما به پیامک‌ها دسترسی می‌گیرد. برای ارسال ایمیل، رمز حساب داخل برنامه قرار داده نمی‌شود.",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
