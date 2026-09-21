package com.freex.vpn.domain.model

data class VpnSnapshot(
    val state: ConnectionState = ConnectionState.DISCONNECTED,
    val server: Server? = null,
    val connectedAtMillis: Long? = null,
    val error: String? = null,
    val publicIp: String? = null
)
