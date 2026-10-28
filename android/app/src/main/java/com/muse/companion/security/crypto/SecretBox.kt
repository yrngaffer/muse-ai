package com.muse.companion.security.crypto

/** Authenticated-encryption boundary. Implementations use AES-256-GCM. */
interface SecretBox {
    fun seal(plaintext: ByteArray): ByteArray
    fun open(sealed: ByteArray): ByteArray
    fun sealString(s: String): String
    fun openString(s: String): String
}
