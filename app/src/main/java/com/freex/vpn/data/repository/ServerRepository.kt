package com.freex.vpn.data.repository

import com.freex.vpn.data.api.ServerApi
import com.freex.vpn.data.local.SettingsStore
import com.freex.vpn.domain.model.Server
import com.freex.vpn.domain.repository.ServerRepositoryContract
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class ServerRepository(
    private val api: ServerApi,
    private val settings: SettingsStore
) : ServerRepositoryContract {
    private val _servers = MutableStateFlow<List<Server>>(emptyList())
    override val servers: Flow<List<Server>> = _servers
    override val favorites: Flow<Set<String>> = settings.favorites
    override val recent: Flow<List<String>> = settings.recent

    override suspend fun refresh(): Result<List<Server>> {
        val result = api.fetchServers()
        result.onSuccess { list ->
            _servers.value = list.filter { it.online }
        }
        return result
    }

    override suspend fun setFavorite(id: String, favorite: Boolean) = settings.setFavorite(id, favorite)
    override suspend fun markUsed(id: String) = settings.markRecent(id)
}
