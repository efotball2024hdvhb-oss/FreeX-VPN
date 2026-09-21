package com.freex.vpn.vpn

import com.wireguard.android.backend.Tunnel

class WireGuardTunnel(
    private val tunnelName: String,
    private val onState: (Tunnel.State) -> Unit
) : Tunnel {
    override fun getName(): String = tunnelName
    override fun onStateChange(newState: Tunnel.State) = onState(newState)
}
