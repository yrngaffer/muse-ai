package com.muse.companion.tools.impl

import android.content.Context
import android.content.Intent
import com.muse.companion.security.ArgSpec
import com.muse.companion.security.ArgType
import com.muse.companion.security.PermissionLevel
import com.muse.companion.security.ToolSpec
import com.muse.companion.tools.Tool
import com.muse.companion.tools.ToolResult

/**
 * Launches an installed app by package name. NOTE (honest limitation): Android 11+ package
 * visibility means we can only launch apps that expose a launcher intent we can resolve; we
 * deliberately do NOT request the Play-restricted QUERY_ALL_PACKAGES. Unknown/hidden packages
 * fail cleanly. See docs/11-permissions-guide.md.
 */
class LaunchAppTool(private val context: Context) : Tool {
    override val spec = ToolSpec(
        id = "launch_app",
        description = "Launch an installed app by its package name (e.g. com.android.chrome).",
        permission = PermissionLevel.REVERSIBLE,
        requiresInternet = false,
        reversible = true,
        requiresConfirmation = false,
        timeoutMs = 5_000,
        maxOutputBytes = 500,
        args = listOf(ArgSpec("package", ArgType.STRING, required = true, description = "app package id")),
    )

    override suspend fun execute(args: Map<String, Any>): ToolResult {
        val pkg = args["package"] as String
        val launch = context.packageManager.getLaunchIntentForPackage(pkg)
            ?: return ToolResult(spec.id, false, "App '$pkg' not found or not visible (Android package visibility).")
        return try {
            context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            ToolResult(spec.id, true, "Launched $pkg")
        } catch (e: Exception) {
            ToolResult(spec.id, false, "Launch failed: ${e.message}")
        }
    }
}
