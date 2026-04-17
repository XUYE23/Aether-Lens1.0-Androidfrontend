// app/src/main/java/com/aether/app/data/DraftVoiceCard.kt
package com.aether.app.data

data class DraftVoiceCard(
    val partialText: String = "",
    val finalText: String? = null,       // null = still recognising; non-null = finalised
    val errorMessage: String? = null,    // non-null = recognition failed
    val createdAt: Long = System.currentTimeMillis()
)
