package com.smsforwarder

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.*

private val Bg = Color(0xFF080A0F)
private val Card = Color(0xFF11141B)
private val Blue = Color(0xFF0A84FF)
private val Green = Color(0xFF30D158)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = darkColorScheme(background = Bg, surface = Card, primary = Blue)) { App() } }
    }
}

@Composable
private fun App() {
    val c = LocalContext.current
    var list by remember { mutableStateOf(SmsRepository.getAll(c)) }
    var enabled by remember { mutableStateOf(SmsRepository.isEnabled(c)) }
    var email by remember { mutableStateOf(SmsRepository.getEmail(c)) }
    var backend by remember { mutableStateOf(SmsRepository.getBackend(c)) }
    var settings by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    LaunchedEffect(Unit) {
        val p = mutableListOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
        if (android.os.Build.VERSION.SDK_INT >= 33) p += Manifest.permission.POST_NOTIFICATIONS
        permissionLauncher.launch(p.toTypedArray())
    }

    if (settings) {
        SettingsPage(email, backend, enabled,
            { email = it; SmsRepository.setEmail(c, it) },
            { backend = it; SmsRepository.setBackend(c, it) },
            { enabled = it; SmsRepository.setEnabled(c, it) },
            { settings = false })
        return
    }

    Scaffold(containerColor = Bg, topBar = {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("SMS Forwarder", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Secure message center", color = Color(0xFF8E8E93), fontSize = 14.sp)
            }
            IconButton({ list = SmsRepository.getAll(c) }) { Icon(Icons.Default.Refresh, null, tint = Color.White) }
            IconButton({ settings = true }) { Icon(Icons.Default.Settings, null, tint = Color.White) }
        }
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp)) {
            Status(enabled, backend.isNotBlank())
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Messages", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("${list.size}", color = Color(0xFF8E8E93))
            }
            Spacer(Modifier.height(10.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 28.dp)) {
                items(list, key = { it.id }) { SmsCard(it) }
            }
        }
    }
}

@Composable
private fun Status(enabled: Boolean, backend: Boolean) {
    Surface(shape = RoundedCornerShape(24.dp), color = Card) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Blue, Color(0xFF5E5CE6)))), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Wifi, null, tint = Color.White)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(if (enabled) "Forwarding enabled" else "Forwarding paused", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(if (backend) "Backend configured" else "Add backend URL in Settings", color = Color(0xFF8E8E93), fontSize = 13.sp)
            }
            Box(Modifier.size(11.dp).clip(CircleShape).background(if (enabled && backend) Green else Color(0xFFFF9F0A)))
        }
    }
}

@Composable
private fun SmsCard(s: SmsItem) {
    val date = remember(s.timestamp) { SimpleDateFormat("dd MMM • HH:mm", Locale.getDefault()).format(Date(s.timestamp)) }
    Surface(shape = RoundedCornerShape(22.dp), color = Card) {
        Column(Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF1C1C1E)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Message, null, tint = Blue)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.sender, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(date, color = Color(0xFF8E8E93), fontSize = 12.sp)
                }
                Icon(Icons.Default.CheckCircle, null, tint = if (s.forwarded) Green else Color(0xFFFF9F0A))
            }
            Spacer(Modifier.height(12.dp))
            Text(s.body, color = Color(0xFFE5E5EA), fontSize = 15.sp, lineHeight = 21.sp)
        }
    }
}

@Composable
private fun SettingsPage(
    email: String, backend: String, enabled: Boolean,
    onEmail: (String) -> Unit, onBackend: (String) -> Unit,
    onEnabled: (Boolean) -> Unit, back: () -> Unit
) {
    Scaffold(containerColor = Bg) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 18.dp)) {
            Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(back) { Text("Back") }
                Text("Settings", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
            }
            Group("Forwarding") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Automatic forwarding", color = Color.White, fontWeight = FontWeight.Medium)
                        Text("Forward new SMS through your backend", color = Color(0xFF8E8E93), fontSize = 12.sp)
                    }
                    Switch(enabled, onEnabled)
                }
            }
            Spacer(Modifier.height(16.dp))
            Group("Email") {
                OutlinedTextField(email, onEmail, Modifier.fillMaxWidth(), label = { Text("Destination email") }, singleLine = true)
            }
            Spacer(Modifier.height(16.dp))
            Group("Backend") {
                OutlinedTextField(backend, onBackend, Modifier.fillMaxWidth(), label = { Text("HTTPS backend URL") }, placeholder = { Text("https://example.com") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                Text("Use HTTPS in production.", color = Color(0xFF8E8E93), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun Group(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title.uppercase(), color = Color(0xFF8E8E93), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        Surface(shape = RoundedCornerShape(22.dp), color = Card) { Column(Modifier.padding(16.dp), content = content) }
    }
}
