package com.muse.companion.core

/** Injectable clock so time-dependent logic is testable (no hidden System.currentTimeMillis calls). */
fun interface Clock {
    fun nowMillis(): Long

    companion object {
        val SYSTEM = Clock { System.currentTimeMillis() }
    }
}
