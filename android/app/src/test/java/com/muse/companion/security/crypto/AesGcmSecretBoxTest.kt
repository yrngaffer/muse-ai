package com.muse.companion.security.crypto

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import javax.crypto.AEADBadTagException
import javax.crypto.KeyGenerator

class AesGcmSecretBoxTest {
    private val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    private val box = AesGcmSecretBox { key }

    @Test fun roundTripsString() {
        val sealed = box.sealString("top secret")
        assertThat(sealed).isNotEqualTo("top secret")
        assertThat(box.openString(sealed)).isEqualTo("top secret")
    }

    @Test fun usesFreshIvEachTime() {
        assertThat(box.sealString("same")).isNotEqualTo(box.sealString("same"))
    }

    @Test fun tamperingIsRejected() {
        val sealed = box.seal("hello".toByteArray())
        sealed[sealed.size - 1] = (sealed[sealed.size - 1] + 1).toByte()
        try {
            box.open(sealed)
            throw AssertionError("expected an AEAD authentication failure")
        } catch (e: AEADBadTagException) {
            assertThat(e).isInstanceOf(AEADBadTagException::class.java)
        }
    }
}
