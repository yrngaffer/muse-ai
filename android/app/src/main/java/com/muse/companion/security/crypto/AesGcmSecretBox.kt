package com.muse.companion.security.crypto

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-256-GCM authenticated encryption. The key is supplied by a provider (Android Keystore on
 * device; an in-memory key in tests), so the crypto transform itself is unit-testable on the JVM.
 * Wire format: [12-byte IV][ciphertext || 16-byte GCM tag]. Base64 uses java.util (API 26+).
 */
class AesGcmSecretBox(private val keyProvider: () -> SecretKey) : SecretBox {
    private val rng = SecureRandom()

    override fun seal(plaintext: ByteArray): ByteArray {
        val iv = ByteArray(IV_LEN).also { rng.nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, keyProvider(), GCMParameterSpec(TAG_BITS, iv))
        return iv + cipher.doFinal(plaintext)
    }

    override fun open(sealed: ByteArray): ByteArray {
        require(sealed.size > IV_LEN) { "ciphertext too short" }
        val iv = sealed.copyOfRange(0, IV_LEN)
        val ct = sealed.copyOfRange(IV_LEN, sealed.size)
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.DECRYPT_MODE, keyProvider(), GCMParameterSpec(TAG_BITS, iv))
        return cipher.doFinal(ct)
    }

    override fun sealString(s: String): String =
        Base64.getEncoder().encodeToString(seal(s.toByteArray(Charsets.UTF_8)))

    override fun openString(s: String): String =
        String(open(Base64.getDecoder().decode(s)), Charsets.UTF_8)

    companion object {
        private const val TRANSFORM = "AES/GCM/NoPadding"
        private const val IV_LEN = 12
        private const val TAG_BITS = 128
    }
}
