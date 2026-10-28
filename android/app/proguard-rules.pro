# Keep kotlinx.serialization generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.muse.companion.** {
    *** Companion;
}
-keepclasseswithmembers class com.muse.companion.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**

# OkHttp (standard)
-dontwarn okhttp3.**
-dontwarn okio.**
