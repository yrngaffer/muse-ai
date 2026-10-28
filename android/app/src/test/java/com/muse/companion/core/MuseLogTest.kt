package com.muse.companion.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MuseLogTest {
    @Test fun redactsSecrets() {
        assertThat(MuseLog.scrub("password: hunter2")).doesNotContain("hunter2")
        assertThat(MuseLog.scrub("api_key=abc123")).doesNotContain("abc123")
        assertThat(MuseLog.scrub("authorization: Bearer xyz")).contains("<redacted>")
    }

    @Test fun leavesNormalTextAlone() {
        assertThat(MuseLog.scrub("the weather is nice")).isEqualTo("the weather is nice")
    }
}
