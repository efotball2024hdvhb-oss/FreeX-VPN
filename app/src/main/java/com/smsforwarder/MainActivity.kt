package com.smsforwarder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Background = Color(0xFF080A0F)
private val SurfaceGlass = Color(0xFF111722).copy(alpha = 0.90f)
private val SurfaceSoft = Color(0xFF151B27).copy(alpha = 0.92f)
private val Border = Color.White.copy(alpha = 0.08f)
private val TelegramBlue = Color(0xFF24A1DE)
private val TelegramBlue2 = Color(0xFF5B8CFF)
private val Green = Color(0xFF30D158)
private val Orange = Color(0xFFFF9F0A)
private val Red = Color(0xFFFF453A)
private val TextPrimary = Color(0xFFF7F9FC)
private val TextSecondary = Color(0xFF8E9AA8)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SmsForwarderTheme {
                SmsForwarderApp()
            }
        }
    }
}

@Composable
private fun SmsForwarderTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = TelegramBlue,
        secondary = TelegramBlue2,
        background = Background,
        surface = SurfaceGlass,
        onBackground = TextPrimary,
        onSurface = TextPrimary
    )

    MaterialTheme(
        colorScheme = scheme,
        typography = Typography(
            headlineLarge = LocalTextStyle.current.copy(
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            ),
            titleLarge = LocalTextStyle.current.copy(
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            ),
            bodyLarge = LocalTextStyle.current.copy(fontSize = 15.sp),
            bodyMedium = LocalTextStyle.current.copy(fontSize = 13.sp)
        ),
        content = { Column(content = content) }
    )
}

@Composable
private fun SmsForwarderApp() {
    val context = LocalContext.current
    var refreshKey by remember { mutableIntStateOf(0) }
    var settings by remember { mutableStateOf(false) }

    key(refreshKey) {
        if (settings) {
            SettingsScreen(
                onBack = { settings = false },
                onSaved = { refreshKey++ }
            )
        } else {
            HomeScreen(
                onRefresh = { refreshKey++ },
                onSettings = { settings = true }
            )
        }
    }
}

@Composable
private fun HomeScreen(
    onRefresh: () -> Unit,
    onSettings: () -> Unit
) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(SmsRepository.isEnabled(context)) }
    var list by remember { mutableStateOf(SmsRepository.getAll(context)) }

    val smsGranted = remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECEIVE_SMS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        smsGranted.value = result[Manifest.permission.RECEIVE_SMS] == true ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECEIVE_SMS
            ) == PackageManager.PERMISSION_GRANTED
        list = SmsRepository.getAll(context)
    }

    LaunchedEffect(Unit) {
        val missing = buildList {
            if (Build.VERSION.SDK_INT >= 23 &&
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECEIVE_SMS
                ) != PackageManager.PERMISSION_GRANTED
            ) add(Manifest.permission.RECEIVE_SMS)

            if (Build.VERSION.SDK_INT >= 23 &&
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_SMS
                ) != PackageManager.PERMISSION_GRANTED
            ) add(Manifest.permission.READ_SMS)

            if (Build.VERSION.SDK_INT >= 33 &&
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0B1018),
                        Background,
                        Color(0xFF06070B)
                    )
                )
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Header(
                onRefresh = {
                    list = SmsRepository.getAll(context)
                    onRefresh()
                },
                onSettings = onSettings
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Spacer(Modifier.height(2.dp))
                    HeroCard(
                        enabled = enabled,
                        smsGranted = smsGranted.value,
                        backendConfigured = SmsRepository.getBackend(context).isNotBlank(),
                        onToggle = {
                            enabled = it
                            SmsRepository.setEnabled(context, it)
                        }
                    )
                }

                item {
                    SectionHeader(
                        title = "Recent messages",
                        count = list.size
                    )
                }

                if (list.isEmpty()) {
                    item { EmptyState(smsGranted.value) }
                } else {
                    items(list, key = { it.id }) { sms ->
                        SmsCard(sms)
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(
    onRefresh: () -> Unit,
    onSettings: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "SMS Forwarder",
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Private message center",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }

        RoundIconButton(Icons.Default.Refresh, onRefresh)
        Spacer(Modifier.width(7.dp))
        RoundIconButton(Icons.Default.Settings, onSettings)
    }
}

@Composable
private fun HeroCard(
    enabled: Boolean,
    smsGranted: Boolean,
    backendConfigured: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val statusColor = when {
        !smsGranted -> Orange
        enabled && backendConfigured -> Green
        enabled -> TelegramBlue
        else -> TextSecondary
    }

    val statusText = when {
        !smsGranted -> "Permission needed"
        enabled && backendConfigured -> "Active"
        enabled -> "Listening"
        else -> "Paused"
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(TelegramBlue, TelegramBlue2)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.SignalCellularAlt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(27.dp)
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        "Forwarding service",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(5.dp))
                    StatusChip(statusText, statusColor)
                }

                IosSwitch(
                    checked = enabled,
                    onCheckedChange = onToggle
                )
            }

            Spacer(Modifier.height(18.dp))

            HorizontalDivider(color = Border)

            Spacer(Modifier.height(15.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusLine(
                    icon = Icons.Default.Message,
                    title = if (smsGranted) "SMS listener ready" else "SMS permission required",
                    active = smsGranted
                )

                Spacer(Modifier.width(12.dp))

                StatusLine(
                    icon = if (backendConfigured) Icons.Default.CloudDone else Icons.Default.CloudOff,
                    title = if (backendConfigured) "Email route ready" else "Backend not configured",
                    active = backendConfigured
                )
            }
        }
    }
}

@Composable
private fun StatusLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    active: Boolean
) {
    Row(
        Modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (active) Green else Orange,
            modifier = Modifier.size(17.dp)
        )
        Spacer(Modifier.width(7.dp))
        Text(
            title,
            color = TextSecondary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun IosSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val track by animateColorAsState(
        if (checked) TelegramBlue else Color(0xFF303744),
        label = "track"
    )
    val scale by animateFloatAsState(
        targetValue = if (checked) 1.02f else 1f,
        animationSpec = spring(),
        label = "scale"
    )

    Box(
        Modifier
            .size(width = 52.dp, height = 32.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(track)
            .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
            .clickable { onCheckedChange(!checked) }
            .padding(3.dp)
    ) {
        Box(
            Modifier
                .size(26.dp)
                .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
private fun StatusChip(
    text: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.13f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            color.copy(alpha = 0.22f)
        )
    ) {
        Row(
            Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(6.dp))
            Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )

        Surface(
            shape = RoundedCornerShape(50),
            color = Color.White.copy(alpha = 0.05f)
        ) {
            Text(
                "$count",
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun SmsCard(sms: SmsItem) {
    val date = remember(sms.timestamp) {
        SimpleDateFormat("dd MMM • HH:mm", Locale.getDefault())
            .format(Date(sms.timestamp))
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF162B3A),
                                Color(0xFF10202D)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        TelegramBlue.copy(alpha = 0.18f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Message,
                    contentDescription = null,
                    tint = TelegramBlue,
                    modifier = Modifier.size(21.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        sms.sender,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        modifier = Modifier,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        relativeTime(sms.timestamp),
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Spacer(Modifier.height(5.dp))

                Text(
                    sms.body,
                    color = Color(0xFFD7DCE4),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(11.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (sms.forwarded) Icons.Default.CheckCircle else Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = if (sms.forwarded) Green else Orange,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        if (sms.forwarded) "Delivered to email" else "Waiting to send",
                        color = if (sms.forwarded) Green else Orange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.weight(1f))
                    Text(date, color = TextSecondary, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun EmptyState(permissionGranted: Boolean) {
    GlassCard(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(24.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(TelegramBlue.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (permissionGranted) Icons.Default.Message else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (permissionGranted) TelegramBlue else Orange,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.height(15.dp))

            Text(
                if (permissionGranted) "No messages yet" else "SMS permission is required",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(6.dp))

            Text(
                if (permissionGranted)
                    "New incoming SMS messages will appear here automatically."
                else
                    "Grant SMS permission from Android settings or the permission prompt.",
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
private fun SettingsScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf(SmsRepository.getEmail(context)) }
    var backend by remember { mutableStateOf(SmsRepository.getBackend(context)) }
    var enabled by remember { mutableStateOf(SmsRepository.isEnabled(context)) }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B1018), Background)
                )
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundIconButton(Icons.Default.ArrowBack, onBack)
                Text(
                    "Settings",
                    color = TextPrimary,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    GlassCard(Modifier.fillMaxWidth(), RoundedCornerShape(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(TelegramBlue.copy(alpha = 0.13f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Lock, null, tint = TelegramBlue)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Privacy-first forwarding", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "The Android app does not contain SMTP passwords.",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                item {
                    SettingsGroup("Forwarding") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Automatic forwarding", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text("Send every new SMS through the configured backend.", color = TextSecondary, fontSize = 12.sp)
                            }
                            IosSwitch(enabled) {
                                enabled = it
                                SmsRepository.setEnabled(context, it)
                                onSaved()
                            }
                        }
                    }
                }

                item {
                    SettingsGroup("Destination email") {
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                SmsRepository.setEmail(context, it)
                                onSaved()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Email, null) },
                            label = { Text("Email") }
                        )
                    }
                }

                item {
                    SettingsGroup("Backend") {
                        OutlinedTextField(
                            value = backend,
                            onValueChange = {
                                backend = it
                                SmsRepository.setBackend(context, it)
                                onSaved()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.CloudDone, null) },
                            label = { Text("HTTPS backend URL") },
                            placeholder = { Text("https://your-domain.example") }
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "The backend must expose POST /v1/forward and send the email server-side.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            title.uppercase(Locale.getDefault()),
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        GlassCard(Modifier.fillMaxWidth(), RoundedCornerShape(22.dp), content = { Column(content = content) })
    }
}

@Composable
private fun GlassCard(
    modifier: Modifier,
    shape: RoundedCornerShape,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.border(
            1.dp,
            Border,
            shape
        ),
        shape = shape,
        color = SurfaceGlass,
        tonalElevation = 0.dp,
        shadowElevation = 7.dp,
        content = { Column(content = content) }
    )
}

@Composable
private fun RoundIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(42.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.055f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(19.dp))
        }
    }
}

private fun relativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000L -> "now"
        diff < 3_600_000L -> "${diff / 60_000L}m"
        diff < 86_400_000L -> "${diff / 3_600_000L}h"
        else -> "${diff / 86_400_000L}d"
    }
}
