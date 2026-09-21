package com.freex.vpn.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freex.vpn.FreeXVpnApplication
import com.freex.vpn.domain.model.ConnectionState
import com.freex.vpn.ui.theme.Theme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as FreeXVpnApplication
        val vpnRepo = app.container.vpnRepository

        setContent {
            Theme {
                val snapshot by vpnRepo.snapshot.collectAsState()
                val scope = rememberCoroutineScope()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "FreeX-VPN",
                                fontSize = 24.sp,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            val isConnected = snapshot.state == ConnectionState.CONNECTED
                            FilledIconButton(
                                onClick = {
                                    scope.launch {
                                        if (isConnected) {
                                            vpnRepo.disconnect()
                                        } else {
                                            snapshot.server?.let { vpnRepo.connect(it) }
                                        }
                                    }
                                },
                                modifier = Modifier.size(120.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = if (isConnected) Color(0xFF4CAF50) else Color(0xFFE53935)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PowerSettingsNew,
                                    contentDescription = "Toggle",
                                    modifier = Modifier.size(54.dp),
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = snapshot.state.name,
                                fontSize = 18.sp,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}
