package com.muse.companion.security

import com.google.common.truth.Truth.assertThat
import com.muse.companion.security.guards.PathGuard
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PathGuardTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val guard = PathGuard()

    @Test fun allowsInsideRoot() {
        val root = tmp.newFolder("ws")
        val f = File(root, "notes/today.txt")
        assertThat(guard.check(f.path, listOf(root.path))).isInstanceOf(PathGuard.Result.Ok::class.java)
    }

    @Test fun blocksTraversalEscape() {
        val root = tmp.newFolder("ws")
        val escape = File(root, "../secret.txt").path
        assertThat(guard.check(escape, listOf(root.path))).isInstanceOf(PathGuard.Result.Blocked::class.java)
    }

    @Test fun blocksOutsideRoot() {
        val root = tmp.newFolder("ws")
        val other = tmp.newFolder("other")
        assertThat(guard.check(File(other, "x.txt").path, listOf(root.path)))
            .isInstanceOf(PathGuard.Result.Blocked::class.java)
    }

    @Test fun blocksNullByte() {
        val root = tmp.newFolder("ws")
        assertThat(guard.check("a\u0000b", listOf(root.path))).isInstanceOf(PathGuard.Result.Blocked::class.java)
    }

    @Test fun blocksWhenNoRootsConfigured() {
        assertThat(guard.check("/tmp/x", emptyList())).isInstanceOf(PathGuard.Result.Blocked::class.java)
    }
}
