package com.muse.companion.ai

enum class Role { SYSTEM, USER, ASSISTANT, TOOL }

data class ChatMessage(val role: Role, val content: String)
