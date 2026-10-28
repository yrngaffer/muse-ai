package com.muse.companion.security

import com.google.common.truth.Truth.assertThat
import com.muse.companion.security.guards.UrlGuard
import org.junit.Test
import java.net.InetAddress

class UrlGuardTest {
    private fun guardResolvingTo(ip: String) = UrlGuard(resolver = { listOf(InetAddress.getByName(ip)) })

    @Test fun allowsPublicHttps() {
        assertThat(guardResolvingTo("8.8.8.8").check("https://example.com/path"))
            .isInstanceOf(UrlGuard.Result.Ok::class.java)
    }

    @Test fun blocksPlainHttp() {
        assertThat(guardResolvingTo("8.8.8.8").check("http://example.com"))
            .isInstanceOf(UrlGuard.Result.Blocked::class.java)
    }

    @Test fun blocksLoopbackLiteral() {
        assertThat(UrlGuard().check("https://127.0.0.1/")).isInstanceOf(UrlGuard.Result.Blocked::class.java)
    }

    @Test fun blocksPrivateLiterals() {
        assertThat(UrlGuard().check("https://10.1.2.3/")).isInstanceOf(UrlGuard.Result.Blocked::class.java)
        assertThat(UrlGuard().check("https://192.168.0.5/")).isInstanceOf(UrlGuard.Result.Blocked::class.java)
    }

    @Test fun blocksCloudMetadataLiteral() {
        assertThat(UrlGuard().check("https://169.254.169.254/latest/meta-data/"))
            .isInstanceOf(UrlGuard.Result.Blocked::class.java)
    }

    @Test fun blocksDnsRebindingToPrivate() {
        // A public-looking hostname that resolves to a private IP must be blocked AFTER resolution.
        assertThat(guardResolvingTo("192.168.1.10").check("https://totally-innocent.example/"))
            .isInstanceOf(UrlGuard.Result.Blocked::class.java)
    }

    @Test fun enforcesAllowList() {
        val g = guardResolvingTo("8.8.8.8")
        assertThat(g.check("https://api.example.com/", listOf("example.com")))
            .isInstanceOf(UrlGuard.Result.Ok::class.java)
        assertThat(g.check("https://evil.org/", listOf("example.com")))
            .isInstanceOf(UrlGuard.Result.Blocked::class.java)
    }

    @Test fun blocksMalformed() {
        assertThat(UrlGuard().check("not a url")).isInstanceOf(UrlGuard.Result.Blocked::class.java)
    }
}
