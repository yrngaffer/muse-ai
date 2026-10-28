package com.muse.companion.security.guards

import java.net.InetAddress
import java.net.URI

/**
 * Defends outbound HTTP against SSRF / DNS-rebinding. A URL is allowed only if:
 *  - the scheme is https (http only if explicitly permitted),
 *  - the host, if an IP literal, is not in a private/loopback/link-local/metadata range,
 *  - DNS resolution yields NO address in those ranges (checked AFTER resolution),
 *  - (optional) the host matches an allow-list of suffixes.
 *
 * The DNS resolver is injectable, so this is fully unit-testable offline.
 */
class UrlGuard(
    private val resolver: (String) -> List<InetAddress> = { host -> InetAddress.getAllByName(host).toList() },
    private val allowHttp: Boolean = false,
) {
    sealed interface Result {
        data class Ok(val url: URI) : Result
        data class Blocked(val reason: String) : Result
    }

    fun check(raw: String, allowedSuffixes: List<String>? = null): Result {
        val uri = try {
            URI(raw.trim())
        } catch (e: Exception) {
            return Result.Blocked("malformed URL")
        }
        val scheme = uri.scheme?.lowercase() ?: return Result.Blocked("missing scheme")
        if (scheme != "https" && !(allowHttp && scheme == "http")) {
            return Result.Blocked("scheme '$scheme' not allowed (use https)")
        }
        val host = uri.host ?: return Result.Blocked("missing host")

        if (!allowedSuffixes.isNullOrEmpty()) {
            val h = host.lowercase()
            if (allowedSuffixes.none { h == it.lowercase() || h.endsWith(".${it.lowercase()}") }) {
                return Result.Blocked("host '$host' not in allow-list")
            }
        }

        // IP literal: check directly, no DNS.
        literalAddress(host)?.let { addr ->
            return if (isDangerous(addr)) Result.Blocked("blocked address range: ${addr.hostAddress}")
            else Result.Ok(uri)
        }

        // Hostname: resolve and reject if ANY resolved address is dangerous (DNS-rebinding defense).
        val addrs = try {
            resolver(host)
        } catch (e: Exception) {
            return Result.Blocked("DNS resolution failed")
        }
        if (addrs.isEmpty()) return Result.Blocked("host did not resolve")
        for (a in addrs) if (isDangerous(a)) return Result.Blocked("resolves to blocked range: ${a.hostAddress}")
        return Result.Ok(uri)
    }

    private fun literalAddress(host: String): InetAddress? {
        val h = host.trim('[', ']')
        val looksNumeric = h.matches(Regex("^[0-9.]+$")) || h.contains(':')
        return if (looksNumeric) try { InetAddress.getByName(h) } catch (e: Exception) { null } else null
    }

    private fun isDangerous(a: InetAddress): Boolean {
        if (a.isAnyLocalAddress || a.isLoopbackAddress || a.isLinkLocalAddress ||
            a.isSiteLocalAddress || a.isMulticastAddress
        ) return true
        val b = a.address
        if (b.size == 4) {
            val x = b.map { it.toInt() and 0xFF }
            // 100.64.0.0/10 carrier-grade NAT
            if (x[0] == 100 && x[1] in 64..127) return true
            // 169.254.0.0/16 link-local incl. 169.254.169.254 cloud metadata (also caught above; explicit)
            if (x[0] == 169 && x[1] == 254) return true
            // 192.0.0.0/24 IETF protocol assignments
            if (x[0] == 192 && x[1] == 0 && x[2] == 0) return true
        }
        return false
    }
}
