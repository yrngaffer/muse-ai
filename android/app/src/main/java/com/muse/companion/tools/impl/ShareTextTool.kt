package com.muse.companion.tools.impl

import android.content.Context
import android.content.Intent
import com.muse.companion.security.ArgSpec
import com.muse.companion.security.ArgType
import com.muse.companion.security.PermissionLevel
import com.muse.companion.security.ToolSpec
import com.muse.companion.tools.Tool
import com.muse.companion.tools.ToolResult

/** Opens the Android share sheet with some text. Reversible (the user still picks a target app). */
class ShareTextTool(private val context: Context) : Tool {
    override val spec = ToolSpec(
        id = "share_text",
        description = "Open the Android share sheet with some text.",
        permission = PermissionLevel.REVERSIBLE,
        requiresInternet = false,
        reversible = true,
        requiresConfirmation = false,
        timeoutMs = 5_000,
        maxOutputBytes = 500,
        args = listOf(ArgSpec("text", ArgType.STRING, required = true, description = "text to share", maxLength = 10_000)),
    )

    override suspend fun execute(args: Map<String, Any>): ToolResult {
        val text = args["text"] as String
        return try {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(send, "Share via").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            ToolResult(spec.id, true, "Opened share sheet")
        } catch (e: Exception) {
            ToolResult(spec.id, false, "Share failed: ${e.message}")
        }
    }
}
