package com.freex.vpn.domain.model

data class Server(
    val id: String,
    val country: String,
    val countryCode: String,
    val city: String,
    val host: String,
    val port: Int,
    val protocol: String,
    val latencyMs: Long?,
    val online: Boolean,
    val loadPercent: Int?,
    val lastCheckedEpochSeconds: Long?,
    val speedMbps: Double?,
    val score: Double?,
    val configDownloadUrl: String
)
