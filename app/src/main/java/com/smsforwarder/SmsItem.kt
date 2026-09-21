package com.smsforwarder

data class SmsItem(
    val id: Long,
    val sender: String,
    val body: String,
    val timestamp: Long,
    val forwarded: Boolean = false
)
