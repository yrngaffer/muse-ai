package com.muse.companion.tools.impl

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.muse.companion.security.ArgSpec
import com.muse.companion.security.ArgType
import com.muse.companion.security.PermissionLevel
import com.muse.companion.security.ToolScope
import com.muse.companion.security.ToolSpec
import com.muse.companion.tools.Tool
import com.muse.companion.tools.ToolResult

/** Opens an https URL in the user's browser. The URL is validated by UrlGuard in the broker first. */
class OpenUrlTool(private val context: Context) : Tool {
    override val spec = ToolSpec(
        id = "open_url",
        description = "Open a web URL in the user's browser.",
        permission = PermissionLevel.REVERSIBLE,
        requiresInternet = false, // launching the browser doesn't use our own network permission
        reversible = true,
        requiresConfirmation = false,
        timeoutMs = 5_000,
        maxOutputBytes = 1_000,
        args = listOf(ArgSpec("url", ArgType.URL, required = true, description = "https URL to open")),
        scope = ToolScope.NONE,
    )

    override suspend fun execute(args: Map<String, Any>): ToolResult {
        val url = args["url"] as String
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            ToolResult(spec.id, true, "Opened $url")
        } catch (e: Exception) {
            ToolResult(spec.id, false, "Could not open URL: ${e.message}")
        }
    }
}
