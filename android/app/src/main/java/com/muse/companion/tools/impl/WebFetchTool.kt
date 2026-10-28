package com.muse.companion.tools.impl

import com.muse.companion.security.ArgSpec
import com.muse.companion.security.ArgType
import com.muse.companion.security.PermissionLevel
import com.muse.companion.security.ToolSpec
import com.muse.companion.tools.Tool
import com.muse.companion.tools.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Fetches the text of a public https page. The broker has already validated the URL through
 * UrlGuard (SSRF/DNS-rebinding) before this runs. Redirects are NOT followed (a redirect target
 * would bypass the guard). Output is capped. Only usable when online tools are enabled.
 */
class WebFetchTool(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(15, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build(),
) : Tool {
    override val spec = ToolSpec(
        id = "web_fetch",
        description = "Fetch the readable text of a public https web page.",
        permission = PermissionLevel.READ_ONLY,
        requiresInternet = true,
        reversible = true,
        requiresConfirmation = false,
        timeoutMs = 20_000,
        maxOutputBytes = 20_000,
        args = listOf(ArgSpec("url", ArgType.URL, required = true, description = "https URL to fetch")),
    )

    override suspend fun execute(args: Map<String, Any>): ToolResult = withContext(Dispatchers.IO) {
        val url = args["url"] as String
        try {
            client.newCall(Request.Builder().url(url).header("User-Agent", "Muse/0.1").build()).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext ToolResult(spec.id, false, "HTTP ${resp.code}")
                val text = stripHtml(resp.body?.string().orEmpty()).take(spec.maxOutputBytes)
                ToolResult(spec.id, true, text)
            }
        } catch (e: Exception) {
            ToolResult(spec.id, false, "Fetch failed: ${e.message}")
        }
    }

    private fun stripHtml(html: String): String =
        html.replace(Regex("(?s)<script.*?</script>"), " ")
            .replace(Regex("(?s)<style.*?</style>"), " ")
            .replace(Regex("<[^>]+>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
}
