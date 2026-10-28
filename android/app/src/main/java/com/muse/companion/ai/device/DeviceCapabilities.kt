package com.muse.companion.ai.device

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs

/** A snapshot of the signals that drive model selection. */
data class DeviceProfile(
    val totalRamBytes: Long,
    val isLowRam: Boolean,
    val abis: List<String>,
    val has64Bit: Boolean,
    val freeAppStorageBytes: Long,
    val sdkInt: Int,
)

/** Reads device capabilities at runtime (we cannot know them from the dev box; the app detects them). */
class DeviceCapabilities(private val context: Context) {
    fun profile(): DeviceProfile {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val abis = Build.SUPPORTED_ABIS?.toList() ?: emptyList()
        val stat = StatFs(context.filesDir.absolutePath)
        return DeviceProfile(
            totalRamBytes = mi.totalMem,
            isLowRam = am.isLowRamDevice,
            abis = abis,
            has64Bit = abis.any { it.contains("arm64") || it.contains("x86_64") },
            freeAppStorageBytes = stat.availableBytes,
            sdkInt = Build.VERSION.SDK_INT,
        )
    }
}
