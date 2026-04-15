package com.aether.app.data

import java.util.UUID

data class ApiConfig(
    val id: String = UUID.randomUUID().toString(),
    val providerName: String,
    val remark: String = "",
    val websiteUrl: String = "",
    val apiKey: String,
    val requestUrl: String
)
