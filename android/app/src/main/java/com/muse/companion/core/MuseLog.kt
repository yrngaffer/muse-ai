package com.muse.companion.core

import android.util.Log

/**
 * Logger that refuses to print secrets. Any token/password/key pattern is masked before it
 * reaches logcat. We never log memory content, credentials, or keys. [scrub] is pure and tested.
 */
object MuseLog {
    private const val TAG = "Muse"
    private val SECRET =
        Regex("(?i)\\b(token|secret|password|passwd|api[_-]?key|authorization|bearer)\\b\\s*[:=]\\s*[^\\s,;]+")

    fun scrub(msg: String): String = SECRET.replace(msg, "<redacted>")

    fun d(msg: String) = Log.d(TAG, scrub(msg))
    fun i(msg: String) = Log.i(TAG, scrub(msg))
    fun w(msg: String) = Log.w(TAG, scrub(msg))
    fun e(msg: String, t: Throwable? = null) = Log.e(TAG, scrub(msg), t)
}
