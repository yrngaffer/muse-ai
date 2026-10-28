package com.muse.companion.tools

import com.muse.companion.security.ToolSpec

data class ToolCall(val toolId: String, val args: Map<String, String>)

data class ToolResult(val toolId: String, val success: Boolean, val output: String)

/** A runtime tool: its immutable [spec] + an executor that receives ONLY broker-validated args. */
interface Tool {
    val spec: ToolSpec
    suspend fun execute(args: Map<String, Any>): ToolResult
}
