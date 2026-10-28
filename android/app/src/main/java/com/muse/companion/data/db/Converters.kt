package com.muse.companion.data.db

import androidx.room.TypeConverter
import com.muse.companion.data.memory.MemoryLayer
import com.muse.companion.data.memory.Sensitivity
import com.muse.companion.data.memory.VerificationStatus

/** Explicit enum<->String converters so the schema is stable and portable. */
class Converters {
    @TypeConverter fun layerToString(v: MemoryLayer): String = v.name
    @TypeConverter fun stringToLayer(v: String): MemoryLayer = MemoryLayer.valueOf(v)

    @TypeConverter fun verToString(v: VerificationStatus): String = v.name
    @TypeConverter fun stringToVer(v: String): VerificationStatus = VerificationStatus.valueOf(v)

    @TypeConverter fun sensToString(v: Sensitivity): String = v.name
    @TypeConverter fun stringToSens(v: String): Sensitivity = Sensitivity.valueOf(v)
}
