package com.muse.companion.ai

import com.google.common.truth.Truth.assertThat
import com.muse.companion.ai.device.DeviceProfile
import com.muse.companion.ai.model.ModelCatalog
import org.junit.Test

class ModelCatalogTest {
    private val gb = 1024L * 1024 * 1024

    private fun profile(ramGb: Long, freeGb: Long = 20, abi64: Boolean = true, lowRam: Boolean = false) =
        DeviceProfile(
            totalRamBytes = ramGb * gb,
            isLowRam = lowRam,
            abis = if (abi64) listOf("arm64-v8a") else listOf("armeabi-v7a"),
            has64Bit = abi64,
            freeAppStorageBytes = freeGb * gb,
            sdkInt = 34,
        )

    @Test fun noModelWithout64BitAbi() {
        assertThat(ModelCatalog.recommend(profile(8, abi64 = false)).model).isNull()
    }

    @Test fun noModelOnLowRamDevice() {
        assertThat(ModelCatalog.recommend(profile(2)).model).isNull()
        assertThat(ModelCatalog.recommend(profile(8, lowRam = true)).model).isNull()
    }

    @Test fun picksAtLeastAsLargeForMoreRam() {
        val small = ModelCatalog.recommend(profile(4)).model
        val big = ModelCatalog.recommend(profile(8)).model
        assertThat(small).isNotNull()
        assertThat(big).isNotNull()
        assertThat(big!!.approxSizeBytes).isAtLeast(small!!.approxSizeBytes)
    }
}
