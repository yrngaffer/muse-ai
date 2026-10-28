package com.muse.companion.ai.model

import com.muse.companion.ai.device.DeviceProfile

/**
 * Open-weight model entry. `sha256` is filled when a concrete download URL is finalized (M2);
 * downloads are integrity-checked against it and a model that fails verification is deleted,
 * never loaded. `provenance` marked "verify" must be confirmed before shipping a real URL.
 */
data class ModelInfo(
    val id: String,
    val name: String,
    val approxSizeBytes: Long,
    val estRuntimeRamBytes: Long,
    val contextTokens: Int,
    val license: String,
    val provenance: String,
    val sha256: String,
)

object ModelCatalog {
    private const val GB = 1024L * 1024 * 1024
    private const val MB = 1024L * 1024

    val models: List<ModelInfo> = listOf(
        ModelInfo("gemma3-1b-int4", "Gemma 3 1B (int4)", 900 * MB, 1800 * MB, 2048, "Gemma Terms of Use", "google (verify)", ""),
        ModelInfo("llama32-1b-int4", "Llama 3.2 1B (int4)", 1 * GB, 2 * GB, 4096, "Llama 3.2 Community License", "meta-llama (verify)", ""),
        ModelInfo("qwen25-1_5b-int4", "Qwen2.5 1.5B (int4)", 1200 * MB, 2300 * MB, 4096, "Apache-2.0", "Qwen (verify)", ""),
        ModelInfo("gemma2-2b-int4", "Gemma 2 2B (int4)", 1500 * MB, 3 * GB, 4096, "Gemma Terms of Use", "google (verify)", ""),
        ModelInfo("phi35-mini-int4", "Phi-3.5-mini (int4)", 2400 * MB, 4 * GB, 4096, "MIT", "microsoft (verify)", ""),
    )

    data class Recommendation(val model: ModelInfo?, val rationale: String)

    /** Pure function of the device profile — unit-tested without a device. */
    fun recommend(p: DeviceProfile): Recommendation {
        if (!p.has64Bit) {
            return Recommendation(null, "No 64-bit ABI; on-device LLM unsupported. Use offline mode or an opt-in remote engine.")
        }
        if (p.isLowRam || p.totalRamBytes < 3 * GB) {
            return Recommendation(null, "Low RAM (< 3 GB); on-device LLM not advisable. Offline or remote only.")
        }
        val ramBudget = p.totalRamBytes * 55 / 100 // allow a model to use up to ~55% of RAM
        val affordable = models.filter {
            it.estRuntimeRamBytes <= ramBudget && it.approxSizeBytes * 3 / 2 <= p.freeAppStorageBytes
        }
        val pick = affordable.maxByOrNull { it.approxSizeBytes } // best quality that fits
        return if (pick != null) {
            Recommendation(pick, "Recommended for ${p.totalRamBytes / GB} GB RAM, ${p.freeAppStorageBytes / GB} GB free.")
        } else {
            Recommendation(null, "No catalog model fits this device's RAM/storage budget right now.")
        }
    }
}
