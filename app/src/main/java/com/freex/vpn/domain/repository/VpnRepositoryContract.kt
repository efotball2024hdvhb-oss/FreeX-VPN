package com.freex.vpn.domain.repository

import com.freex.vpn.domain.model.ConnectionState
import com.freex.vpn.domain.model.Server
import com.freex.vpn.domain.model.VpnSnapshot
import kotlinx.coroutines.flow.StateFlow

interface VpnRepositoryContract {
    val snapshot: StateFlow<VpnSnapshot>
    suspend fun connect(server: Server): Result<Unit>
    suspend fun disconnect(): Result<Unit>
    fun preparePermission(androidContext: android.content.Context): android.content.Intent?
}
