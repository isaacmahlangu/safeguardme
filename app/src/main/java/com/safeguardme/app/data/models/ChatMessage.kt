// data/models/ChatMessage.kt
package com.safeguardme.app.data.models

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: Sender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isTyping: Boolean = false
)

enum class Sender {
    USER, BOT
}