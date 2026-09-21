package com.freex.vpn.domain.repository

import com.freex.vpn.domain.model.Server
import kotlinx.coroutines.flow.Flow

interface ServerRepositoryContract {
    val servers: Flow<List<Server>>
    val favorites: Flow<Set<String>>
    val recent: Flow<List<String>>

    suspend fun refresh(): Result<List<Server>>
    suspend fun setFavorite(id: String, favorite: Boolean)
    suspend fun markUsed(id: String)
}
