package com.freex.vpn.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScopeProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import com.freex.vpn.FreeXVpnApplication
import com.freex.vpn.domain.model.*
import com.freex.vpn.ui.theme.FreeXTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val vm by lazy {
        val app = application as FreeXVpnApplication
        ViewModelProvider(this, factory = FreeXVmFactory(app)).get(FreeXViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FreeXTheme { FreeXApp(vm) } }
    }
}

class FreeXVmFactory(private val app: FreeXVpnApplication) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        FreeXViewModel(app) as T
}

class FreeXViewModel(private val app: FreeXVpnApplication) : ViewModel() {
    val snapshot = app.container.vpnRepository.snapshot
    val servers = app.container.serverRepository.servers
    val favorites = app.container.serverRepository.favorites
    val recent = app.container.serverRepository.recent

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _sort = MutableStateFlow(ServerSort.LATENCY)
    val sort: StateFlow<ServerSort> = _sort

    val filtered = combine(servers, favorites, query, sort) { list, favs, q, sort ->
        list.filter {
            q.isBlank() || "${it.country} ${it.city} ${it.host}".contains(q, true)
        }.sortedWith(
            when (sort) {
                ServerSort.LATENCY -> compareBy<Server> { it.latencyMs ?: Long.MAX_VALUE }
                ServerSort.LOAD -> compareBy<Server> { it.loadPercent ?: Int.MAX_VALUE }
                ServerSort.COUNTRY -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.country }
            }
        )
    }

    val autoConnect = app.container.settings.autoConnect

    init { refresh() }

    fun refresh() { viewModelScope.launch { app.container.serverRepository.refresh() } }
    fun setQuery(v: String) { _query.value = v }
    fun setSort(v: ServerSort) { _sort.value = v }
    fun favorite(s: Server, value: Boolean) {
        viewModelScope.launch { app.container.serverRepository.setFavorite(s.id, value) }
    }
    fun connect(s: Server) {
        viewModelScope.launch {
            app.container.serverRepository.markUsed(s.id)
            app.container.vpnRepository.connect(s)
        }
    }
    fun disconnect() {
        viewModelScope.launch { app.container.vpnRepository.disconnect() }
    }
    fun preparePermission(): Intent? = app.container.vpnRepository.preparePermission(app)
}

enum class ServerSort { LATENCY, LOAD, COUNTRY }

@Composable
private fun FreeXApp(vm: FreeXViewModel) {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "home") {
        composable("home") { HomeScreen(vm, onServers = { nav.navigate("servers") }, onSettings = { nav.navigate("settings") }) }
        composable("servers") { ServerScreen(vm, onBack = { nav.popBackStack() }) }
        composable("settings") {
            val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as FreeXVpnApplication
            val settingsVm: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        SettingsViewModel(app.container.settings) as T
                }
            )
            SettingsScreen(settingsVm, onBack = { nav.popBackStack() })
        }
    }
}

@Composable
private fun HomeScreen(vm: FreeXViewModel, onServers: () -> Unit, onSettings: () -> Unit) {
    val snapshot by vm.snapshot.collectAsStateWithLifecycle()
    val servers by vm.filtered.collectAsStateWithLifecycle()
    var pendingPermission by remember { mutableStateOf<Intent?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == android.app.Activity.RESULT_OK) {
            servers.firstOrNull()?.let(vm::connect)
        }
    }
    val autoConnect by vm.autoConnect.collectAsStateWithLifecycle()

    LaunchedEffect(autoConnect, servers.size, snapshot.state) {
        if (autoConnect &&
            servers.isNotEmpty() &&
            snapshot.state == ConnectionState.DISCONNECTED
        ) {
            val permission = vm.preparePermission()
            if (permission != null) launcher.launch(permission) else vm.connect(servers.first())
        }
    }

    val onConnect = {
        if (snapshot.state == ConnectionState.CONNECTED || snapshot.state == ConnectionState.CONNECTING) {
            vm.disconnect()
        } else {
            val server = servers.firstOrNull()
            if (server != null) {
                val permission = vm.preparePermission()
                if (permission != null) launcher.launch(permission) else vm.connect(server)
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFF050608),
        bottomBar = { FloatingDock(onServers, onSettings) }
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(26.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("FreeX", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("PRIVATE • SIMPLE • FREE", color = Color(0xFF6C778A), style = MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, null, tint = Color(0xFF9DA7B8)) }
            }

            Spacer(Modifier.height(28.dp))
            ConnectionOrb(snapshot.state, onConnect)
            Spacer(Modifier.height(24.dp))

            AnimatedContent(snapshot.state, label = "state") { state ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        when (state) {
                            ConnectionState.CONNECTED -> "CONNECTED"
                            ConnectionState.CONNECTING -> "CONNECTING..."
                            ConnectionState.DISCONNECTING -> "DISCONNECTING..."
                            ConnectionState.RECONNECTING -> "RECONNECTING..."
                            ConnectionState.ERROR -> "CONNECTION ERROR"
                            ConnectionState.DISCONNECTED -> "DISCONNECTED"
                        },
                        fontWeight = FontWeight.Bold,
                        color = if (state == ConnectionState.CONNECTED) Color(0xFF65F5D1) else Color.White
                    )
                    Text(
                        snapshot.server?.let { "${it.city}, ${it.country}" } ?: "Choose a server",
                        color = Color(0xFF8B95A7)
                    )
                    snapshot.error?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(Modifier.height(26.dp))
            if (snapshot.state == ConnectionState.CONNECTED) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Public IP  ${snapshot.publicIp ?: "checking…"}",
                    color = Color(0xFF8B95A7),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            ServerPreview(snapshot.server ?: servers.firstOrNull(), onServers)
        }
    }
}

@Composable
private fun ConnectionOrb(state: ConnectionState, onClick: () -> Unit) {
    val active = state == ConnectionState.CONNECTED
    val busy = state == ConnectionState.CONNECTING || state == ConnectionState.RECONNECTING
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        1f, 1.08f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "pulse"
    )
    val color by animateColorAsState(if (active) Color(0xFF65F5D1) else Color(0xFF9C7CFF), label = "orb")
    Box(
        Modifier.size(if (busy) (190 * pulse).dp else 190.dp)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(color.copy(alpha = .25f), Color.Transparent)))
            .clickable(enabled = state != ConnectionState.DISCONNECTING, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.size(142.dp).clip(CircleShape)
                .background(Color(0xFF0B0E15))
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PowerSettingsNew, null, tint = color, modifier = Modifier.size(54.dp))
        }
    }
}

@Composable
private fun ServerPreview(server: Server?, onServers: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onServers),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B0E15)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(server?.countryCode ?: "—", fontWeight = FontWeight.Bold, color = Color(0xFF65F5D1))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(server?.country ?: "No server selected", fontWeight = FontWeight.SemiBold)
                Text(server?.city ?: "Configure a legitimate server source", color = Color(0xFF7D8799), style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF6E788B))
        }
    }
}

@Composable
private fun ServerScreen(vm: FreeXViewModel, onBack: () -> Unit) {
    val list by vm.filtered.collectAsStateWithLifecycle()
    val favs by vm.favorites.collectAsStateWithLifecycle()
    val recent by vm.recent.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    var favoritesOnly by remember { mutableStateOf(false) }
    var recentOnly by remember { mutableStateOf(false) }
    var countryFilter by remember { mutableStateOf<String?>(null) }
    var countryMenuOpen by remember { mutableStateOf(false) }

    val visible = list.filter { server ->
        (!favoritesOnly || favs.contains(server.id)) &&
        (!recentOnly || recent.contains(server.id)) &&
        (countryFilter == null || server.country == countryFilter)
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF050608)).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Text("Servers", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = vm::refresh) { Icon(Icons.Default.Refresh, null) }
        }
        OutlinedTextField(
            value = query,
            onValueChange = vm::setQuery,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search country, city or host") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true,
            shape = RoundedCornerShape(18.dp)
        )
        Row(Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(true, "Fastest") { vm.setSort(ServerSort.LATENCY) }
            FilterChip(false, "Lowest load") { vm.setSort(ServerSort.LOAD) }
            Box {
                FilterChip(countryFilter != null, countryFilter ?: "Country") { countryMenuOpen = true }
                DropdownMenu(expanded = countryMenuOpen, onDismissRequest = { countryMenuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("All countries") },
                        onClick = { countryFilter = null; countryMenuOpen = false }
                    )
                    list.map { it.country }.distinct().sorted().forEach { country ->
                        DropdownMenuItem(
                            text = { Text(country) },
                            onClick = { countryFilter = country; countryMenuOpen = false }
                        )
                    }
                }
            }
            FilterChip(false, "Country sort") { vm.setSort(ServerSort.COUNTRY) }
            FilterChip(favoritesOnly, "Favorites") { favoritesOnly = !favoritesOnly }
            FilterChip(recentOnly, "Recent") { recentOnly = !recentOnly }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(visible, key = { it.id }) { server ->
                ServerCard(server, favs.contains(server.id), { vm.favorite(server, !favs.contains(server.id)) }, { vm.connect(server) })
            }
        }
    }
}

@Composable
private fun FilterChip(selected: Boolean, text: String, onClick: () -> Unit) {
    AssistChip(onClick = onClick, label = { Text(text) }, leadingIcon = {
        if (selected) Icon(Icons.Default.Check, null, Modifier.size(16.dp))
    })
}

@Composable
private fun ServerCard(server: Server, favorite: Boolean, onFavorite: () -> Unit, onConnect: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onConnect),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B0E15)),
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).clip(CircleShape).background(Color(0xFF151A25)),
                contentAlignment = Alignment.Center
            ) { Text(server.countryCode, fontWeight = FontWeight.Bold, color = Color(0xFF65F5D1)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("${server.city}, ${server.country}", fontWeight = FontWeight.SemiBold)
                Text("${server.host}:${server.port} • ${server.protocol.uppercase()}", color = Color(0xFF788397), style = MaterialTheme.typography.bodySmall)
                Text(
                    "Latency ${server.latencyMs?.let { "${it} ms" } ?: "—"} • Load ${server.loadPercent?.let { "$it%" } ?: "—"}",
                    color = Color(0xFF65F5D1),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            IconButton(onClick = onFavorite) {
                Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null)
            }
        }
    }
}


@Composable
private fun SettingsScreen(vm: SettingsViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(Modifier.fillMaxSize().background(Color(0xFF050608)).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Text("Connection", color = Color(0xFF65F5D1), modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))

        SettingToggle(
            title = "Auto Connect",
            subtitle = "Connect when the app starts",
            checked = state.autoConnect,
            onCheckedChange = vm::setAutoConnect
        )
        SettingToggle(
            title = "Auto Select Server",
            subtitle = "Choose the lowest-latency online server",
            checked = state.autoSelect,
            onCheckedChange = vm::setAutoSelect
        )
        SettingToggle(
            title = "Reconnect Automatically",
            subtitle = "Reconnect after a network transition",
            checked = state.reconnect,
            onCheckedChange = vm::setReconnect
        )

        ListItem(
            headlineContent = { Text("Kill Switch") },
            supportingContent = {
                Text(
                    "Android Always-on VPN + lockdown provides the strict blocking semantics.",
                    color = Color(0xFF788397)
                )
            },
            trailingContent = {
                TextButton(onClick = {
                    context.startActivity(Intent(android.provider.Settings.ACTION_VPN_SETTINGS))
                }) { Text("OPEN") }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )

        Text("Protocol", color = Color(0xFF65F5D1), modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
        ListItem(
            headlineContent = { Text("OpenVPN") },
            supportingContent = { Text("Real encrypted OpenVPN tunnel using the OpenVPN for Android engine.", color = Color(0xFF788397)) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )

        Text("Privacy", color = Color(0xFF65F5D1), modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
        Text(
            "No account, analytics SDK, ad SDK, or tracking SDK is included. Server configuration is fetched only from the configured HTTPS source.",
            color = Color(0xFF8A94A6)
        )
    }
}


@Composable
private fun SettingToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, color = Color(0xFF788397)) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun FloatingDock(onServers: () -> Unit, onSettings: () -> Unit) {
    Surface(
        Modifier.padding(horizontal = 22.dp, vertical = 12.dp).fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = Color(0xEE10141D)
    ) {
        Row(
            Modifier.padding(8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DockItem(Icons.Default.Home, "Home", {})
            DockItem(Icons.Default.Public, "Servers", onServers)
            DockItem(Icons.Default.Settings, "Settings", onSettings)
        }
    }
}

@Composable
private fun DockItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        Modifier.clickable(onClick = onClick).padding(horizontal = 22.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = Color(0xFFB6C0D1), modifier = Modifier.size(22.dp))
        Text(label, color = Color(0xFF7E899C), style = MaterialTheme.typography.labelSmall)
    }
}
