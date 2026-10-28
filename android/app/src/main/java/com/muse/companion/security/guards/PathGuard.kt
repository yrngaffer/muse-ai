package com.muse.companion.security.guards

import java.io.File

/**
 * Prevents path traversal / escape outside granted roots. A path is allowed only if its
 * canonical form (with `..`, symlinks, and `.` resolved) is inside one of the allowed
 * canonical roots. Null bytes and empty paths are rejected outright.
 */
class PathGuard {
    sealed interface Result {
        data class Ok(val canonicalPath: String) : Result
        data class Blocked(val reason: String) : Result
    }

    fun check(raw: String, allowedRoots: List<String>): Result {
        if (raw.isBlank()) return Result.Blocked("empty path")
        if (raw.contains('\u0000')) return Result.Blocked("null byte in path")
        if (allowedRoots.isEmpty()) return Result.Blocked("no allowed roots configured")

        val target = try {
            File(raw).canonicalFile
        } catch (e: Exception) {
            return Result.Blocked("cannot canonicalize path")
        }
        for (root in allowedRoots) {
            val canonRoot = try {
                File(root).canonicalFile
            } catch (e: Exception) {
                continue
            }
            if (isInside(canonRoot, target)) return Result.Ok(target.path)
        }
        return Result.Blocked("path escapes allowed roots")
    }

    private fun isInside(root: File, target: File): Boolean {
        var p: File? = target
        while (p != null) {
            if (p == root) return true
            p = p.parentFile
        }
        return false
    }
}
