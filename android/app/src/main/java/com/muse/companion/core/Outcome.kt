package com.muse.companion.core

/** A tiny Result-like type so failures are explicit across layers instead of thrown silently. */
sealed interface Outcome<out T> {
    data class Ok<T>(val value: T) : Outcome<T>
    data class Err(val message: String, val cause: Throwable? = null) : Outcome<Nothing>
}

inline fun <T> outcome(block: () -> T): Outcome<T> =
    try {
        Outcome.Ok(block())
    } catch (t: Throwable) {
        Outcome.Err(t.message ?: t.javaClass.simpleName, t)
    }
